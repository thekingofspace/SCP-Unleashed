package net.rai.scp_unleashed.scp087;

import software.bernie.geckolib.animation.AnimationController;

public class Scp087HunterController extends AnimationController<Scp087Hunter> {
    public static final String WalkAnimation = "walk";
    public static final String RunAnimation = "run";
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 2;

    public Scp087HunterController(Scp087Hunter Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp087Hunter> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp087Hunter.ActionNone ? LoopBlend : ActionBlend;
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
        Scp087Gait Walking = animatable.gait();
        if (Walking == null || !WalkAnimation.equals(Name) && !RunAnimation.equals(Name)) {
            return Adjusted;
        }
        return Walking.cycle() * currentAnimation.animation().length();
    }
}
