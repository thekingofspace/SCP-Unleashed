package net.rai.scp_unleashed.scp465;

import net.rai.scp_unleashed.dclass.HumanoidGaitController;

public class Scp465BeatController extends HumanoidGaitController<Scp465Guest> {
    public Scp465BeatController(Scp465Guest Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp465Guest> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        if (!Scp465Guest.isBeatClip(currentAnimation.animation().name())) {
            return Adjusted;
        }
        double Length = currentAnimation.animation().length();
        if (Length <= 0.0) {
            return Adjusted;
        }
        double Phase = animatable.beatTicks() % Length;
        return Phase < 0.0 ? Phase + Length : Phase;
    }
}
