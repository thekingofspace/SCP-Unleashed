package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp4666.item.Scp4666WinterGearItem;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp4666Cold {
    public static final int SinkTicks = 100;
    public static final double SinkDepth = 1.3;
    public static final int RemindTicks = 600;
    public static final int AssignEvery = 20;
    public static final String Prefix = Scp4666Director.Prefix;
    public static final float ChillMax = 100.0f;
    public static final float ChillFrost = 20.0f;
    public static final float ChillSlow = 40.0f;
    public static final float ChillSleep = 70.0f;
    public static final int ChillEvery = 20;
    public static final int BiteEvery = 60;
    public static final float BiteDamage = 1.0f;
    public static final float OutsideDay = 2.0f;
    public static final float OutsideNight = 3.0f;
    public static final float Freezing = 1.0f;
    public static final float Chilly = 0.4f;
    public static final float Mild = -1.0f;
    public static final float Warm = -3.0f;
    public static final float Hearth = -4.0f;
    public static final int WarmLevel = 60;
    public static final int MildLevel = 30;
    public static final double HearthReach = 4.0;
    public static final int FrozenCap = 139;
    public static final float[] Stages = {ChillFrost, ChillSlow, ChillSleep, ChillMax};
    public static final String[] StageKeys = {"chill_numb", "chill_slow", "chill_deep", "chill_bite"};
    private static final Map<MinecraftServer, Map<UUID, Sinking>> Scenes = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Float>> Chill = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Integer>> Reminded = new ConcurrentHashMap<>();

    private static final class Sinking {
        final Vec3 Anchor;
        int Ticks;

        Sinking(Vec3 Anchor) {
            this.Anchor = Anchor;
        }
    }

    private Scp4666Cold() {
    }

    public static List<ServerPlayer> claimants(ServerLevel World) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : Scp4666World.living(World)) {
            if (!Scp4666World.exempt(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static double nearestBed(Scp4666Map Layout, ServerPlayer User) {
        double Best = Double.MAX_VALUE;
        for (Scp4666Map.Bed Entry : Layout.Beds) {
            Best = Math.min(Best, User.position().distanceTo(Vec3.atCenterOf(Entry.Head())));
        }
        return Best;
    }

    public static List<ServerPlayer> assign(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        List<ServerPlayer> Chosen = new ArrayList<>();
        List<ServerPlayer> Open = new ArrayList<>();
        for (ServerPlayer User : claimants(World)) {
            if (!State.isCold(User.getUUID())) {
                Open.add(User);
            }
        }
        int Beds = Layout.Beds.size();
        if (Open.size() <= Beds) {
            return Chosen;
        }
        Map<UUID, Double> Tie = new HashMap<>();
        for (ServerPlayer User : Open) {
            Tie.put(User.getUUID(), World.getRandom().nextDouble());
        }
        Open.sort(Comparator.<ServerPlayer>comparingInt(User -> User.isSleeping() ? 0 : 1).thenComparingDouble(User -> nearestBed(Layout, User))
                .thenComparingDouble(User -> Tie.get(User.getUUID())));
        for (ServerPlayer User : Open.subList(Beds, Open.size())) {
            mark(World, State, User);
            Chosen.add(User);
        }
        return Chosen;
    }

    public static void mark(ServerLevel World, Scp4666State State, ServerPlayer User) {
        State.setCold(User.getUUID(), true);
        if (User.isSleeping()) {
            User.stopSleepInBed(true, true);
        }
        warn(World, User, "cold");
        Reminded.computeIfAbsent(World.getServer(), Key -> new HashMap<>()).put(User.getUUID(), 0);
    }

    private static void warn(ServerLevel World, ServerPlayer User, String Key) {
        User.displayClientMessage(Component.translatable(Prefix + Key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        User.playNotifySound(Scp4666Registry.Whisper.get(), SoundSource.HOSTILE, 0.6f, 0.8f);
    }

    public static boolean sinking(ServerPlayer User) {
        Map<UUID, Sinking> Active = Scenes.get(User.server);
        return Active != null && Active.containsKey(User.getUUID());
    }

    public static List<ServerPlayer> sleepers(List<ServerPlayer> Living, Scp4666State State) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : Living) {
            if (!State.isCold(User.getUUID()) && !sinking(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static void clear(MinecraftServer Server) {
        Scenes.remove(Server);
        Reminded.remove(Server);
        Chill.remove(Server);
    }

    public static float chill(ServerPlayer User) {
        Map<UUID, Float> Found = Chill.get(User.server);
        return Found == null ? 0.0f : Found.getOrDefault(User.getUUID(), 0.0f);
    }

    public static void setChill(ServerPlayer User, float Value) {
        Chill.computeIfAbsent(User.server, Key -> new ConcurrentHashMap<>()).put(User.getUUID(), Mth.clamp(Value, 0.0f, ChillMax));
    }

    public static float rate(Scp4666Map Layout, Scp4666State State, ServerPlayer User) {
        Vec3 Where = User.position();
        float Dressed = Scp4666WinterGearItem.warmth(User);
        if (!Layout.insideCabin(Where)) {
            return (Scp4666Chores.night(State) ? OutsideNight : OutsideDay) * (1.0f - Dressed);
        }
        int Heat = Scp4666Heat.warmth(State);
        float Rate = Heat >= WarmLevel ? Warm : Heat >= MildLevel ? Mild : Heat > 0 ? Chilly : Freezing;
        if (Scp4666Heat.fireLevel(State) > 0 && Layout.nearHearth(Where, HearthReach) || Scp4666Heat.boilerLevel(State) > 0 && Layout.nearHeater(Where, HearthReach)) {
            Rate += Hearth;
        }
        return Rate > 0.0f ? Rate * (1.0f - Dressed * Scp4666WinterGearItem.IndoorShare) : Rate;
    }

    public static boolean biting(float Value) {
        return Value >= ChillMax;
    }

    public static int frozenTicks(float Value) {
        if (Value <= ChillFrost) {
            return 0;
        }
        return Math.min(FrozenCap, Math.round((Value - ChillFrost) / (ChillMax - ChillFrost) * FrozenCap));
    }

    public static void chillTick(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        MinecraftServer Server = World.getServer();
        Map<UUID, Float> Values = Chill.computeIfAbsent(Server, Key -> new ConcurrentHashMap<>());
        boolean Step = Server.getTickCount() % ChillEvery == 0;
        boolean Bite = Server.getTickCount() % BiteEvery == 0;
        boolean Over = State.phase() == Scp4666State.PhaseEnding || State.phase() == Scp4666State.PhaseOver;
        List<UUID> Seen = new ArrayList<>();
        for (ServerPlayer User : claimants(World)) {
            if (sinking(User) || Scp4666World.standingIn() && !Scp4666World.nearSite(Server, User.blockPosition())) {
                continue;
            }
            if (Over || Scp4666Endings.held(User)) {
                Values.remove(User.getUUID());
                User.setTicksFrozen(0);
                continue;
            }
            Seen.add(User.getUUID());
            float Before = Values.getOrDefault(User.getUUID(), 0.0f);
            float After = Before;
            if (Step) {
                if (!Layout.insideCabin(User.position())) {
                    Scp4666WinterGearItem.wear(World, User, 1);
                }
                After = Mth.clamp(Before + rate(Layout, State, User), 0.0f, ChillMax);
                Values.put(User.getUUID(), After);
                for (int Index = 0; Index < Stages.length; Index++) {
                    if (Before < Stages[Index] && After >= Stages[Index]) {
                        User.displayClientMessage(Component.translatable(Prefix + StageKeys[Index]).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                    }
                }
                if (Before >= ChillSlow && After < ChillFrost) {
                    User.displayClientMessage(Component.translatable(Prefix + "chill_thaw").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                }
            }
            int Frozen = frozenTicks(After);
            User.setTicksFrozen(Frozen > 0 ? Frozen + 2 : 0);
            if (Bite && biting(After)) {
                User.hurt(new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp4666Registry.Chill)), BiteDamage);
            }
        }
        Values.keySet().removeIf(Id -> !Seen.contains(Id) && World.getServer().getPlayerList().getPlayer(Id) == null);
        for (ServerPlayer User : World.players()) {
            if (!Seen.contains(User.getUUID()) && Values.containsKey(User.getUUID()) && Scp4666World.exempt(User)) {
                Values.remove(User.getUUID());
            }
        }
    }

    public static void tick(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        MinecraftServer Server = World.getServer();
        chillTick(World, Layout, State);
        int Phase = State.phase();
        if ((Phase == Scp4666State.PhaseMidnight || Phase == Scp4666State.PhaseWoken) && Server.getTickCount() % AssignEvery == 0) {
            assign(World, Layout, State);
        }
        Map<UUID, Sinking> Active = Scenes.computeIfAbsent(Server, Key -> new ConcurrentHashMap<>());
        Map<UUID, Integer> Notes = Reminded.computeIfAbsent(Server, Key -> new HashMap<>());
        for (UUID Id : new ArrayList<>(State.cold())) {
            ServerPlayer User = player(World, Id);
            if (User == null || User.level() != World || !User.isAlive() || Active.containsKey(Id)) {
                continue;
            }
            if (Layout.inTheSnow(User.position()) && User.getY() < Layout.Floor + 3.0) {
                begin(World, User, Active);
                continue;
            }
            int Count = Notes.merge(Id, 1, Integer::sum);
            if (Count % RemindTicks == 0) {
                warn(World, User, "cold_again");
            }
        }
        for (Map.Entry<UUID, Sinking> Entry : new ArrayList<>(Active.entrySet())) {
            ServerPlayer User = player(World, Entry.getKey());
            if (User == null || User.level() != World || !User.isAlive()) {
                Active.remove(Entry.getKey());
                continue;
            }
            step(World, State, User, Entry.getValue(), Active);
        }
    }

    private static ServerPlayer player(ServerLevel World, UUID Id) {
        ServerPlayer Online = World.getServer().getPlayerList().getPlayer(Id);
        if (Online != null) {
            return Online;
        }
        return World.getPlayerByUUID(Id) instanceof ServerPlayer Found ? Found : null;
    }

    public static void begin(ServerLevel World, ServerPlayer User, Map<UUID, Sinking> Active) {
        Sinking Scene = new Sinking(User.position());
        Active.put(User.getUUID(), Scene);
        if (User.isSleeping()) {
            User.stopSleepInBed(true, true);
        }
        PacketDistributor.sendToPlayer(User, new Scp4666FxPayload(Scp4666FxPayload.Hold, User.getX(), User.getY(), User.getZ(), 1));
        PacketDistributor.sendToPlayer(User, new Scp4666FxPayload(Scp4666FxPayload.Bury, User.getX(), User.getY(), User.getZ(), SinkTicks));
        World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp4666Registry.Laugh.get(), SoundSource.HOSTILE, 1.0f, 0.62f);
        World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp4666Registry.BreathLow.get(), SoundSource.HOSTILE, 0.9f, 0.6f);
    }

    public static void start(ServerLevel World, ServerPlayer User) {
        begin(World, User, Scenes.computeIfAbsent(World.getServer(), Key -> new ConcurrentHashMap<>()));
    }

    private static void step(ServerLevel World, Scp4666State State, ServerPlayer User, Sinking Scene, Map<UUID, Sinking> Active) {
        Scene.Ticks++;
        double Share = Mth.clamp(Scene.Ticks / (double) SinkTicks, 0.0, 1.0);
        double Eased = Share * Share * (3.0 - 2.0 * Share);
        Vec3 Spot = Scene.Anchor.subtract(0.0, SinkDepth * Eased, 0.0);
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT), User.getYRot(), User.getXRot());
        User.setDeltaMovement(Vec3.ZERO);
        User.fallDistance = 0.0f;
        User.hurtMarked = true;
        if (Scene.Ticks % 2 == 0) {
            World.sendParticles(ParticleTypes.SNOWFLAKE, Scene.Anchor.x, Spot.y + 0.3, Scene.Anchor.z, 6, 0.45, 0.12, 0.45, 0.02);
        }
        if (Scene.Ticks == 45) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Scp4666Registry.Laugh.get(), SoundSource.HOSTILE, 0.9f, 0.55f);
        }
        if (Scene.Ticks == 70) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Scp4666Registry.Breath.get(), SoundSource.HOSTILE, 0.8f, 0.55f);
        }
        if (Scene.Ticks < SinkTicks) {
            return;
        }
        Active.remove(User.getUUID());
        State.setCold(User.getUUID(), false);
        PacketDistributor.sendToPlayer(User, new Scp4666FxPayload(Scp4666FxPayload.Hold, 0.0, 0.0, 0.0, 0));
        DamageSource Source = source(World);
        User.hurt(Source, Float.MAX_VALUE);
        if (User.isAlive() && !User.isCreative() && !User.isSpectator()) {
            User.setHealth(0.0f);
            User.die(Source);
        }
        if (User.isAlive()) {
            BlockPos Back = BlockPos.containing(Scene.Anchor);
            User.teleportTo(World, Back.getX() + 0.5, Scene.Anchor.y, Back.getZ() + 0.5, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT), User.getYRot(), User.getXRot());
        }
    }

    public static DamageSource source(ServerLevel World) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp4666Registry.Cold));
    }
}
