package net.rai.scp_unleashed.scp017.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp017.Scp017Shadow;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp017GeoModel extends GeoModel<Scp017Shadow> {
    public static final ResourceLocation ModelPath = location("geo/entity/scp017/shadow.geo.json");
    public static final ResourceLocation TexturePath = location("textures/entity/scp017/shadow.png");
    public static final ResourceLocation EyesPath = location("textures/entity/scp017/shadow_eyes.png");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp017/shadow.animation.json");
    public static final String LookBone = "look";
    public static final float PitchLimit = 30.0f;
    public static final float YawLimit = 50.0f;

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp017Shadow Shadow) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp017Shadow Shadow) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp017Shadow Shadow) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp017Shadow Shadow, long InstanceId, AnimationState<Scp017Shadow> Animated) {
        if (Shadow.locksMovement() || Shadow.isDeadOrDying()) {
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
