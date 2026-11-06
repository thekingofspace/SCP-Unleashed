package net.rai.scp_unleashed.client.geo;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public abstract class SharedGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final SharedBones Shared = new SharedBones();

    @Override
    public void handleAnimations(T Animatable, long InstanceId, AnimationState<T> Animated, float Partial) {
        Shared.animate(InstanceId, getAnimationProcessor().getRegisteredBones(), () -> {
            Animated.animationTick = Double.NaN;
            super.handleAnimations(Animatable, InstanceId, Animated, Partial);
            return !Double.isNaN(Animated.animationTick);
        }, () -> setCustomAnimations(Animatable, InstanceId, Animated));
    }
}
