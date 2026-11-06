package net.rai.scp_unleashed.scp492;

import net.rai.scp_unleashed.motion.ActionClock;
import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp492GaitController extends AnimationController<Scp492Figure> {
    public static final Set<String> StrideClips = Set.of("walk", "run", "panic");
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;

    public Scp492GaitController(Scp492Figure Figure, String Name, int TransitionTicks, AnimationStateHandler<Scp492Figure> Handler) {
        super(Figure, Name, TransitionTicks, Handler);
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
