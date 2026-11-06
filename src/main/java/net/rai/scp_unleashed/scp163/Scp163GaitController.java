package net.rai.scp_unleashed.scp163;

import software.bernie.geckolib.animation.AnimationController;

public class Scp163GaitController extends AnimationController<Scp163Castaway> {
    public static final String WalkAnimation = "walk";
    public static final String RunAnimation = "run";
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 3;
    public static final int QuirkBlend = 5;

    public Scp163GaitController(Scp163Castaway Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp163Castaway> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        if (Action == Scp163Castaway.ActionApprove || Action == Scp163Castaway.ActionDisapprove) {
            return ActionBlend;
        }
        return Scp163Quirk.byAction(Action) != null ? QuirkBlend : LoopBlend;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                return animatable.actionElapsed();
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        Scp163Gait Gait = animatable.gait();
        if (Gait == null || !WalkAnimation.equals(Name) && !RunAnimation.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
