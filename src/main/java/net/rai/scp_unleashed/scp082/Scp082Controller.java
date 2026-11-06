package net.rai.scp_unleashed.scp082;

import software.bernie.geckolib.animation.AnimationController;

public class Scp082Controller extends AnimationController<Scp082Fernand> {
    public Scp082Controller(Scp082Fernand Owner, String Name, int TransitionTicks, AnimationStateHandler<Scp082Fernand> Handler) {
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
            return Adjusted;
        }
        Scp082Gait Gait = animatable.gaitFor(currentAnimation.animation().name());
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
