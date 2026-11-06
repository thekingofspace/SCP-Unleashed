package net.rai.scp_unleashed.scp492.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp492.Scp492Figure;
import net.rai.scp_unleashed.scp492.Scp492Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp492GeoModel extends SharedGeoModel<Scp492Figure> {
    public static final ResourceLocation ModelPath = Scp492Registry.location("geo/entity/scp492/scp492_figure.geo.json");
    public static final ResourceLocation TexturePath = Scp492Registry.location("textures/entity/scp492/scp492_figure.png");
    public static final ResourceLocation AnimationPath = Scp492Registry.location("animations/entity/scp492/scp492_figure.animation.json");
    public static final String LookBone = "look";
    public static final float LookUp = 14.0f;
    public static final float LookDown = 6.0f;
    public static final float LookTurn = 40.0f;

    @Override
    public ResourceLocation getModelResource(Scp492Figure Figure) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp492Figure Figure) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp492Figure Figure) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp492Figure Figure, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    public static boolean looking(Scp492Figure Figure) {
        return Figure.isAlive() && !Figure.isTorn() && !Figure.isInSittingPose() && Figure.actionClip() == null && !(Figure.isAfraid() && !Figure.isStriding());
    }

    @Override
    public void setCustomAnimations(Scp492Figure Figure, long InstanceId, AnimationState<Scp492Figure> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look == null) {
            return;
        }
        float Pitch = 0.0f;
        float Yaw = 0.0f;
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data != null && looking(Figure)) {
            Pitch = Mth.clamp(Data.headPitch(), -LookUp, LookDown);
            Yaw = Mth.clamp(Data.netHeadYaw(), -LookTurn, LookTurn);
        }
        Look.setRotX(-Pitch * Mth.DEG_TO_RAD);
        Look.setRotY(-Yaw * Mth.DEG_TO_RAD);
        Look.setRotZ(0.0f);
    }
}
