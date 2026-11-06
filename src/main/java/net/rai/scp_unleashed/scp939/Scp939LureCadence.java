package net.rai.scp_unleashed.scp939;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.radio.VoiceConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public final class Scp939LureCadence {
    public static final double StalkRange = 32.0;
    public static final double IdleRange = 48.0;
    public static final int CoverWait = 40;
    public static final int ShiftReach = 6;
    public static final int ShiftRise = 2;
    public static final int ShiftTries = 4;
    public static final double ShiftModifier = 0.45;
    private static final Map<Scp939, Plan> Plans = new WeakHashMap<>();

    private Scp939LureCadence() {
    }

    public enum State {
        Quiet,
        Idle,
        Stalking
    }

    private static final class Plan {
        private long Due = -1L;
        private long Deadline = -1L;
        private State Last = State.Quiet;
    }

    public static State state(ServerLevel World, Scp939 Hunter) {
        if (!Hunter.isAlive() || Hunter.isBaby() || Hunter.isBusy() || Hunter.isLuring()) {
            return State.Quiet;
        }
        LivingEntity Target = Hunter.getTarget();
        if (Target != null && Target.isAlive()) {
            double Distance = Hunter.distanceTo(Target);
            return Distance <= StalkRange && Distance > Hunter.lungeRange() ? State.Stalking : State.Quiet;
        }
        if (Scp939VoiceAttraction.remembered(Hunter, World.getGameTime()) != null) {
            return State.Stalking;
        }
        return playerNear(World, Hunter) ? State.Idle : State.Quiet;
    }

    private static boolean playerNear(ServerLevel World, Scp939 Hunter) {
        for (Player User : World.players()) {
            if (!User.isSpectator() && !User.isCreative() && User.isAlive() && User.distanceToSqr(Hunter) <= IdleRange * IdleRange) {
                return true;
            }
        }
        return false;
    }

    public static int minTicks(State Which) {
        return 20 * (Which == State.Stalking ? VoiceConfig.lureMinSeconds() : VoiceConfig.idleLureMinSeconds());
    }

    public static int maxTicks(State Which) {
        return 20 * (Which == State.Stalking ? VoiceConfig.lureMaxSeconds() : VoiceConfig.idleLureMaxSeconds());
    }

    public static int interval(State Which, RandomSource Random) {
        return Mth.nextInt(Random, minTicks(Which), maxTicks(Which));
    }

    private static void schedule(Plan Current, State Which, long Now, RandomSource Random, boolean Early) {
        int Drawn = interval(Which, Random);
        Current.Due = Now + (Early ? Drawn / 2 : Drawn);
        Current.Deadline = Now + (Early ? maxTicks(Which) / 2 : maxTicks(Which));
    }

    public static boolean tick(ServerLevel World, Scp939 Hunter, long Now) {
        State Which = state(World, Hunter);
        Plan Current = Plans.computeIfAbsent(Hunter, Key -> new Plan());
        if (Which == State.Quiet) {
            Current.Due = -1L;
            Current.Last = State.Quiet;
            return false;
        }
        if (Current.Due < 0L || Which != Current.Last && Current.Deadline - Now > maxTicks(Which)) {
            schedule(Current, Which, Now, Hunter.getRandom(), true);
        }
        Current.Last = Which;
        if (Now < Current.Due) {
            return false;
        }
        if (Now < Current.Deadline && seen(Hunter)) {
            Current.Due = Math.min(Current.Deadline, Now + CoverWait);
            return false;
        }
        Hunter.lureCall(World);
        schedule(Current, Which, Now, Hunter.getRandom(), false);
        if (Which == State.Idle || Hunter.getTarget() == null) {
            shift(World, Hunter);
        }
        return true;
    }

    private static boolean seen(Scp939 Hunter) {
        LivingEntity Target = Hunter.getTarget();
        return Target != null && Target.hasLineOfSight(Hunter);
    }

    private static void shift(ServerLevel World, Scp939 Hunter) {
        if (Hunter.isNoAi()) {
            return;
        }
        Vec3 Best = null;
        int BestLight = Integer.MAX_VALUE;
        for (int Try = 0; Try < ShiftTries; Try++) {
            Vec3 Found = DefaultRandomPos.getPos(Hunter, ShiftReach, ShiftRise);
            if (Found == null) {
                continue;
            }
            BlockPos Spot = BlockPos.containing(Found);
            int Light = World.getBrightness(LightLayer.BLOCK, Spot) + World.getBrightness(LightLayer.SKY, Spot);
            if (Light < BestLight) {
                Best = Found;
                BestLight = Light;
            }
        }
        if (Best != null) {
            Hunter.getNavigation().moveTo(Best.x, Best.y, Best.z, ShiftModifier);
        }
    }

    @Nullable
    public static Long due(Scp939 Hunter) {
        Plan Current = Plans.get(Hunter);
        return Current == null || Current.Due < 0L ? null : Current.Due;
    }

    public static void forget(Scp939 Hunter) {
        Plans.remove(Hunter);
    }
}
