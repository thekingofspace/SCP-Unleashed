package net.rai.scp_unleashed.scp280.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp280.Scp280Registry;
import net.rai.scp_unleashed.scp280.Scp280Shade;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp280ShadeGeoModel extends GeoModel<Scp280Shade> {
    public static final ResourceLocation ModelPath = Scp280Registry.location("geo/entity/scp280/shade.geo.json");
    public static final ResourceLocation TexturePath = Scp280Registry.location("textures/entity/scp280/shade.png");
    public static final ResourceLocation EyesPath = Scp280Registry.location("textures/entity/scp280/shade_eyes.png");
    public static final ResourceLocation AnimationPath = Scp280Registry.location("animations/entity/scp280/shade.animation.json");
    public static final String LookBone = "look";
    public static final int MaxEyes = 64;
    public static final float PitchLimit = 30.0f;
    public static final float YawLimit = 50.0f;
    public static final int BlinkTicks = 5;
    public static final int StaringShare = 7;

    @Override
    public ResourceLocation getModelResource(Scp280Shade Shade) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp280Shade Shade) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp280Shade Shade) {
        return AnimationPath;
    }

    public static int hash(int Value) {
        int Mixed = Value * 0x45D9F3B;
        Mixed = (Mixed >>> 16 ^ Mixed) * 0x45D9F3B;
        return (Mixed >>> 16 ^ Mixed) & 0x7FFFFFFF;
    }

    @Override
    public void setCustomAnimations(Scp280Shade Shade, long InstanceId, AnimationState<Scp280Shade> Animated) {
        float Partial = Animated.getPartialTick();
        float Time = Shade.tickCount + Partial;
        if (!Shade.locksMovement()) {
            GeoBone Look = getAnimationProcessor().getBone(LookBone);
            EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
            if (Look != null && Data != null) {
                Look.setRotX(Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
                Look.setRotY(Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
            }
        }
        float Base = Scp280ShadeEyes.baseOpenness(Shade, Partial);
        boolean Staring = Shade.isChasing();
        for (int Index = 0; Index < MaxEyes; Index++) {
            GeoBone Lid = getAnimationProcessor().getBone("lid_" + Index);
            if (Lid == null) {
                break;
            }
            float Open = Base * Scp280ShadeEyes.eyeFactor(Shade, Index, Time) * blink(Index, Time);
            if (Open >= 0.97f) {
                Lid.setHidden(true);
            } else {
                Lid.setHidden(false);
                Lid.setScaleY(Math.max(0.02f, 1.0f - Open));
            }
            GeoBone Pupil = getAnimationProcessor().getBone("pupil_" + Index);
            if (Pupil != null) {
                Pupil.setPosX(look(Index, Time, Staring));
            }
        }
    }

    public static float blink(int Index, float Time) {
        int Period = 55 + hash(Index * 3 + 1) % 110;
        float Phase = (Time + hash(Index * 5 + 2) % Period) % Period;
        if (Phase >= BlinkTicks) {
            return 1.0f;
        }
        return Math.abs(Phase / BlinkTicks * 2.0f - 1.0f);
    }

    public static float look(int Index, float Time, boolean Staring) {
        if (Staring && hash(Index * 13 + 7) % 10 < StaringShare) {
            return 0.0f;
        }
        int Period = 30 + hash(Index * 11 + 3) % 70;
        int Step = (int) ((Time + hash(Index * 17 + 5) % Period) / Period);
        int Roll = hash(Index * 31 + Step * 101) % 5;
        return Roll == 0 ? -1.0f : Roll == 1 ? 1.0f : 0.0f;
    }
}
