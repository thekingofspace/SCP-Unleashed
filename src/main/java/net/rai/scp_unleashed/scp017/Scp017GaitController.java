package net.rai.scp_unleashed.scp017;

import software.bernie.geckolib.animation.AnimationController;

public class Scp017GaitController extends AnimationController<Scp017Shadow> {
    public Scp017GaitController(Scp017Shadow Shadow, String Name, int TransitionTicks, AnimationStateHandler<Scp017Shadow> Handler) {
        super(Shadow, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        Scp017Gait Gait = animatable.gaitFor(currentAnimation.animation().name());
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
