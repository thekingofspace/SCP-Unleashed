package net.rai.scp_unleashed.scp999;

import net.rai.scp_unleashed.motion.ActionClock;
import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp999GaitController extends AnimationController<Scp999Blob> {
    public static final Set<String> StrideClips = Set.of("hop", "bound");
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 3;

    public Scp999GaitController(Scp999Blob Blob, String Name, int TransitionTicks, AnimationStateHandler<Scp999Blob> Handler) {
        super(Blob, Name, TransitionTicks, Handler);
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                animatable.actionClock().learn(Clip, currentAnimation.animation().length());
                return ActionClock.catchUp(animatable.actionElapsed(), transitionLength);
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        if (!StrideClips.contains(currentAnimation.animation().name())) {
            return Adjusted;
        }
        return animatable.stride().cycle(animatable.renderPartial()) * currentAnimation.animation().length();
    }
}
