package net.rai.scp_unleashed.scp3199.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp3199.Scp3199;
import net.rai.scp_unleashed.scp3199.Scp3199Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp3199GeoModel extends SharedGeoModel<Scp3199> {
    public static final ResourceLocation ModelPath = Scp3199Registry.location("geo/entity/scp3199/scp3199.geo.json");
    public static final ResourceLocation TexturePath = Scp3199Registry.location("textures/entity/scp3199/scp3199.png");
    public static final ResourceLocation AnimationPath = Scp3199Registry.location("animations/entity/scp3199/scp3199.animation.json");
    public static final float YawLimit = 40.0f;
    public static final float PitchLimit = 25.0f;

    @Override
    public ResourceLocation getModelResource(Scp3199 Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp3199 Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp3199 Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp3199 Owner, long InstanceId, AnimationState<Scp3199> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Egg = getAnimationProcessor().getBone("egg");
        if (Egg != null) {
            Egg.setHidden(Owner.eggHandedOver());
        }
        GeoBone Look = getAnimationProcessor().getBone("look");
        if (Look == null) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        float Yaw = 0.0f;
        float Pitch = 0.0f;
        if (Data != null && Owner.canAct() && !Owner.isSwimmingNow()) {
            Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
            Pitch = Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit);
        }
        Look.setRotY(Look.getInitialSnapshot().getRotY() + Yaw * Mth.DEG_TO_RAD);
        Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
    }
}
