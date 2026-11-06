package net.rai.scp_unleashed.scp075;

import software.bernie.geckolib.animation.AnimationController;

public class Scp075GaitController extends AnimationController<Scp075Snail> {
    public static final String CrawlAnimation = "crawl";
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 3;

    public Scp075GaitController(Scp075Snail Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp075Snail> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp075Snail.ActionNone ? LoopBlend : ActionBlend;
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
        if (animationState != State.RUNNING || currentAnimation == null || !CrawlAnimation.equals(currentAnimation.animation().name())) {
            return Adjusted;
        }
        Scp075Gait Gait = animatable.gait();
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
