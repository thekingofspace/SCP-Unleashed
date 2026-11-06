package net.rai.scp_unleashed.scp106;

import software.bernie.geckolib.animation.AnimationController;

public class Scp106Controller extends AnimationController<Scp106OldMan> {
    public static final String WalkAnimation = "walk";
    public static final String LoopedAction = "corrode";
    public static final int LoopBlend = 5;
    public static final int ActionBlend = 3;

    public Scp106Controller(Scp106OldMan Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp106OldMan> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp106OldMan.ActionNone || Action == Scp106OldMan.ActionHidden ? LoopBlend : ActionBlend;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            String Name = currentAnimation.animation().name();
            if (Clip != null && Clip.equals(Name) && !"idle".equals(Clip)) {
                double Length = Math.max(1.0, currentAnimation.animation().length());
                double Ticks = animatable.actionElapsed() * 20.0;
                return LoopedAction.equals(Name) ? Ticks % Length : Math.min(Ticks, Length);
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        Scp106Gait Walking = animatable.gait();
        if (Walking == null || !WalkAnimation.equals(currentAnimation.animation().name())) {
            return Adjusted;
        }
        return Walking.cycle() * currentAnimation.animation().length();
    }
}
