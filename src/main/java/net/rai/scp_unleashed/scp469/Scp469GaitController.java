package net.rai.scp_unleashed.scp469;

import software.bernie.geckolib.animation.AnimationController;

public class Scp469GaitController extends AnimationController<Scp469Angel> {
    public static final String WalkAnimation = "walk";
    public static final String RunAnimation = "run";
    public static final String SqueezeAnimation = "squeeze";
    public static final String BurstAnimation = "burst";
    public static final int LoopBlend = 4;
    public static final int ActionBlend = 3;
    public static final int FoldBlend = 6;

    public Scp469GaitController(Scp469Angel Creature, String Name, int TransitionTicks, AnimationStateHandler<Scp469Angel> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public static int blendFor(Scp469Angel Creature) {
        if (Creature.isDeadOrDying() || Creature.isReforming()) {
            return 1;
        }
        if (Creature.action() != Scp469Angel.ActionNone) {
            return ActionBlend;
        }
        return Creature.isFolded() ? FoldBlend : LoopBlend;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Name = currentAnimation.animation().name();
            if (BurstAnimation.equals(Name) && animatable.isDeadOrDying()) {
                return animatable.deathTime + animatable.renderPartial();
            }
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(Name)) {
                return animatable.actionElapsed();
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        Scp469Gait Gait = animatable.gait();
        if (Gait == null || !WalkAnimation.equals(Name) && !RunAnimation.equals(Name) && !SqueezeAnimation.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
