package net.rai.scp_unleashed.scp346;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

public class Scp346Controller extends AnimationController<Scp346Pterosaur> {
    private float Partial;

    public Scp346Controller(Scp346Pterosaur Bird, String Name, int Blend, AnimationStateHandler<Scp346Pterosaur> Handler) {
        super(Bird, Name, Blend, Handler);
    }

    @Override
    public void process(GeoModel<Scp346Pterosaur> Model, AnimationState<Scp346Pterosaur> Animated, Map<String, GeoBone> Bones, Map<String, BoneSnapshot> Snapshots,
                        double SeekTime, boolean Crash) {
        Partial = Animated.getPartialTick();
        super.process(Model, Animated, Bones, Snapshots, SeekTime, Crash);
    }

    @Override
    protected double adjustTick(double Tick) {
        double Adjusted = super.adjustTick(Tick);
        if (animationState == State.TRANSITIONING || currentAnimation == null) {
            return Adjusted;
        }
        String Name = currentAnimation.animation().name();
        double Length = currentAnimation.animation().length();
        if (Name.equals(animatable.actionClip())) {
            animatable.actionClock().learn(Name, Length + transitionLength);
            return Adjusted;
        }
        if (Scp346Pterosaur.WalkClip.equals(Name) && animationState == State.RUNNING && Length > 0.0) {
            double Walked = animatable.walkTicks(Partial);
            return Walked - Math.floor(Walked / Length) * Length;
        }
        return Adjusted;
    }
}
