package net.rai.scp_unleashed.scp049;

import software.bernie.geckolib.animation.AnimationController;

public class Scp049Controller extends AnimationController<Scp049Doctor> {
    private final boolean Gestures;

    public Scp049Controller(Scp049Doctor Doctor, String Name, int TransitionTicks, boolean Gestures, AnimationStateHandler<Scp049Doctor> Handler) {
        super(Doctor, Name, TransitionTicks, Handler);
        this.Gestures = Gestures;
    }

    public boolean playing(String Clip) {
        return currentAnimation != null && Clip.equals(currentAnimation.animation().name()) && animationState != State.STOPPED;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = Gestures ? animatable.gestureClip() : animatable.bodyClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                return animatable.actionElapsed();
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (Gestures || animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        Scp049Gait Gait = animatable.gaitFor(currentAnimation.animation().name());
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
