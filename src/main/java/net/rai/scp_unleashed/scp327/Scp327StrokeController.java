package net.rai.scp_unleashed.scp327;

import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

public class Scp327StrokeController extends AnimationController<Scp327Siren> {
    private float Partial;

    public Scp327StrokeController(Scp327Siren Creature, String Name, AnimationStateHandler<Scp327Siren> Handler) {
        super(Creature, Name, 0, Handler);
    }

    @Override
    public void process(GeoModel<Scp327Siren> Model, AnimationState<Scp327Siren> Animated, Map<String, GeoBone> Bones, Map<String, BoneSnapshot> Snapshots, double SeekTime,
                        boolean Crash) {
        Partial = Animated.getPartialTick();
        super.process(Model, Animated, Bones, Snapshots, SeekTime, Crash);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Base = super.adjustTick(Tick);
        AnimationProcessor.QueuedAnimation Current = getCurrentAnimation();
        if (getAnimationState() != State.RUNNING || Current == null || !Scp327Siren.isStroke(Current.animation().name())) {
            return Base;
        }
        double Phase = Mth.lerp(Partial, animatable.StrokePhaseO, animatable.StrokePhase);
        return (Phase - Math.floor(Phase)) * Current.animation().length();
    }
}
