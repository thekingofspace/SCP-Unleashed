package net.rai.scp_unleashed.scp058;

import software.bernie.geckolib.animation.AnimationController;

public class Scp058GaitController extends AnimationController<Scp058Heart> {
    public static final String WalkAnimation = "walk";
    public static final String RunAnimation = "run";
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;

    public Scp058GaitController(Scp058Heart Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp058Heart> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        if (Action == Scp058Heart.ActionEmerge) {
            return 0;
        }
        return Action == Scp058Heart.ActionNone ? LoopBlend : ActionBlend;
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
        Scp058Gait Gait = animatable.gait();
        if (Gait == null || !WalkAnimation.equals(Name) && !RunAnimation.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
