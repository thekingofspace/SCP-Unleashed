package net.rai.scp_unleashed.scp076;

import software.bernie.geckolib.animation.AnimationController;

public class Scp076AbelController extends AnimationController<Scp076Abel> {
    public Scp076AbelController(Scp076Abel Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp076Abel> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.attackClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                return animatable.attackElapsed();
            }
        }
        return super.adjustTick(Tick);
    }
}
