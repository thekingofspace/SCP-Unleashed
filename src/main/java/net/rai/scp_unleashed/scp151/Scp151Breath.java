package net.rai.scp_unleashed.scp151;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp151.network.Scp151BreathPayload;

import java.util.List;

public final class Scp151Breath {
    public static final int GazeHold = 3;
    public static final int DrainPerTick = 2;
    public static final int EmptyDrain = 1;
    public static final int RecoveryDelay = 60;
    public static final int RefillCycle = 4;
    public static final int RefillShare = 3;
    public static final float DrownDamage = 2.0f;
    public static final int GurgleEvery = 45;
    public static final float GurgleVolume = 0.35f;
    public static final float GurgleGrowth = 0.45f;
    public static final float GaspVolume = 0.8f;
    public static final int GaspBelow = 240;

    private Scp151Breath() {
    }

    public static void gaze(LivingEntity Target, long Now) {
        Target.setData(Scp151Registry.LastGaze, Now);
    }

    public static long lastGaze(LivingEntity Target) {
        return Target.hasData(Scp151Registry.LastGaze) ? Target.getData(Scp151Registry.LastGaze) : 0L;
    }

    public static boolean isGazing(LivingEntity Target) {
        long Last = lastGaze(Target);
        return Last > 0L && Target.level().getGameTime() - Last <= GazeHold;
    }

    public static boolean isRecovering(LivingEntity Target) {
        long Last = lastGaze(Target);
        return Last > 0L && Target.level().getGameTime() - Last > GazeHold && Target.getAirSupply() < Target.getMaxAirSupply();
    }

    public static int refill(long Now) {
        return Math.floorMod(Now, (long) RefillCycle) < RefillShare ? 1 : 0;
    }

    public static void apply(LivingBreatheEvent Event, boolean Gazing, long Since, long Now) {
        LivingEntity Target = Event.getEntity();
        if (Gazing) {
            Event.setCanBreathe(false);
            Event.setConsumeAirAmount(Target.getAirSupply() > 0 ? DrainPerTick : EmptyDrain);
            return;
        }
        if (!Event.canBreathe() || Target.getAirSupply() >= Target.getMaxAirSupply()) {
            return;
        }
        Event.setRefillAirAmount(Since < RecoveryDelay ? 0 : refill(Now));
    }

    public static void breathe(LivingBreatheEvent Event) {
        LivingEntity Target = Event.getEntity();
        Level World = Target.level();
        if (World.isClientSide) {
            return;
        }
        long Last = lastGaze(Target);
        if (Last <= 0L) {
            return;
        }
        long Now = World.getGameTime();
        long Since = Now - Last;
        boolean Gazing = Since <= GazeHold;
        if (!Gazing && Target.getAirSupply() >= Target.getMaxAirSupply()) {
            Target.removeData(Scp151Registry.LastGaze);
            return;
        }
        apply(Event, Gazing, Since, Now);
        if (Gazing && Math.floorMod(Now + Target.getId() * 7L, (long) GurgleEvery) == 0L) {
            float Breathless = 1.0f - Mth.clamp(Target.getAirSupply() / (float) Target.getMaxAirSupply(), 0.0f, 1.0f);
            play(Target, Scp151Registry.GurgleLayers, GurgleVolume + GurgleGrowth * Breathless, 0.9f + Target.getRandom().nextFloat() * 0.2f);
        }
        if (Since == RecoveryDelay && Target.getAirSupply() < GaspBelow) {
            play(Target, Scp151Registry.GaspLayers, GaspVolume, 0.95f + Target.getRandom().nextFloat() * 0.1f);
        }
    }

    public static void drown(LivingDrownEvent Event) {
        LivingEntity Target = Event.getEntity();
        if (Target.level().isClientSide || !Event.isDrowning() || !isGazing(Target)) {
            return;
        }
        Event.setDamageAmount(0.0f);
        Target.hurt(Target.damageSources().source(Scp151Registry.DrowningDamage), DrownDamage);
    }

    private static void play(LivingEntity Target, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        SoundSource Source = Target instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Target.level().playSound(null, Target.getX(), Target.getEyeY(), Target.getZ(), Layer.get(), Source, Volume, Pitch);
        }
    }

    public static int state(ServerPlayer User) {
        return (isGazing(User) ? 1 : 0) | (isRecovering(User) ? 2 : 0);
    }

    public static void sync(ServerPlayer User) {
        int State = state(User);
        int Sent = User.getData(Scp151Registry.SentState);
        if (State == Sent) {
            return;
        }
        User.setData(Scp151Registry.SentState, State);
        PacketDistributor.sendToPlayer(User, new Scp151BreathPayload((State & 1) != 0, (State & 2) != 0));
    }

    public static void resend(ServerPlayer User) {
        User.setData(Scp151Registry.SentState, -1);
        sync(User);
    }
}
