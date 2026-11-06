package net.rai.scp_unleashed.scp149.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Wings;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.List;

public class Scp149FlyGeoModel extends GeoModel<Scp149Fly> {
    public static final String Name = "scp149_fly";
    public static final String BellyBone = "belly";
    public static final String AbdomenBone = "abdomen";
    public static final List<String> ForeLegs = List.of("fl", "fr", "ml", "mr");
    public static final List<String> HindLegs = List.of("hl", "hr");
    public static final float GorgedSwell = 1.22f;
    public static final float GorgedLength = 1.08f;
    public static final float TibiaShare = 0.7f;
    private static final ResourceLocation ModelPath = location("geo/entity/scp149/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = location("textures/entity/scp149/" + Name + ".png");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp149/" + Name + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp149Fly Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp149Fly Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp149Fly Creature) {
        return AnimationPath;
    }

    private static void nudge(GeoBone Part, float X, float Y, float Z) {
        Part.setRotX(Part.getRotX() - X * Mth.DEG_TO_RAD);
        Part.setRotY(Part.getRotY() - Y * Mth.DEG_TO_RAD);
        Part.setRotZ(Part.getRotZ() + Z * Mth.DEG_TO_RAD);
    }

    @Override
    public void setCustomAnimations(Scp149Fly Creature, long InstanceId, AnimationState<Scp149Fly> Animated) {
        float Partial = Animated.getPartialTick();
        float Time = Creature.tickCount + Partial;
        boolean Gorged = Creature.isGorged();
        GeoBone Belly = getAnimationProcessor().getBone(BellyBone);
        if (Belly != null) {
            float Pump = Scp149Wings.pump(Time);
            float Swell = Gorged ? GorgedSwell : 1.0f;
            Belly.setScaleX(Swell * Pump);
            Belly.setScaleY(Swell * Pump);
            Belly.setScaleZ(Gorged ? GorgedLength : 1.0f);
        }
        Scp149Wings Wings = Creature.wings();
        if (Wings == null) {
            return;
        }
        GeoBone Abdomen = getAnimationProcessor().getBone(AbdomenBone);
        if (Abdomen != null) {
            float Sway = Gorged ? Scp149Wings.pendulum(Time) : 0.0f;
            nudge(Abdomen, Wings.abdomenPitch(Partial, Gorged), Wings.abdomenYaw(Partial, Gorged) + Sway, Wings.abdomenRoll(Partial, Gorged));
        }
        float Trail = Wings.legTrail(Partial);
        float Hind = Wings.hindTrail(Partial);
        for (String Leg : ForeLegs) {
            trail(Leg, Trail);
        }
        for (String Leg : HindLegs) {
            trail(Leg, Hind);
        }
    }

    private void trail(String Leg, float Degrees) {
        GeoBone Femur = getAnimationProcessor().getBone("femur_" + Leg);
        GeoBone Tibia = getAnimationProcessor().getBone("tibia_" + Leg);
        if (Femur != null) {
            nudge(Femur, -Degrees, 0.0f, 0.0f);
        }
        if (Tibia != null) {
            nudge(Tibia, -Degrees * TibiaShare, 0.0f, 0.0f);
        }
    }
}
