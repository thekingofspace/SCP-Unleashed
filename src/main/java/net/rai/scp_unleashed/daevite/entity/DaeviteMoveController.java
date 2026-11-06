package net.rai.scp_unleashed.daevite.entity;

import software.bernie.geckolib.animation.AnimationController;

public class DaeviteMoveController<T extends DaeviteMob> extends AnimationController<T> {
    public DaeviteMoveController(T Owner, String Name, int TransitionTicks, AnimationStateHandler<T> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.moveClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name()) && !animatable.loopingMove()) {
                return animatable.moveElapsed();
            }
        }
        return super.adjustTick(Tick);
    }
}
