package net.rai.scp_unleashed.scp1762;

import software.bernie.geckolib.animation.AnimationController;

public class Scp1762Controller extends AnimationController<Scp1762Dragon> {
    public static final int LoopBlend = 5;

    public Scp1762Controller(Scp1762Dragon Dragon) {
        super(Dragon, "body", LoopBlend, State -> {
            Scp1762Dragon Self = State.getAnimatable();
            String Clip = Self.clip();
            State.getController().transitionLength(Self.transition());
            if (Self.restartClip()) {
                State.getController().forceAnimationReset();
            }
            return State.setAndContinue(Self.holdingClip() ? Scp1762Dragon.hold(Clip) : Scp1762Dragon.loop(Clip));
        });
        setAnimationSpeedHandler(Scp1762Dragon::animSpeed);
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null && animatable.holdingClip() && animatable.clip().equals(currentAnimation.animation().name())) {
            return animatable.clipTicks();
        }
        return super.adjustTick(Tick);
    }
}
