package net.rai.scp_unleashed.scp2521;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Set;

public class Scp2521GaitController extends AnimationController<Scp2521> {
    public static final Set<String> StrideClips = Set.of("walk_legs", "walk_upper");
    public static final int Blend = 4;
    public static final int WalkBlend = 6;
    public static final int StrideBlend = 3;
    public static final RawAnimation WalkLegs = RawAnimation.begin().thenLoop("walk_legs");
    public static final RawAnimation StandLegs = RawAnimation.begin().thenLoop("stand_legs");
    public static final RawAnimation WalkUpper = RawAnimation.begin().thenLoop("walk_upper");

    private final boolean Legs;
    private boolean Walking;
    private RawAnimation Chosen;

    public Scp2521GaitController(Scp2521 Owner, boolean Legs) {
        super(Owner, Legs ? "gait" : "body", Blend, Scp2521GaitController::choose);
        this.Legs = Legs;
    }

    private static PlayState choose(AnimationState<Scp2521> Event) {
        return ((Scp2521GaitController) Event.getController()).pick(Event);
    }

    public boolean legs() {
        return Legs;
    }

    public PlayState pick(AnimationState<Scp2521> Event) {
        boolean Striding = animatable.striding();
        if (Legs) {
            return play(Event, Striding, Striding ? WalkLegs : StandLegs);
        }
        boolean Upright = Striding && Scp2521.walksUpright(animatable.mode());
        return play(Event, Upright, Upright ? WalkUpper : Scp2521.animationFor(animatable.mode()));
    }

    private PlayState play(AnimationState<Scp2521> Event, boolean Walk, RawAnimation Wanted) {
        if (Wanted != Chosen) {
            transitionLength(Walk != Walking ? WalkBlend : Blend);
            Walking = Walk;
            Chosen = Wanted;
        }
        return Event.setAndContinue(Wanted);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null || !StrideClips.contains(currentAnimation.animation().name())) {
            return Adjusted;
        }
        double Cycle = animatable.stride().cycle(animatable.renderPartial());
        if (Double.isNaN(Cycle) || Double.isInfinite(Cycle)) {
            return Adjusted;
        }
        return Cycle * currentAnimation.animation().length();
    }
}
