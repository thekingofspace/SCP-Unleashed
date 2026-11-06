package net.rai.scp_unleashed.scp939.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp939GeoModel extends SharedGeoModel<Scp939> {
    public static final ResourceLocation ModelPath = Scp939Registry.location("geo/entity/scp939/scp939.geo.json");
    public static final ResourceLocation TexturePath = Scp939Registry.location("textures/entity/scp939/scp939.png");
    public static final ResourceLocation AnimationPath = Scp939Registry.location("animations/entity/scp939/scp939.animation.json");
    public static final float YawLimit = 40.0f;
    public static final float PitchLimit = 25.0f;

    @Override
    public ResourceLocation getModelResource(Scp939 Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp939 Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp939 Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp939 Owner, long InstanceId, AnimationState<Scp939> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Look = getAnimationProcessor().getBone("look");
        if (Look == null) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        float Yaw = 0.0f;
        float Pitch = 0.0f;
        if (Data != null && Owner.isAlive() && !Owner.isBusy()) {
            Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
            Pitch = Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit);
        }
        Look.setRotY(Look.getInitialSnapshot().getRotY() + Yaw * Mth.DEG_TO_RAD);
        Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
    }
}
