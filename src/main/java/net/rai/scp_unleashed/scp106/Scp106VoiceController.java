package net.rai.scp_unleashed.scp106;

import software.bernie.geckolib.animation.AnimationController;

public class Scp106VoiceController extends AnimationController<Scp106OldMan> {
    public static final String QuietAnimation = "quiet";

    public Scp106VoiceController(Scp106OldMan Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp106OldMan> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null && !QuietAnimation.equals(currentAnimation.animation().name())
                && (animatable.laughing() || animatable.breathing())) {
            double Length = Math.max(1.0, currentAnimation.animation().length());
            return Math.min(animatable.voiceElapsed() * 20.0, Length);
        }
        return super.adjustTick(Tick);
    }
}
