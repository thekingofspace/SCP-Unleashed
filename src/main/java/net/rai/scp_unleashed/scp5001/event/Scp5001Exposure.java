package net.rai.scp_unleashed.scp5001.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.world.Scp5001Sites;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001ExposurePayload;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.voice.VoiceArchive;

import java.util.Optional;

public final class Scp5001Exposure {
    public static final float CaptureShare = 0.15f;
    public static final float EchoShare = 0.5f;
    public static final int CaptureTicks = 180;
    public static final int EchoOdds = 4;

    private Scp5001Exposure() {
    }

    public static int get(Entity Target) {
        return Target.getData(Scp5001EventRegistry.Exposure);
    }

    public static void set(Entity Target, int Value) {
        int Clamped = Math.max(0, Value);
        Target.setData(Scp5001EventRegistry.Exposure, Clamped);
        if (Target instanceof ServerPlayer User) {
            sync(User, false);
        }
    }

    public static void sync(ServerPlayer User, boolean Pulse) {
        PacketDistributor.sendToPlayer(User, new Scp5001ExposurePayload(get(User), Scp5001EventConfig.exposureMax(), Pulse));
    }

    public static boolean inside(Entity Target, Scp5001State State) {
        if (!State.located() || !(Target.level() instanceof ServerLevel World) || World != World.getServer().overworld()) {
            return false;
        }
        BoundingBox Site = Scp5001Sites.siteBounds(World.getServer());
        if (Site != null) {
            return Site.isInside(Target.blockPosition());
        }
        BlockPos Core = State.core();
        double Dx = Math.abs(Target.getX() - Core.getX() - 0.5);
        double Dz = Math.abs(Target.getZ() - Core.getZ() - 0.5);
        double Dy = Math.abs(Target.getY() - Core.getY());
        int Radius = Scp5001EventConfig.siteRadius();
        return Dx <= Radius && Dz <= Radius && Dy <= Scp5001EventConfig.siteHeight();
    }

    public static boolean susceptible(LivingEntity Target) {
        if (!Target.isAlive() || Target instanceof Scp5001Mimic || Target instanceof Scp610Creature || Target instanceof ArmorStand) {
            return false;
        }
        return !(Target instanceof Player User) || !User.isCreative() && !User.isSpectator();
    }

    public static void pulse(ServerLevel World, Scp5001State State, double Scale) {
        if (!State.located()) {
            return;
        }
        BlockPos Core = State.core();
        int Radius = Scp5001EventConfig.siteRadius();
        int Max = Scp5001EventConfig.exposureMax();
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, Scp5001Event.siteBox(State), Scp5001Exposure::susceptible)) {
            double Distance = Math.sqrt(Each.distanceToSqr(Core.getX() + 0.5, Core.getY() + 8.0, Core.getZ() + 0.5));
            double Closeness = 1.0 - Math.min(1.0, Distance / Radius);
            double Raw = Scp5001EventConfig.exposureGainFar() + (Scp5001EventConfig.exposureGainNear() - Scp5001EventConfig.exposureGainFar()) * Closeness;
            int Gain = Math.max(Raw * Scale > 0.0 ? 1 : 0, (int) Math.round(Raw * Scale));
            int Next = get(Each) + Gain;
            Each.setData(Scp5001EventRegistry.Exposure, Math.min(Max, Next));
            if (Each instanceof ServerPlayer User) {
                sync(User, true);
                if (Next >= Max * CaptureShare) {
                    VoiceArchive.capture(Scp5001EventRegistry.VoiceId, User, CaptureTicks);
                }
                if (Next >= Max * EchoShare && World.random.nextInt(EchoOdds) == 0) {
                    VoiceArchive.replay(Scp5001EventRegistry.VoiceId, User, Optional.empty(), Optional.of(User.getUUID()), 0.85f);
                }
            }
            if (Next >= Max) {
                unmake(World, Each);
            }
        }
    }

    public static void unmake(ServerLevel World, LivingEntity Victim) {
        DamageSource Source = World.damageSources().source(Scp5001EventRegistry.Resonance);
        World.playSound(null, Victim.getX(), Victim.getY(), Victim.getZ(), Scp5001EventRegistry.Unmade.get(), SoundSource.HOSTILE, 1.0f, 0.7f);
        Victim.hurt(Source, Math.max(1000.0f, Victim.getMaxHealth() * 100.0f));
    }

    public static void tick(ServerLevel World, Scp5001State State, long Now) {
        if (Now % Scp5001EventConfig.exposureDecayTicks() != 0) {
            return;
        }
        boolean Waving = State.phase() == Scp5001State.Phase.Rising || State.phase() == Scp5001State.Phase.Overdrive;
        for (ServerPlayer Each : World.getServer().getPlayerList().getPlayers()) {
            int Current = get(Each);
            if (Current <= 0) {
                continue;
            }
            if (Waving && inside(Each, State)) {
                continue;
            }
            set(Each, Current - 1);
        }
    }
}
