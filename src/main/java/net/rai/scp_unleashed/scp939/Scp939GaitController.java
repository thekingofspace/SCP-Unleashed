package net.rai.scp_unleashed.scp939;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp939GaitController extends AnimationController<Scp939> {
    public static final Set<String> StrideClips = Set.of("walk", "prowl", "sprint");

    public Scp939GaitController(Scp939 Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp939> Handler) {
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
