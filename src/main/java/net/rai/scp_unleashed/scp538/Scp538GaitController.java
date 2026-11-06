package net.rai.scp_unleashed.scp538;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp538GaitController extends AnimationController<Scp538Spider> {
    public static final Set<String> StrideClips = Set.of("crawl", "skitter");

    public Scp538GaitController(Scp538Spider Spider, String Name, int TransitionTicks, AnimationStateHandler<Scp538Spider> Handler) {
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
