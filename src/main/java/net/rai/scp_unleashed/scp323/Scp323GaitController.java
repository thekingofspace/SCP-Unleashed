package net.rai.scp_unleashed.scp323;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp323GaitController extends AnimationController<Scp323Beast> {
    public static final Set<String> GaitClips = Set.of("stalk", "sprint", "crawl");
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 3;

    public Scp323GaitController(Scp323Beast Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp323Beast> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        if (Action == Scp323Beast.ActionMorph) {
            return 0;
        }
        return Action == Scp323Beast.ActionNone ? LoopBlend : ActionBlend;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                return animatable.actionElapsed();
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        Scp323Gait Gait = animatable.gait();
        if (Gait == null || !GaitClips.contains(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
