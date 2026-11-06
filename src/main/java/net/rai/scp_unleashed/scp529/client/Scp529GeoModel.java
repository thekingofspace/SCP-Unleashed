package net.rai.scp_unleashed.scp529.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp529.Scp529Cat;
import net.rai.scp_unleashed.scp529.Scp529Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp529GeoModel extends SharedGeoModel<Scp529Cat> {
    public static final ResourceLocation ModelPath = Scp529Registry.location("geo/entity/scp529/scp529_cat.geo.json");
    public static final ResourceLocation TexturePath = Scp529Registry.location("textures/entity/scp529/scp529_cat.png");
    public static final ResourceLocation AnimationPath = Scp529Registry.location("animations/entity/scp529/scp529_cat.animation.json");
    public static final String LookBone = "look";
    public static final String[] EyeBones = {"eye_left", "eye_right"};
    public static final float LookUp = 25.0f;
    public static final float LookDown = 20.0f;
    public static final float LookTurn = 50.0f;
    public static final int BlinkPeriod = 97;
    public static final float BlinkTicks = 3.0f;
    public static final float Closed = 0.12f;
    public static final float OpenEnough = 0.5f;

    @Override
    public ResourceLocation getModelResource(Scp529Cat Josie) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp529Cat Josie) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp529Cat Josie) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp529Cat Josie, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    public static boolean looking(Scp529Cat Josie) {
        return Josie.isAlive() && Josie.actionClip() == null && Josie.rest() != Scp529Cat.RestSleep && !Josie.isLying();
    }

    public static boolean blinking(Scp529Cat Josie, float Partial) {
        double Clock = Josie.tickCount + Partial + Math.floorMod(Josie.getId() * 37, BlinkPeriod);
        return Clock % BlinkPeriod < BlinkTicks;
    }

    @Override
    public void setCustomAnimations(Scp529Cat Josie, long InstanceId, AnimationState<Scp529Cat> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look != null) {
            float Pitch = 0.0f;
            float Yaw = 0.0f;
            EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
            if (Data != null && looking(Josie)) {
                Pitch = Mth.clamp(Data.headPitch(), -LookUp, LookDown);
                Yaw = Mth.clamp(Data.netHeadYaw(), -LookTurn, LookTurn);
            }
            Look.setRotX(-Pitch * Mth.DEG_TO_RAD);
            Look.setRotY(-Yaw * Mth.DEG_TO_RAD);
            Look.setRotZ(0.0f);
        }
        if (blinking(Josie, Josie.renderPartial())) {
            for (String Name : EyeBones) {
                GeoBone Eye = getAnimationProcessor().getBone(Name);
                if (Eye != null && Eye.getScaleY() > OpenEnough) {
                    Eye.setScaleY(Closed);
                }
            }
        }
    }
}
