package net.rai.scp_unleashed.scp2845.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp2845.Scp2845;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp2845Model extends SharedGeoModel<Scp2845> {
    public static final ResourceLocation ModelPath = Scp2845Registry.location("geo/entity/scp2845/scp2845.geo.json");
    public static final ResourceLocation TexturePath = Scp2845Registry.location("textures/entity/scp2845/scp2845.png");
    public static final ResourceLocation AnimationPath = Scp2845Registry.location("animations/entity/scp2845/scp2845.animation.json");
    public static final float YawLimit = 40.0f;
    public static final float PitchLimit = 25.0f;

    @Override
    public ResourceLocation getModelResource(Scp2845 Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp2845 Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp2845 Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp2845 Owner, long InstanceId, AnimationState<Scp2845> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Look = getAnimationProcessor().getBone("look");
        if (Look != null) {
            EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
            float Yaw = 0.0f;
            float Pitch = 0.0f;
            if (Data != null && Owner.isAlive() && !Owner.isBowing() && !Owner.isBreakingOut()) {
                Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
                Pitch = Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit);
            }
            Look.setRotY(Look.getInitialSnapshot().getRotY() + Yaw * Mth.DEG_TO_RAD);
            Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
        }
        Scp2845Orbit.apply(getAnimationProcessor(), (Owner.tickCount + Owner.renderPartial()) / 20.0f + (Owner.getId() % 97) * 0.37f);
    }
}
