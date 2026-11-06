package net.rai.scp_unleashed.scp093;

import software.bernie.geckolib.animation.AnimationController;

public class Scp093GaitController extends AnimationController<Scp093Torso> {
    public static final String CrawlAnimation = "crawl";
    public static final String DeathAnimation = "death";
    public static final int LoopBlend = 5;
    public static final int ActionBlend = 3;

    public Scp093GaitController(Scp093Torso Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp093Torso> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        return Action == Scp093Torso.ActionNone ? LoopBlend : ActionBlend;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Name = currentAnimation.animation().name();
            if (DeathAnimation.equals(Name)) {
                return animatable.deathElapsed();
            }
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(Name)) {
                return animatable.actionElapsed();
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null || !CrawlAnimation.equals(currentAnimation.animation().name())) {
            return Adjusted;
        }
        Scp093Gait Gait = animatable.gait();
        if (Gait == null) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
