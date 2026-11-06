package net.rai.scp_unleashed.scp027.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp027.Scp027Rat;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp027RatGeoModel extends GeoModel<Scp027Rat> {
    public static final ResourceLocation RatModel = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp027/rat.geo.json");
    public static final ResourceLocation RatTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp027/rat.png");
    public static final ResourceLocation RatAnimation = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/scp027/rat.animation.json");
    public static final float HeadPitchLimit = 30.0f;
    public static final float HeadYawLimit = 50.0f;

    @Override
    public ResourceLocation getModelResource(Scp027Rat Owner) {
        return RatModel;
    }

    @Override
    public ResourceLocation getTextureResource(Scp027Rat Owner) {
        return RatTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp027Rat Owner) {
        return RatAnimation;
    }

    @Override
    public RenderType getRenderType(Scp027Rat Owner, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public void setCustomAnimations(Scp027Rat Owner, long InstanceId, AnimationState<Scp027Rat> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Head = getAnimationProcessor().getBone("head");
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Head != null && Data != null) {
            Head.setRotX(Head.getRotX() + Mth.clamp(Data.headPitch(), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD);
            Head.setRotY(Head.getRotY() + Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD);
        }
    }
}
