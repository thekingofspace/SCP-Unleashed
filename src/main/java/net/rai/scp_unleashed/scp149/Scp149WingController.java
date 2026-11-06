package net.rai.scp_unleashed.scp149;

import software.bernie.geckolib.animation.AnimationController;

public class Scp149WingController extends AnimationController<Scp149Fly> {
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 1;

    public Scp149WingController(Scp149Fly Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp149Fly> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp149Fly.ActionNone ? LoopBlend : ActionBlend;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                return animatable.actionElapsed();
            }
        }
        return super.adjustTick(Tick);
    }
}
