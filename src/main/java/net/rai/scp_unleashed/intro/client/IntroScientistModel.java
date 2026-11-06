package net.rai.scp_unleashed.intro.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.intro.IntroRegistry;
import net.rai.scp_unleashed.intro.IntroScientist;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class IntroScientistModel extends DClassGeoModel<IntroScientist> {
    public static final ResourceLocation Texture = IntroRegistry.location("textures/entity/intro/scientist.png");
    public static final ResourceLocation Geometry = IntroRegistry.location("geo/entity/intro/scientist.geo.json");
    public static final float GestureEase = 9.0f;
    public static final float LeanAngle = 0.22f;
    public static final float SteepleLift = 0.95f;
    public static final float SteepleTurn = 0.52f;
    public static final float TapLift = 0.62f;
    public static final float TapBeat = 0.9f;
    public static final float TiredLids = 0.32f;

    @Override
    public ResourceLocation getModelResource(IntroScientist Owner) {
        return Geometry;
    }

    @Override
    public ResourceLocation getTextureResource(IntroScientist Owner) {
        return Texture;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    private void add(String Name, float X, float Y, float Z) {
        GeoBone Found = part(Name);
        if (Found != null) {
            Found.setRotX(Found.getRotX() + X);
            Found.setRotY(Found.getRotY() + Y);
            Found.setRotZ(Found.getRotZ() + Z);
        }
    }

    private void tired(String Name) {
        GeoBone Found = part(Name);
        if (Found != null && Found.getScaleY() < TiredLids) {
            Found.setHidden(false);
            Found.setScaleY(TiredLids);
        }
    }

    private void blend(String Name, float X, float Y, float Z, float Share) {
        GeoBone Found = part(Name);
        if (Found != null && Share > 0.0f) {
            Found.setRotX(Mth.lerp(Share, Found.getRotX(), X));
            Found.setRotY(Mth.lerp(Share, Found.getRotY(), Y));
            Found.setRotZ(Mth.lerp(Share, Found.getRotZ(), Z));
        }
    }

    @Override
    public void setCustomAnimations(IntroScientist Owner, long InstanceId, AnimationState<IntroScientist> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        tired("right_lid");
        tired("left_lid");
        if (!IntroClient.isScientist(Owner)) {
            return;
        }
        GeoBone Mouth = part("mouth");
        if (Mouth != null) {
            Mouth.setHidden(!IntroClient.mouthOpen());
        }
        float Partial = Animated.getPartialTick();
        int Page = IntroClient.page();
        float Age = IntroClient.pageAge(Partial);
        float In = Mth.clamp(Age / GestureEase, 0.0f, 1.0f);
        float Shaped = In * In * (3.0f - 2.0f * In);
        float Clock = Owner.tickCount + Partial;
        boolean Seated = Owner.isSittingPose();
        if (IntroClient.speaking()) {
            add("head", Mth.sin(Clock * 0.42f) * 0.035f, Mth.sin(Clock * 0.17f) * 0.03f, 0.0f);
        }
        if (Page < 0) {
            return;
        }
        if (IntroClient.contains(IntroClient.LeanPages, Page) && Seated) {
            add("body", -LeanAngle * Shaped, 0.0f, 0.0f);
            add("head", LeanAngle * 0.6f * Shaped, 0.0f, 0.0f);
            blend("right_arm", 0.9f, 0.18f, 0.0f, Shaped);
            blend("left_arm", 0.9f, -0.18f, 0.0f, Shaped);
        } else if (IntroClient.contains(IntroClient.SteeplePages, Page)) {
            float Breathe = Mth.sin(Clock * 0.12f) * 0.03f;
            blend("right_arm", SteepleLift + Breathe, SteepleTurn, 0.0f, Shaped);
            blend("left_arm", SteepleLift + Breathe, -SteepleTurn, 0.0f, Shaped);
        } else if (IntroClient.contains(IntroClient.TapPages, Page) && Seated) {
            float Tap = Age < 40.0f ? Math.max(0.0f, Mth.sin(Age * TapBeat)) * 0.12f : 0.0f;
            blend("right_arm", TapLift - Tap, 0.12f, 0.0f, Shaped);
            add("head", 0.12f * Shaped, 0.0f, 0.0f);
        } else if (IntroClient.speaking()) {
            float Wave = Mth.sin(Clock * 0.21f) * 0.12f;
            add("right_arm", (0.3f + Wave) * Shaped, 0.08f * Shaped, 0.0f);
        }
    }
}
