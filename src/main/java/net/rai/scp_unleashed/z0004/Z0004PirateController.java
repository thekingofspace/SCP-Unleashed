package net.rai.scp_unleashed.z0004;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Z0004PirateController extends AnimationController<Z0004Pirate> {
    public static final Set<String> StrideClips = Set.of("walk", "run");
    public static final int Blend = 4;

    public Z0004PirateController(Z0004Pirate Owner) {
        super(Owner, "body", Blend, State -> State.setAndContinue(Owner.bodyAnimation()));
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
