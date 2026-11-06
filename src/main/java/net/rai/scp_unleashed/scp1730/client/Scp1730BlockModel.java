package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730ThresherBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp1730BlockModel<T extends BlockEntity & GeoAnimatable> extends GeoModel<T> {
    public static final float WreckTilt = 14.0f;
    public static final float PulseDepth = 0.24f;
    public static final float PulseSeconds = 0.8f;
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;

    public Scp1730BlockModel(String Name) {
        this.ModelPath = Scp1730Registry.location("geo/block/scp1730/" + Name + ".geo.json");
        this.TexturePath = Scp1730Registry.location("textures/block/scp1730/" + Name + ".png");
        this.AnimationPath = Scp1730Registry.location("animations/block/scp1730/" + Name + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(T Owner, long InstanceId, AnimationState<T> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        if (Owner instanceof Scp1730TurretBlockEntity Turret) {
            float Partial = Animated.getPartialTick();
            GeoBone Yaw = getAnimationProcessor().getBone("yaw");
            GeoBone Pitch = getAnimationProcessor().getBone("pitch");
            if (Yaw != null) {
                Yaw.setRotY(Yaw.getInitialSnapshot().getRotY() + Mth.rotLerp(Partial, Turret.AimYawOld, Turret.AimYaw) * Mth.DEG_TO_RAD);
            }
            if (Pitch != null) {
                Pitch.setRotX(Pitch.getInitialSnapshot().getRotX() + Mth.lerp(Partial, Turret.AimPitchOld, Turret.AimPitch) * Mth.DEG_TO_RAD);
                Pitch.setRotZ(Pitch.getInitialSnapshot().getRotZ() + (Turret.wrecked() ? WreckTilt * Mth.DEG_TO_RAD : 0.0f));
            }
        } else if (Owner instanceof Scp1730ThresherBlockEntity Thresher) {
            GeoBone Core = getAnimationProcessor().getBone("core");
            if (Core != null) {
                Core.setHidden(Thresher.pulled());
            }
        } else if (Owner instanceof RealityFolderBlockEntity Folder) {
            folder(Folder, Animated.getPartialTick());
        }
    }

    private void folder(RealityFolderBlockEntity Folder, float Partial) {
        float Coil = Folder.coilAngle(Partial);
        float Charge = Folder.shownCharge(Partial);
        spin("coil_ne", -Coil);
        spin("coil_sw", -Coil);
        spin("coil_nw", Coil);
        spin("coil_se", Coil);
        spin("projector_ring", -Folder.ringAngle(Partial));
        light("lamp_1", ramp(Charge, 0.12f, 0.18f));
        light("lamp_2", ramp(Charge, 0.22f, 0.30f));
        light("lamp_3", ramp(Charge, 0.33f, 0.40f));
        light("lamp_4", ramp(Charge, 0.43f, 0.50f));
        float Glow = ramp(Charge, 0.57f, 0.65f);
        light("glow_ne", Glow);
        light("glow_nw", Glow);
        light("glow_se", Glow);
        light("glow_sw", Glow);
        light("core_glow", ramp(Charge, 0.72f, 0.85f));
        float Pulse = 1.0f;
        if (Folder.status() == RealityFolderBlockEntity.StatusRunning && Folder.getLevel() != null) {
            float Seconds = (Folder.getLevel().getGameTime() + Partial) / 20.0f;
            Pulse += PulseDepth * Math.max(0.0f, Mth.sin(Seconds * Mth.TWO_PI / PulseSeconds));
        }
        light("spire_glow", ramp(Charge, 0.87f, 0.95f) * Pulse);
    }

    private static float ramp(float Value, float Low, float High) {
        return Mth.clamp((Value - Low) / (High - Low), 0.0f, 1.0f);
    }

    private void spin(String Name, float Degrees) {
        GeoBone Bone = getAnimationProcessor().getBone(Name);
        if (Bone != null) {
            Bone.setRotY(Bone.getInitialSnapshot().getRotY() - Degrees * Mth.DEG_TO_RAD);
        }
    }

    private void light(String Name, float Scale) {
        GeoBone Bone = getAnimationProcessor().getBone(Name);
        if (Bone != null) {
            Bone.setScaleX(Scale);
            Bone.setScaleY(Scale);
            Bone.setScaleZ(Scale);
        }
    }
}
