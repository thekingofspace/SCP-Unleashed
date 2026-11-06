package net.rai.scp_unleashed.scp111;

import software.bernie.geckolib.animation.AnimationController;

public class Scp111GaitController extends AnimationController<Scp111Snail> {
    public static final String GlideAnimation = "glide";
    public static final int LoopBlend = 5;
    public static final int ActionBlend = 3;

    public Scp111GaitController(Scp111Snail Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp111Snail> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp111Snail.ActionNone ? LoopBlend : ActionBlend;
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
        if (animationState != State.RUNNING || currentAnimation == null || !GlideAnimation.equals(currentAnimation.animation().name())) {
            return Adjusted;
        }
        Scp111Gait Gait = animatable.gait();
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
