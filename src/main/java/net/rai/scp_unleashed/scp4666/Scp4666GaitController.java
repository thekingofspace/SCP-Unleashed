package net.rai.scp_unleashed.scp4666;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp4666GaitController extends AnimationController<Scp4666YuleMan> {
    public static final Set<String> StrideClips = Set.of("walk", "run", "drag");
    public static final int Blend = 5;

    public Scp4666GaitController(Scp4666YuleMan Man, String Name, int TransitionTicks, AnimationStateHandler<Scp4666YuleMan> Handler) {
        super(Man, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null || !StrideClips.contains(currentAnimation.animation().name())) {
            return Adjusted;
        }
        return animatable.stride().cycle(animatable.renderPartial()) * currentAnimation.animation().length();
    }
}
