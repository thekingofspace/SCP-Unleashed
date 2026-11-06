package net.rai.scp_unleashed.scp372;

import software.bernie.geckolib.animation.AnimationController;

public class Scp372GaitController extends AnimationController<Scp372Insect> {
    public static final String WalkAnimation = "walk";
    public static final String SkitterAnimation = "skitter";
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;

    public Scp372GaitController(Scp372Insect Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp372Insect> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(byte Action) {
        if (Action == Scp372Insect.ActionMaterialize || Action == Scp372Insect.ActionFlicker) {
            return 0;
        }
        return Action == Scp372Insect.ActionNone ? LoopBlend : ActionBlend;
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
        Scp372Gait Gait = animatable.gait();
        if (Gait == null || !WalkAnimation.equals(Name) && !SkitterAnimation.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
