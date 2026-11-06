package net.rai.scp_unleashed.scp205.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp205.Scp205Devil;
import net.rai.scp_unleashed.scp205.Scp205Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp205DevilGeoModel extends GeoModel<Scp205Devil> {
    public static final ResourceLocation ModelPath = Scp205Registry.location("geo/entity/scp205/devil.geo.json");
    public static final ResourceLocation TexturePath = Scp205Registry.location("textures/entity/scp205/devil.png");
    public static final ResourceLocation EyesPath = Scp205Registry.location("textures/entity/scp205/devil_eyes.png");
    public static final ResourceLocation AnimationPath = Scp205Registry.location("animations/entity/scp205/devil.animation.json");
    public static final String LookBone = "look";
    public static final float PitchLimit = 35.0f;
    public static final float YawLimit = 55.0f;

    @Override
    public ResourceLocation getModelResource(Scp205Devil Devil) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp205Devil Devil) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp205Devil Devil) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp205Devil Devil, long InstanceId, AnimationState<Scp205Devil> Animated) {
        if (Devil.busy()) {
            return;
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
    }
}
