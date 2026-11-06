package net.rai.scp_unleashed.scp2845;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp2845GaitController extends AnimationController<Scp2845> {
    public static final Set<String> StrideClips = Set.of("walk", "gallop", "stride");

    public Scp2845GaitController(Scp2845 Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp2845> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        if (!StrideClips.contains(currentAnimation.animation().name())) {
            return Adjusted;
        }
        return animatable.stride().cycle(animatable.renderPartial()) * currentAnimation.animation().length();
    }
}
