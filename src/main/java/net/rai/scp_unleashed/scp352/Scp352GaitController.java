package net.rai.scp_unleashed.scp352;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Scp352GaitController extends AnimationController<Scp352Crone> {
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;
    private static final Map<String, RawAnimation> Plays = new ConcurrentHashMap<>();
    private float Partial;
    private boolean Started;

    public Scp352GaitController(Scp352Crone Creature, String Name, AnimationStateHandler<Scp352Crone> Handler) {
        super(Creature, Name, 0, Handler);
    }

    public static RawAnimation play(String Clip) {
        return Plays.computeIfAbsent(Clip, Name -> RawAnimation.begin().thenPlayAndHold(Name));
    }

    public void blend(int Ticks) {
        transitionLength(Started ? Ticks : 0);
    }

    @Override
    public void process(GeoModel<Scp352Crone> Model, AnimationState<Scp352Crone> Animated, Map<String, GeoBone> Bones, Map<String, BoneSnapshot> Snapshots,
                        double SeekTime, boolean Crash) {
        Partial = Animated.getPartialTick();
        animatable.setRenderPartial(Partial);
        super.process(Model, Animated, Bones, Snapshots, SeekTime, Crash);
        Started = getCurrentAnimation() != null;
    }

    @Override
    protected double adjustTick(double Tick) {
        double Base = super.adjustTick(Tick);
        AnimationProcessor.QueuedAnimation Current = getCurrentAnimation();
        if (getAnimationState() != State.RUNNING || Current == null) {
            return Base;
        }
        String Name = Current.animation().name();
        if (Name.startsWith("death")) {
            return animatable.deathTime + Partial;
        }
        if (Name.equals("leap")) {
            return animatable.leapClipTick();
        }
        if (Name.equals(animatable.actionClip())) {
            return animatable.actionElapsed();
        }
        Scp352Phase Phase = animatable.phase();
        if (Phase.timed() && Name.equals(Phase.clip())) {
            return animatable.phaseElapsed();
        }
        Scp352Gait Gait = Scp352Gait.byClip(Name);
        if (Gait == null || !Gait.moving()) {
            return Base;
        }
        double Value = animatable.gaitPhase(Partial);
        return (Value - Math.floor(Value)) * Current.animation().length();
    }
}
