package net.rai.scp_unleashed.scp2521.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp2521.Scp2521;
import net.rai.scp_unleashed.scp2521.Scp2521Motion;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;
import software.bernie.geckolib.animation.AnimationState;

public class Scp2521GeoModel extends SharedGeoModel<Scp2521> {
    public static final ResourceLocation ModelPath = Scp2521Registry.location("geo/entity/scp2521/scp2521.geo.json");
    public static final ResourceLocation TexturePath = Scp2521Registry.location("textures/entity/scp2521/scp2521.png");
    public static final ResourceLocation AnimationPath = Scp2521Registry.location("animations/entity/scp2521/scp2521.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp2521 Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp2521 Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp2521 Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp2521 Owner, long InstanceId, AnimationState<Scp2521> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        float Partial = Animated.getPartialTick();
        Scp2521Motion.Sway Current = Scp2521Motion.sway(Scp2521Motion.phase(Owner.mode(), Owner.modeSeconds(Partial)));
        float Clock = Mth.lerp(Partial, Owner.SwayClockBefore, Owner.SwayClock) + (Owner.getId() % 97) * 0.37f;
        float Gaze = Owner.gazeShown(Partial);
        float Walk = Owner.walkShown(Partial);
        if (Gaze > 0.0f || Walk > 0.0f) {
            Current = new Scp2521Motion.Sway(Current.Amount() + Walk * Scp2521Sway.WalkDrift, Current.Speed(), Current.Coil() * (1.0f - Walk * Scp2521Sway.WalkUncoil)
                    + Gaze * Scp2521Sway.GazeCoil, Current.Flare(), Current.Trail() + Walk * Scp2521Sway.WalkTrail);
        }
        Scp2521Sway.apply(getAnimationProcessor(), Clock, Current);
        Scp2521Sway.attend(getAnimationProcessor(), Gaze, Owner.leanShown(Partial), (Owner.getId() & 1) == 0 ? 1.0f : -1.0f);
    }
}
