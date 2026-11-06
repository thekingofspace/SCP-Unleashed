package net.rai.scp_unleashed.scp548;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp548GaitController extends AnimationController<Scp548Spider> {
    public static final Set<String> StrideClips = Set.of("walk", "run");

    public Scp548GaitController(Scp548Spider Spider, String Name, int TransitionTicks, AnimationStateHandler<Scp548Spider> Handler) {
        super(Spider, Name, TransitionTicks, Handler);
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
