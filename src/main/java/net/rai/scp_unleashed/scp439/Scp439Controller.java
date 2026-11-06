package net.rai.scp_unleashed.scp439;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

public class Scp439Controller extends AnimationController<Scp439Creature> {
    private float Partial;

    public Scp439Controller(Scp439Creature Creature, String Name, AnimationStateHandler<Scp439Creature> Handler) {
        super(Creature, Name, 0, Handler);
    }

    public boolean playing(String Clip) {
        AnimationProcessor.QueuedAnimation Current = getCurrentAnimation();
        return Current != null && Clip.equals(Current.animation().name()) && getAnimationState() != State.STOPPED;
    }

    @Override
    public void process(GeoModel<Scp439Creature> Model, AnimationState<Scp439Creature> Animated, Map<String, GeoBone> Bones, Map<String, BoneSnapshot> Snapshots,
                        double SeekTime, boolean Crash) {
        Partial = Animated.getPartialTick();
        super.process(Model, Animated, Bones, Snapshots, SeekTime, Crash);
    }

    @Override
    protected double adjustTick(double Tick) {
        AnimationProcessor.QueuedAnimation Current = getCurrentAnimation();
        if (getAnimationState() != State.TRANSITIONING && Current != null) {
            String Name = Current.animation().name();
            if (animatable.deathTime > 0 && "death".equals(Name)) {
                return animatable.deathTime + Partial;
            }
            String Clip = animatable.actionClip();
            if (Clip != null && Clip.equals(Name)) {
                animatable.actionClock().learn(Name, Current.animation().length());
                return animatable.actionElapsed();
            }
        }
        double Base = super.adjustTick(Tick);
        if (getAnimationState() != State.RUNNING || Current == null) {
            return Base;
        }
        String Name = Current.animation().name();
        if (animatable.strideNamed(Name) != null) {
            return animatable.gaitCycle(Partial) * Current.animation().length();
        }
        if ("idle".equals(Name)) {
            return animatable.idleCycle(Partial) * Current.animation().length();
        }
        return Base;
    }
}
