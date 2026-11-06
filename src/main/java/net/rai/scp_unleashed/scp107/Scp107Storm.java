package net.rai.scp_unleashed.scp107;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp107.network.Scp107RainPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp107Storm {
    public static final int MinPourTicks = 2400;
    public static final int MaxPourTicks = 4800;
    public static final int MaxRemainingTicks = 12000;
    public static final int MaxCommandSeconds = 1800;
    public static final int HarmInterval = 20;
    public static final float ActiveFloor = 0.5f;
    public static final float LavaDamage = 2.0f;
    public static final float LavaBurnSeconds = 3.0f;
    public static final float CausticDamage = 1.5f;
    public static final int SyncInterval = 100;
    public static final int RumbleDelay = 30;
    public static final int ClearMargin = 200;
    public static final int SoakCheckTicks = 5;
    public static final int CleanseInterval = 60;
    public static final int PruneTicks = 200;
    public static final int OiledTicks = 100;
    public static final int OiledRefresh = 60;
    public static final int SlickTicks = 3;
    private static final Map<UUID, Soak> Soaked = new HashMap<>();
    private static final Map<UUID, Long> Oiled = new HashMap<>();

    private Scp107Storm() {
    }

    public static int pourTicks(RandomSource Random) {
        return Mth.randomBetweenInclusive(Random, MinPourTicks, MaxPourTicks);
    }

    public static void pour(MinecraftServer Server, Scp107RainKind Kind, RandomSource Random) {
        begin(Server, Kind, pourTicks(Random), true);
    }

    public static void begin(MinecraftServer Server, Scp107RainKind Kind, int Ticks, boolean Stack) {
        Scp107Rain State = Scp107Rain.get(Server);
        if (Stack && State.active() && State.kind() == Kind) {
            int Remaining = Math.min(MaxRemainingTicks, State.remaining() + Ticks);
            State.set(Kind, State.elapsed(), State.elapsed() + Remaining, State.elapsed() + RumbleDelay);
        } else {
            int Carried = State.active() ? Math.round(State.strength() * Scp107Rain.RampTicks) : 0;
            int Length = Stack ? Math.min(Ticks, MaxRemainingTicks) : Ticks;
            State.set(Kind, Carried, Carried + Length, Carried + RumbleDelay);
        }
        weather(Server.overworld(), State);
        broadcast(State);
    }

    public static boolean end(MinecraftServer Server, boolean Settle) {
        Scp107Rain State = Scp107Rain.get(Server);
        if (State.kind() == null) {
            return false;
        }
        if (Settle) {
            ServerLevel Overworld = Server.overworld();
            Overworld.setWeatherParameters(0, ServerLevel.RAIN_DELAY.sample(Overworld.random), false, false);
        }
        Soaked.clear();
        State.clear();
        broadcast(State);
        return true;
    }

    private static void weather(ServerLevel Overworld, Scp107Rain State) {
        Scp107RainKind Kind = State.kind();
        if (Kind == null) {
            return;
        }
        if (Kind == Scp107RainKind.Water) {
            Overworld.setWeatherParameters(0, State.remaining(), true, Overworld.isThundering());
        } else {
            Overworld.setWeatherParameters(0, State.remaining() + ClearMargin, false, false);
        }
    }

    public static void tick(ServerLevel Overworld) {
        slick(Overworld);
        MinecraftServer Server = Overworld.getServer();
        Scp107Rain State = Scp107Rain.get(Server);
        Scp107RainKind Kind = State.kind();
        if (Kind == null) {
            return;
        }
        boolean Raining = Overworld.getLevelData().isRaining();
        if ((Kind == Scp107RainKind.Water) != Raining) {
            end(Server, false);
            return;
        }
        State.advance();
        if (State.elapsed() == State.rumble()) {
            rumble(Overworld);
        }
        if (Kind != Scp107RainKind.Milk && !Soaked.isEmpty()) {
            Soaked.clear();
        }
        if (State.strength() >= ActiveFloor) {
            if (Kind.harmful()) {
                harm(Overworld, Kind);
            } else if (Kind == Scp107RainKind.Milk) {
                soak(Overworld);
            } else if (Kind == Scp107RainKind.Oil) {
                smear(Overworld);
            }
        }
        if (State.elapsed() >= State.total()) {
            end(Server, true);
            return;
        }
        if (State.elapsed() % SyncInterval == 0) {
            broadcast(State);
        }
    }

    private static void rumble(ServerLevel Overworld) {
        for (ServerPlayer User : Overworld.players()) {
            float Pitch = 0.9f + Overworld.random.nextFloat() * 0.2f;
            User.playNotifySound(Scp107Registry.Thunder.get(), SoundSource.WEATHER, 0.8f, Pitch);
            User.playNotifySound(Scp107Registry.ThunderRoll.get(), SoundSource.WEATHER, 0.6f, Pitch);
        }
    }

    private static void harm(ServerLevel Overworld, Scp107RainKind Kind) {
        long Time = Overworld.getGameTime();
        List<LivingEntity> Targets = new ArrayList<>();
        for (Entity Candidate : Overworld.getAllEntities()) {
            if (Candidate instanceof LivingEntity Living && Math.floorMod(Time + Living.getId(), HarmInterval) == 0 && caught(Overworld, Living)) {
                Targets.add(Living);
            }
        }
        if (Targets.isEmpty()) {
            return;
        }
        DamageSource Source = Overworld.damageSources().source(Kind == Scp107RainKind.Lava ? Scp107Registry.LavaRainDamage : Scp107Registry.CausticRainDamage);
        for (LivingEntity Living : Targets) {
            if (Kind == Scp107RainKind.Lava) {
                scorch(Overworld, Living, Source);
            } else {
                corrode(Overworld, Living, Source);
            }
        }
    }

    private static void soak(ServerLevel Overworld) {
        long Time = Overworld.getGameTime();
        List<LivingEntity> Due = new ArrayList<>();
        for (Entity Candidate : Overworld.getAllEntities()) {
            if (!(Candidate instanceof LivingEntity Living) || Math.floorMod(Time + Living.getId(), SoakCheckTicks) != 0 || !caught(Overworld, Living)) {
                continue;
            }
            Soak Last = Soaked.get(Living.getUUID());
            boolean Fresh = Last == null || Time - Last.Wet() > SoakCheckTicks;
            boolean Ready = Fresh || Time - Last.Cleansed() >= CleanseInterval;
            Soaked.put(Living.getUUID(), new Soak(Time, Ready ? Time : Last.Cleansed()));
            if (Ready) {
                Due.add(Living);
            }
        }
        for (LivingEntity Living : Due) {
            cleanse(Overworld, Living);
        }
        if (Time % PruneTicks == 0) {
            Soaked.values().removeIf(Entry -> Time - Entry.Wet() > SoakCheckTicks || Entry.Wet() > Time);
        }
    }

    private static void cleanse(ServerLevel World, LivingEntity Living) {
        if (!Living.removeEffectsCuredBy(EffectCures.MILK)) {
            return;
        }
        double Width = Living.getBbWidth() * 0.4;
        World.sendParticles(Scp107Registry.MilkSplash.get(), Living.getX(), Living.getY() + Living.getBbHeight(), Living.getZ(), 6, Width, 0.05, Width, 0.0);
        if (Living instanceof Player) {
            float Pitch = 0.9f + World.random.nextFloat() * 0.2f;
            World.playSound(null, Living.getX(), Living.getY(), Living.getZ(), Scp107Registry.Cleanse.get(), SoundSource.WEATHER, 0.6f, Pitch);
            World.playSound(null, Living.getX(), Living.getY(), Living.getZ(), Scp107Registry.CleanseWash.get(), SoundSource.WEATHER, 0.4f, Pitch);
        }
    }

    private static void smear(ServerLevel Overworld) {
        long Time = Overworld.getGameTime();
        List<LivingEntity> Targets = new ArrayList<>();
        for (Entity Candidate : Overworld.getAllEntities()) {
            if (Candidate instanceof LivingEntity Living && Math.floorMod(Time + Living.getId(), HarmInterval) == 0 && caught(Overworld, Living)) {
                Targets.add(Living);
            }
        }
        for (LivingEntity Living : Targets) {
            coat(Overworld, Living, Time);
        }
    }

    private static void coat(ServerLevel World, LivingEntity Living, long Time) {
        boolean Fresh = Oiled.put(Living.getUUID(), Time + OiledTicks) == null;
        MobEffectInstance Current = Living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (Current == null || !Current.isInfiniteDuration() && Current.getAmplifier() <= 0 && Current.getDuration() < OiledRefresh) {
            Living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, OiledTicks, 0, false, false, true));
        }
        double Width = Living.getBbWidth() * 0.4;
        World.sendParticles(Scp107Registry.OilSplash.get(), Living.getX(), Living.getY() + Living.getBbHeight() * 0.6, Living.getZ(), 3, Width, 0.2, Width, 0.0);
        if (Fresh && Living instanceof Player) {
            World.playSound(null, Living.getX(), Living.getY(), Living.getZ(), Scp107Registry.Oiled.get(), SoundSource.WEATHER, 0.6f, 0.9f + World.random.nextFloat() * 0.2f);
        }
    }

    private static void slick(ServerLevel Overworld) {
        if (Oiled.isEmpty()) {
            return;
        }
        long Time = Overworld.getGameTime();
        Oiled.values().removeIf(Until -> Until <= Time || Until - Time > OiledTicks);
        if (Time % SlickTicks != 0) {
            return;
        }
        for (UUID Id : Oiled.keySet()) {
            if (Overworld.getEntity(Id) instanceof LivingEntity Living && Living.isAlive() && !Living.fireImmune()) {
                int Fire = Living.getRemainingFireTicks();
                if (Fire > 0 && Fire % 20 != 19) {
                    Living.setRemainingFireTicks(Fire + 1);
                }
            }
        }
    }

    public static void forget() {
        Soaked.clear();
        Oiled.clear();
    }

    public static boolean oiled(LivingEntity Living) {
        Long Until = Oiled.get(Living.getUUID());
        return Until != null && Until > Living.level().getGameTime();
    }

    public static boolean wash(LivingEntity Living) {
        Long Until = Oiled.remove(Living.getUUID());
        return Until != null && Until > Living.level().getGameTime();
    }

    public static boolean caught(Level World, LivingEntity Living) {
        if (!Living.isAlive() || Living instanceof ArmorStand || Living.isSpectator() || !Targeting.isTargetable(Living)) {
            return false;
        }
        if (Living instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return false;
        }
        return !Living.isUnderWater() && exposed(World, Living);
    }

    public static boolean exposed(Level World, Entity Target) {
        BlockPos Feet = Target.blockPosition();
        if (open(World, Feet)) {
            return true;
        }
        BlockPos Head = BlockPos.containing(Feet.getX(), Target.getBoundingBox().maxY, Feet.getZ());
        return !Head.equals(Feet) && open(World, Head);
    }

    public static boolean open(Level World, BlockPos Pos) {
        return World.canSeeSky(Pos) && World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, Pos).getY() <= Pos.getY();
    }

    private static void scorch(ServerLevel World, LivingEntity Living, DamageSource Source) {
        if (!Living.fireImmune()) {
            Living.igniteForSeconds(LavaBurnSeconds);
        }
        Living.hurt(Source, LavaDamage);
        double Width = Living.getBbWidth() * 0.4;
        World.sendParticles(Scp107Registry.Ember.get(), Living.getX(), Living.getY() + Living.getBbHeight(), Living.getZ(), 4, Width, 0.05, Width, 0.02);
        World.sendParticles(ParticleTypes.SMOKE, Living.getX(), Living.getY() + Living.getBbHeight() * 0.8, Living.getZ(), 2, Width, 0.1, Width, 0.01);
        if (Living instanceof Player) {
            float Pitch = 0.9f + World.random.nextFloat() * 0.2f;
            World.playSound(null, Living.getX(), Living.getY(), Living.getZ(), Scp107Registry.Burn.get(), SoundSource.WEATHER, 0.6f, Pitch);
            World.playSound(null, Living.getX(), Living.getY(), Living.getZ(), Scp107Registry.BurnCrackle.get(), SoundSource.WEATHER, 0.5f, Pitch);
        }
    }

    private static void corrode(ServerLevel World, LivingEntity Living, DamageSource Source) {
        Living.hurt(Source, CausticDamage);
        double Width = Living.getBbWidth() * 0.4;
        World.sendParticles(Scp107Registry.CausticSplash.get(), Living.getX(), Living.getY() + Living.getBbHeight(), Living.getZ(), 5, Width, 0.05, Width, 0.0);
        if (Living instanceof Player) {
            float Pitch = 0.9f + World.random.nextFloat() * 0.2f;
            World.playSound(null, Living.getX(), Living.getY(), Living.getZ(), Scp107Registry.Corrode.get(), SoundSource.WEATHER, 0.6f, Pitch);
            World.playSound(null, Living.getX(), Living.getY(), Living.getZ(), Scp107Registry.CorrodeBubble.get(), SoundSource.WEATHER, 0.4f, Pitch);
        }
    }

    private record Soak(long Wet, long Cleansed) {
    }

    public static Scp107RainPayload payload(Scp107Rain State) {
        Scp107RainKind Kind = State.kind();
        return new Scp107RainPayload(Kind == null ? -1 : Kind.ordinal(), State.elapsed(), State.total());
    }

    public static void broadcast(Scp107Rain State) {
        PacketDistributor.sendToAllPlayers(payload(State));
    }

    public static void sync(ServerPlayer User) {
        PacketDistributor.sendToPlayer(User, payload(Scp107Rain.get(User.server)));
    }
}
