package net.rai.scp_unleashed.scp016;

import software.bernie.geckolib.animation.AnimationController;

public class Scp016GaitController<T extends Scp016Spawn> extends AnimationController<T> {
    public static final String GaitAnimation = "walk";

    public Scp016GaitController(T Creature, String Name, int TransitionTicks, AnimationStateHandler<T> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        Scp016Gait Gait = animatable.gait();
        if (Gait == null || animationState != State.RUNNING || currentAnimation == null || !GaitAnimation.equals(currentAnimation.animation().name())) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
