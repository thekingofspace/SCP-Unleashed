package net.rai.scp_unleashed.scp2845;

import software.bernie.geckolib.animation.AnimationController;

public class Scp2845SpiritGait extends AnimationController<Scp2845Spirit> {
    public Scp2845SpiritGait(Scp2845Spirit Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp2845Spirit> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null || !"gallop".equals(currentAnimation.animation().name())) {
            return Adjusted;
        }
        return animatable.cycle(animatable.renderPartial()) * currentAnimation.animation().length();
    }
}
