package net.rai.scp_unleashed.scp178;

import software.bernie.geckolib.animation.AnimationController;

public class Scp178WatcherController extends AnimationController<Scp178Watcher> {
    public static final String WalkAnimation = "walk";
    public static final int LoopBlend = 5;
    public static final int ActionBlend = 2;

    public Scp178WatcherController(Scp178Watcher Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp178Watcher> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp178Watcher.ActionNone ? LoopBlend : ActionBlend;
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
        if (animationState != State.RUNNING || currentAnimation == null || !WalkAnimation.equals(currentAnimation.animation().name())) {
            return Adjusted;
        }
        Scp178Gait Walking = animatable.gait();
        if (Walking == null) {
            return Adjusted;
        }
        return Walking.cycle() * currentAnimation.animation().length();
    }
}
