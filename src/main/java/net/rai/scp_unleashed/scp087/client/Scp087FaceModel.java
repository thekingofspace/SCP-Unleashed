package net.rai.scp_unleashed.scp087.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp087.Scp087Face;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp087FaceModel extends GeoModel<Scp087Face> {
    public static final String Name = "scp087_face";
    public static final float PitchLimit = 50.0f;
    private static final ResourceLocation ModelPath = Scp087HunterModel.location("geo/entity/scp087/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = Scp087HunterModel.location("textures/entity/scp087/" + Name + ".png");
    private static final ResourceLocation AnimationPath = Scp087HunterModel.location("animations/entity/scp087/" + Name + ".animation.json");

    @Override
    public ResourceLocation getModelResource(Scp087Face Face) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp087Face Face) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp087Face Face) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp087Face Face, long InstanceId, AnimationState<Scp087Face> Animated) {
        GeoBone Head = getAnimationProcessor().getBone("head");
        if (Head == null) {
            return;
        }
        Head.setRotX(Head.getRotX() + gazePitch(Face, Animated.getPartialTick()) * Mth.DEG_TO_RAD);
    }

    public static float gazePitch(Scp087Face Face, float PartialTick) {
        Vec3 Eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 Centre = Face.getPosition(PartialTick).add(0.0, Scp087Face.Height / 2.0, 0.0);
        double Flat = Math.sqrt((Eye.x - Centre.x) * (Eye.x - Centre.x) + (Eye.z - Centre.z) * (Eye.z - Centre.z));
        float Pitch = (float) -(Mth.atan2(Eye.y - Centre.y, Math.max(Flat, 1.0e-3)) * Mth.RAD_TO_DEG);
        return Mth.clamp(Pitch, -PitchLimit, PitchLimit);
    }

    public static float gazeYaw(Scp087Face Face, float PartialTick) {
        Vec3 Eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 Centre = Face.getPosition(PartialTick);
        return (float) (Mth.atan2(Eye.z - Centre.z, Eye.x - Centre.x) * Mth.RAD_TO_DEG) - 90.0f;
    }
}
