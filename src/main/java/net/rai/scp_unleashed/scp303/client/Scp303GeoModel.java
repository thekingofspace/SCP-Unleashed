package net.rai.scp_unleashed.scp303.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp303.Scp303Doorman;
import net.rai.scp_unleashed.scp303.Scp303Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp303GeoModel extends GeoModel<Scp303Doorman> {
    public static final ResourceLocation ModelPath = Scp303Registry.location("geo/entity/scp303/doorman.geo.json");
    public static final ResourceLocation TexturePath = Scp303Registry.location("textures/entity/scp303/doorman.png");
    public static final ResourceLocation AnimationPath = Scp303Registry.location("animations/entity/scp303/doorman.animation.json");
    public static final String LookBone = "look";
    public static final float PitchLimit = 35.0f;
    public static final float YawLimit = 50.0f;

    @Override
    public ResourceLocation getModelResource(Scp303Doorman Doorman) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp303Doorman Doorman) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp303Doorman Doorman) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp303Doorman Doorman, long InstanceId, AnimationState<Scp303Doorman> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look == null) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Doorman.locksMovement() || Doorman.isRunning() || Data == null) {
            Look.setRotX(0.0f);
            Look.setRotY(0.0f);
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
    }
}
