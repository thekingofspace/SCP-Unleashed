package net.rai.scp_unleashed.stranger.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class StrangerModel extends GeoModel<StrangerEntity> {
    public static final String LookBone = "look";

    @Override
    public ResourceLocation getModelResource(StrangerEntity Figure) {
        return Figure.variant().Model();
    }

    @Override
    public ResourceLocation getTextureResource(StrangerEntity Figure) {
        return Figure.variant().Texture();
    }

    @Override
    public ResourceLocation getAnimationResource(StrangerEntity Figure) {
        return Figure.variant().Animations();
    }

    @Override
    public void setCustomAnimations(StrangerEntity Figure, long InstanceId, AnimationState<StrangerEntity> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -35.0f, 35.0f) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -60.0f, 60.0f) * Mth.DEG_TO_RAD);
    }
}
