package net.rai.scp_unleashed.scp178.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp178.Scp178Watcher;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp178WatcherModel extends GeoModel<Scp178Watcher> {
    public static final String Name = "scp178_watcher";
    public static final float HeadYawLimit = 55.0f;
    public static final float HeadPitchLimit = 35.0f;
    public static final float TiltAngle = 18.0f;
    public static final float TiltPeriod = 97.0f;
    private static final ResourceLocation ModelPath = location("geo/entity/scp178/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = location("textures/entity/scp178/" + Name + ".png");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp178/" + Name + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp178Watcher Watcher) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp178Watcher Watcher) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp178Watcher Watcher) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp178Watcher Watcher, long InstanceId, AnimationState<Scp178Watcher> Animated) {
        GeoBone Head = getAnimationProcessor().getBone("head");
        if (Head == null || Watcher.deathShare(0.0f) > 0.0f) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data != null && Watcher.action() != Scp178Watcher.ActionLunge) {
            float Pitch = Mth.clamp(Data.headPitch(), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD;
            float Yaw = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD;
            Head.setRotX(Head.getRotX() + Pitch);
            Head.setRotY(Head.getRotY() + Yaw);
        }
        if (!Watcher.isHostile() && Watcher.action() == Scp178Watcher.ActionNone) {
            float Time = Watcher.tickCount + Animated.getPartialTick() + Watcher.getId() * 13.0f;
            float Lean = Mth.sin(Time / TiltPeriod * Mth.TWO_PI);
            float Held = Math.signum(Lean) * Math.min(1.0f, Math.abs(Lean) * 2.2f);
            Head.setRotZ(Head.getRotZ() + Held * TiltAngle * Mth.DEG_TO_RAD);
        }
    }
}
