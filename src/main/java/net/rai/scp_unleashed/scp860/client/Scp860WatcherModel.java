package net.rai.scp_unleashed.scp860.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import net.rai.scp_unleashed.scp860.Scp860Watcher;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp860WatcherModel extends SharedGeoModel<Scp860Watcher> {
    public static final ResourceLocation ModelPath = Scp860Registry.location("geo/entity/scp860/scp860_2.geo.json");
    public static final ResourceLocation TexturePath = Scp860Registry.location("textures/entity/scp860/scp860_2.png");
    public static final ResourceLocation AnimationPath = Scp860Registry.location("animations/entity/scp860/scp860_2.animation.json");
    public static final float YawLimit = 50.0f;
    public static final float PitchLimit = 30.0f;

    @Override
    public ResourceLocation getModelResource(Scp860Watcher Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp860Watcher Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp860Watcher Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp860Watcher Owner, long InstanceId, AnimationState<Scp860Watcher> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Look = getAnimationProcessor().getBone("look");
        if (Look == null) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        float Yaw = 0.0f;
        float Pitch = 0.0f;
        if (Data != null && Owner.isAlive()) {
            Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
            Pitch = Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit);
        }
        Look.setRotY(Look.getInitialSnapshot().getRotY() + Yaw * Mth.DEG_TO_RAD);
        Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
    }
}
