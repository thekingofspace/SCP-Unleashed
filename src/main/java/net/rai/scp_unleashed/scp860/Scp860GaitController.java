package net.rai.scp_unleashed.scp860;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp860GaitController extends AnimationController<Scp860Watcher> {
    public static final Set<String> StrideClips = Set.of("walk", "stalk", "run");

    public Scp860GaitController(Scp860Watcher Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp860Watcher> Handler) {
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
