package net.rai.scp_unleashed.scp096;

import software.bernie.geckolib.animation.AnimationController;

public class Scp096Controller extends AnimationController<Scp096ShyGuy> {
    private String Rated;
    private double RatedStart;

    public Scp096Controller(Scp096ShyGuy Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp096ShyGuy> Handler) {
        super(Owner, Name, TransitionTicks, Handler);
    }

    public boolean playing(String Clip) {
        return currentAnimation != null && Clip.equals(currentAnimation.animation().name()) && animationState != State.STOPPED;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.bodyClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name()) && animatable.timedClip()) {
                return animatable.actionElapsed();
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            Rated = null;
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        if (animatable.isDistressed() && (Name.equals("distress") || Name.equals("distress_peak"))) {
            double Phase = animatable.distressPhase();
            if (!Name.equals(Rated)) {
                Rated = Name;
                RatedStart = Phase;
            }
            return (Phase - RatedStart) % currentAnimation.animation().length();
        }
        Rated = null;
        Scp096Gait Gait = animatable.gaitFor(currentAnimation.animation().name());
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
