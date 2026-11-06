package net.rai.scp_unleashed.scp019;

import software.bernie.geckolib.animation.AnimationController;

public class Scp019GaitController<T extends Scp019Creature> extends AnimationController<T> {
    public static final String WalkAnimation = "walk";
    public static final String RunAnimation = "run";
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;

    public Scp019GaitController(T Creature, String Name, int TransitionTicks, AnimationStateHandler<T> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        if (Action == Scp019Creature.ActionEmerge || Action == Scp019Creature.ActionForm) {
            return 0;
        }
        return Action == Scp019Creature.ActionNone ? LoopBlend : ActionBlend;
    }

    public boolean playing(String Clip) {
        return currentAnimation != null && Clip.equals(currentAnimation.animation().name()) && animationState != State.STOPPED;
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
        Scp019Gait Gait = animatable.gait();
        if (Gait == null || !WalkAnimation.equals(Name) && !RunAnimation.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
