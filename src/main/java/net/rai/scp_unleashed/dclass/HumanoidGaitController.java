package net.rai.scp_unleashed.dclass;

import software.bernie.geckolib.animation.AnimationController;

public class HumanoidGaitController<T extends DClassLike> extends AnimationController<T> {
    public static final String WalkName = "walk";
    public static final String RunName = "run";

    public HumanoidGaitController(T Owner, String Name, int TransitionTicks, AnimationStateHandler<T> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        HumanoidGait Gait = animatable.gait();
        if (Gait == null || animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        if (!WalkName.equals(Name) && !RunName.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
