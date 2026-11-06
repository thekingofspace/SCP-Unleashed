package net.rai.scp_unleashed.scp001;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

public class Scp001GaitController extends AnimationController<Scp001Prototype> {
    private float Partial;

    public Scp001GaitController(Scp001Prototype Creature, String Name, AnimationStateHandler<Scp001Prototype> Handler) {
        super(Creature, Name, 0, Handler);
    }

    @Override
    public void process(GeoModel<Scp001Prototype> Model, AnimationState<Scp001Prototype> Animated, Map<String, GeoBone> Bones, Map<String, BoneSnapshot> Snapshots,
                        double SeekTime, boolean Crash) {
        Partial = Animated.getPartialTick();
        super.process(Model, Animated, Bones, Snapshots, SeekTime, Crash);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Base = super.adjustTick(Tick);
        AnimationProcessor.QueuedAnimation Current = getCurrentAnimation();
        if (getAnimationState() != State.RUNNING || Current == null) {
            return Base;
        }
        Scp001Gait Gait = Scp001Gait.byClip(Current.animation().name());
        if (Gait == null) {
            return Base;
        }
        double Phase = animatable.gaitPhase(Gait, Partial);
        return (Phase - Math.floor(Phase)) * Current.animation().length();
    }
}
