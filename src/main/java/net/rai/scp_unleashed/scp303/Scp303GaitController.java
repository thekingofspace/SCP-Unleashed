package net.rai.scp_unleashed.scp303;

import software.bernie.geckolib.animation.AnimationController;

public class Scp303GaitController extends AnimationController<Scp303Doorman> {
    public static final String WalkAnimation = "walk";
    public static final String RunAnimation = "sprint";
    public static final int LoopBlend = 4;
    public static final int FoldBlend = 6;

    public Scp303GaitController(Scp303Doorman Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp303Doorman> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action, boolean Snapped) {
        if (Action == Scp303Doorman.ActionStand || Action == Scp303Doorman.ActionRapid) {
            return 0;
        }
        if (Action == Scp303Doorman.ActionPeek) {
            return Snapped ? 0 : FoldBlend;
        }
        return LoopBlend;
    }

    public boolean playing(String Clip) {
        return currentAnimation != null && Clip.equals(currentAnimation.animation().name()) && animationState != State.STOPPED;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name()) && animatable.timedAction()) {
                return animatable.actionElapsed();
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        Scp303Gait Gait = animatable.gait();
        if (Gait == null || !WalkAnimation.equals(Name) && !RunAnimation.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
