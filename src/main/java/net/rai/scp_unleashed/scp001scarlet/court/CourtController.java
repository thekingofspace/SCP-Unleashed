package net.rai.scp_unleashed.scp001scarlet.court;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;

public class CourtController<T extends GeoAnimatable & CourtStrider> extends AnimationController<T> {
    public CourtController(T Owner, String Name, int TransitionTicks, AnimationStateHandler<T> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        double Stride = animatable.strideFor(currentAnimation.animation().name());
        if (Stride <= 0.0) {
            return Adjusted;
        }
        return animatable.cycle(Stride) * currentAnimation.animation().length();
    }
}
