package net.rai.scp_unleashed.scp407;

import net.minecraft.world.entity.Mob;
import net.rai.scp_unleashed.motion.ActionClock;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationController;

public class Scp407GaitController<T extends Mob & GeoEntity & Scp407Animated> extends AnimationController<T> {
    public static final String WalkAnimation = "walk";
    public static final String RunAnimation = "run";
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;

    public Scp407GaitController(T Creature, String Name, int TransitionTicks, AnimationStateHandler<T> Handler) {
        super(Creature, Name, TransitionTicks, Handler);
    }

    public boolean playing(String Clip) {
        return currentAnimation != null && Clip.equals(currentAnimation.animation().name()) && animationState != State.STOPPED;
    }

    @Override
    protected double adjustTick(double Tick) {
        if (animationState != State.TRANSITIONING && currentAnimation != null) {
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                if (animatable instanceof ActionClock.Owner Owner) {
                    Owner.actionClock().learn(Clip, currentAnimation.animation().length());
                }
                return ActionClock.catchUp(animatable.actionElapsed(), transitionLength);
            }
        }
        double Adjusted = super.adjustTick(Tick);
        if (animationState != State.RUNNING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        Scp407Gait Gait = animatable.gait();
        if (Gait == null || !WalkAnimation.equals(Name) && !RunAnimation.equals(Name)) {
            return Adjusted;
        }
        return Gait.cycle() * currentAnimation.animation().length();
    }
}
