package net.rai.scp_unleashed.scp427;

import software.bernie.geckolib.animation.AnimationController;

import java.util.Set;

public class Scp427GaitController extends AnimationController<Scp427Flesh> {
    public static final Set<String> GaitClips = Set.of("walk", "run", "crawl");
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 3;

    public Scp427GaitController(Scp427Flesh Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp427Flesh> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp427Flesh.ActionNone ? LoopBlend : ActionBlend;
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
        Scp427Gait Gait = animatable.gait();
        if (Gait == null || !GaitClips.contains(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
