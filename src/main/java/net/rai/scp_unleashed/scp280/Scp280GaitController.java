package net.rai.scp_unleashed.scp280;

import software.bernie.geckolib.animation.AnimationController;

public class Scp280GaitController extends AnimationController<Scp280Shade> {
    public Scp280GaitController(Scp280Shade Shade, String Name, int TransitionTicks, AnimationStateHandler<Scp280Shade> Handler) {
        super(Shade, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        Scp280Gait Gait = animatable.gaitFor(currentAnimation.animation().name());
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
