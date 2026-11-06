package net.rai.scp_unleashed.scp682;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Set;

public class Scp682GaitController extends AnimationController<Scp682> {
    public static final Set<String> StrideClips = Set.of("walk", "run");
    public static final int Blend = 6;
    public static final int SwimEnter = 4;
    public static final int SwimLeave = 6;
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    public static final RawAnimation SwimLoop = RawAnimation.begin().thenLoop("swim");
    public static final int LieSpan = 24 + Blend;
    public static final RawAnimation SettleHold = RawAnimation.begin().thenPlayAndHold("settle");
    public static final RawAnimation RestLoop = RawAnimation.begin().thenLoop("rest");
    public static final RawAnimation CollapseHold = RawAnimation.begin().thenPlayAndHold("collapse");
    public static final RawAnimation DownLoop = RawAnimation.begin().thenLoop("down");
    public static final RawAnimation RiseHold = RawAnimation.begin().thenPlayAndHold("rise");
    public static final RawAnimation CommandLoop = RawAnimation.begin().thenLoop("command");

    private int SeenTick = Integer.MIN_VALUE;
    private int WetFor;
    private int DryFor;
    private boolean Swimming;
    private String Aligned = "";
    private double Offset;

    public Scp682GaitController(Scp682 Owner) {
        super(Owner, "body", Blend, Scp682GaitController::choose);
    }

    private static PlayState choose(AnimationState<Scp682> Event) {
        return ((Scp682GaitController) Event.getController()).pick(Event);
    }

    private void observe() {
        if (animatable.tickCount == SeenTick) {
            return;
        }
        SeenTick = animatable.tickCount;
        if (animatable.isSwimmingNow()) {
            WetFor++;
            DryFor = 0;
        } else {
            DryFor++;
            WetFor = 0;
        }
        if (!Swimming && WetFor >= SwimEnter) {
            Swimming = true;
        } else if (Swimming && DryFor >= SwimLeave) {
            Swimming = false;
        }
    }

    public boolean swimming() {
        return Swimming;
    }

    public PlayState pick(AnimationState<Scp682> Event) {
        observe();
        if (animatable.isCommanding()) {
            return Event.setAndContinue(CommandLoop);
        }
        if (animatable.isCollapsed()) {
            return Event.setAndContinue(lying("collapse", "down", animatable.downedFor() > LieSpan) ? DownLoop : CollapseHold);
        }
        if (animatable.isRising()) {
            return Event.setAndContinue(RiseHold);
        }
        if (Swimming) {
            return Event.setAndContinue(SwimLoop);
        }
        if (animatable.isStriding()) {
            return Event.setAndContinue(animatable.isRunning() ? RunLoop : WalkLoop);
        }
        if (animatable.isCalm()) {
            return Event.setAndContinue(lying("settle", "rest", animatable.calmFor() > LieSpan) ? RestLoop : SettleHold);
        }
        return Event.setAndContinue(IdleLoop);
    }

    private boolean lying(String Into, String Held, boolean Late) {
        if (currentAnimation == null) {
            return Late;
        }
        String Playing = currentAnimation.animation().name();
        if (Playing.equals(Held)) {
            return true;
        }
        if (Playing.equals(Into)) {
            return animationState == State.PAUSED;
        }
        return Late;
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null || !StrideClips.contains(currentAnimation.animation().name())) {
            Aligned = "";
            return Adjusted;
        }
        double Cycle = animatable.stride().cycle(animatable.renderPartial());
        if (Double.isNaN(Cycle) || Double.isInfinite(Cycle)) {
            return Adjusted;
        }
        String Playing = currentAnimation.animation().name();
        if (!Playing.equals(Aligned)) {
            Aligned = Playing;
            Offset = Cycle;
        }
        double Shifted = Cycle - Offset;
        return (Shifted - Math.floor(Shifted)) * currentAnimation.animation().length();
    }
}
