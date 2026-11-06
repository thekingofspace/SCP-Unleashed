package net.rai.scp_unleashed.scp087.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp087.Scp087Hunter;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp087HunterModel extends GeoModel<Scp087Hunter> {
    public static final String Name = "scp087_hunter";
    public static final float HeadYawLimit = 60.0f;
    public static final float HeadPitchLimit = 40.0f;
    public static final float TiltAngle = 34.0f;
    public static final float TwitchAngle = 7.0f;
    private static final ResourceLocation ModelPath = location("geo/entity/scp087/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = location("textures/entity/scp087/" + Name + ".png");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp087/" + Name + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp087Hunter Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp087Hunter Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp087Hunter Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp087Hunter Creature, long InstanceId, AnimationState<Scp087Hunter> Animated) {
        GeoBone Head = getAnimationProcessor().getBone("head");
        if (Head == null || Creature.isDeadOrDying()) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data != null && Creature.action() != Scp087Hunter.ActionLunge) {
            float Pitch = Mth.clamp(Data.headPitch(), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD;
            float Yaw = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD;
            Head.setRotX(Head.getRotX() + Pitch);
            Head.setRotY(Head.getRotY() + Yaw);
        }
        float Time = Creature.tickCount + Animated.getPartialTick();
        if (Creature.tilt() != 0) {
            Head.setRotZ(Head.getRotZ() + Creature.tilt() * TiltAngle * Mth.DEG_TO_RAD);
        }
        float Twitch = twitch(Creature.getId(), Time);
        Head.setRotZ(Head.getRotZ() + Twitch * TwitchAngle * Mth.DEG_TO_RAD);
        Head.setRotY(Head.getRotY() + Twitch * 0.6f * TwitchAngle * Mth.DEG_TO_RAD);
    }

    public static float twitch(int Seed, float Time) {
        int Window = (int) Math.floor(Time / 23.0f);
        int Mixed = (Window * 73856093) ^ (Seed * 19349663);
        if ((Mixed & 7) != 0) {
            return 0.0f;
        }
        float Phase = Time - Window * 23.0f;
        if (Phase > 5.0f) {
            return 0.0f;
        }
        float Side = (Mixed & 8) == 0 ? 1.0f : -1.0f;
        return Side * Mth.sin(Phase / 5.0f * Mth.PI);
    }
}
