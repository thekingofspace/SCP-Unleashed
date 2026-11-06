package net.rai.scp_unleashed.scp049.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp049GeoModel extends GeoModel<Scp049Doctor> {
    public static final ResourceLocation ModelPath = location("geo/entity/scp049/doctor.geo.json");
    public static final ResourceLocation TexturePath = location("textures/entity/scp049/doctor.png");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp049/doctor.animation.json");
    public static final String LookBone = "look";
    public static final float PitchLimit = 30.0f;
    public static final float YawLimit = 55.0f;

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp049Doctor Doctor) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp049Doctor Doctor) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp049Doctor Doctor) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp049Doctor Doctor, long InstanceId, AnimationState<Scp049Doctor> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null || Doctor.isDeadOrDying() || Doctor.bodyClip() != null || Doctor.mode() == Scp049Doctor.ModeSurgery) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
    }
}
