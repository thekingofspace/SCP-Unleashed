package net.rai.scp_unleashed.scp553;

import software.bernie.geckolib.animation.AnimationController;

public class Scp553WingController extends AnimationController<Scp553Butterfly> {
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 2;

    public Scp553WingController(Scp553Butterfly Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp553Butterfly> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp553Butterfly.ActionNone ? LoopBlend : ActionBlend;
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
