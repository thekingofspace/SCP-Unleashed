package net.rai.scp_unleashed.scp3008.staff;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp3008StaffGaitController extends AnimationController<Scp3008Staff> {
    public static final Set<String> StrideClips = Set.of("walk", "prowl", "run");
    public static final int Blend = 4;

    public Scp3008StaffGaitController(Scp3008Staff Staff, String Name, int TransitionTicks, AnimationStateHandler<Scp3008Staff> Handler) {
        super(Staff, Name, TransitionTicks, Handler);
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
