package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

public final class GunStance {
    public static final float LowPitch = -0.87F;
    public static final float LowYaw = 0.36F;
    public static final float LowRoll = 0.0F;
    public static final float SupportPitch = -0.80F;
    public static final float SupportYaw = 0.80F;
    public static final float SupportRoll = 0.0F;
    public static final float OnePitch = -0.82F;
    public static final float OneYaw = 0.22F;
    public static final float OneRoll = 0.0F;
    public static final float CentreShare = 1.0F;
    public static final float HandOffset = 1.0F / 16.0F;
    public static final float StepBob = 0.05F;
    public static final float StepSway = 0.035F;
    public static final float BreathBob = 0.02F;
    public static final float WalkRate = 0.6662F;
    public static final float BreathRate = 0.067F;
    public static final float AimLift = 0.1F;
    public static final float AimMainYaw = 0.3F;
    public static final float AimOffYaw = 0.6F;
    public static final float AimOffPitch = -1.5F;
    public static final float BlendTicks = 5.0F;
    public static final float HeavyPitch = -0.6F;
    public static final float HeavyYaw = 0.1F;
    public static final float HeavyRoll = 0.0F;
    public static final float HeavySupportPitch = -1.0F;
    public static final float HeavySupportYaw = 0.62F;
    public static final float HeavySupportRoll = 0.0F;
    public static final float HeavyBob = 0.03F;
    public static final float HeavyTilt = 46.0F;
    public static final float HeavyGripY = -0.125F;
    public static final float HeavyGripZ = 0.09375F;
    public static final double Forget = 200.0;
    public static final int Prune = 64;

    private static final Map<Integer, Track> Tracks = new HashMap<>();

    private static final class Track {
        private boolean Aiming;
        private float From;
        private double Since;
        private double Seen;
    }

    private GunStance() {
    }

    public static float[] low(boolean RightMain, boolean Support, boolean GunArm, float WalkPos, float WalkSpeed, float Age, boolean Breath) {
        float Side = RightMain ? 1.0F : -1.0F;
        float Phase = WalkPos * WalkRate;
        float Step = Mth.clamp(WalkSpeed, 0.0F, 1.0F);
        float Bob = StepBob * Step * Mth.cos(Phase * 2.0F) + (Breath ? BreathBob * Mth.sin(Age * BreathRate) : 0.0F);
        float Sway = StepSway * Step * Mth.sin(Phase);
        if (!GunArm) {
            return new float[]{SupportPitch + Bob, Side * SupportYaw + Sway, -Side * SupportRoll};
        }
        if (Support) {
            return new float[]{LowPitch + Bob, -Side * LowYaw + Sway, Side * LowRoll};
        }
        return new float[]{OnePitch + Bob, -Side * OneYaw + Sway, Side * OneRoll};
    }

    public static float[] heavy(boolean RightMain, boolean GunArm, float WalkPos, float WalkSpeed, float Age) {
        float Side = RightMain ? 1.0F : -1.0F;
        float Phase = WalkPos * WalkRate;
        float Step = Mth.clamp(WalkSpeed, 0.0F, 1.0F);
        float Bob = HeavyBob * Step * Mth.cos(Phase * 2.0F) + BreathBob * Mth.sin(Age * BreathRate);
        float Sway = StepSway * Step * Mth.sin(Phase);
        if (!GunArm) {
            return new float[]{HeavySupportPitch + Bob, Side * HeavySupportYaw + Sway, -Side * HeavySupportRoll};
        }
        return new float[]{HeavyPitch + Bob, -Side * HeavyYaw + Sway, Side * HeavyRoll};
    }

    public static void tilt(PoseStack Pose, float Share) {
        if (Share <= 0.0F) {
            return;
        }
        Pose.translate(0.0F, HeavyGripY, HeavyGripZ);
        Pose.mulPose(Axis.XP.rotationDegrees(HeavyTilt * Mth.clamp(Share, 0.0F, 1.0F)));
        Pose.translate(0.0F, -HeavyGripY, -HeavyGripZ);
    }

    public static float[] aimAngles(boolean RightMain, boolean GunArm, float HeadX, float HeadY) {
        float Side = RightMain ? 1.0F : -1.0F;
        if (GunArm) {
            return new float[]{-Mth.HALF_PI + HeadX + AimLift, -Side * AimMainYaw + HeadY, 0.0F};
        }
        return new float[]{AimOffPitch + HeadX, Side * AimOffYaw + HeadY, 0.0F};
    }

    public static float[] mix(float[] From, float[] To, float Weight) {
        return new float[]{Mth.lerp(Weight, From[0], To[0]), Mth.lerp(Weight, From[1], To[1]), Mth.lerp(Weight, From[2], To[2])};
    }

    public static void straighten(PoseStack Pose, LivingEntity Holder, ItemStack Stack, boolean Left, float Share) {
        if (Share <= 0.0F) {
            return;
        }
        ItemDisplayContext Context = Left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        BakedModel Model = Minecraft.getInstance().getItemRenderer().getModel(Stack, Holder.level(), Holder, Holder.getId());
        ItemTransform Display = Model.getTransforms().getTransform(Context);
        if (Display == ItemTransform.NO_TRANSFORM) {
            return;
        }
        float Side = Left ? -1.0F : 1.0F;
        float OffsetX = Side * Display.translation.x();
        float OffsetY = Display.translation.y();
        float OffsetZ = Display.translation.z();
        float Centre = -Side * (Display.translation.x() + HandOffset) * Share * CentreShare;
        Quaternionf Undo = new Quaternionf().rotationXYZ(Display.rotation.x() * Mth.DEG_TO_RAD, Side * Display.rotation.y() * Mth.DEG_TO_RAD, Side * Display.rotation.z() * Mth.DEG_TO_RAD)
                .conjugate();
        Pose.translate(Centre + OffsetX, OffsetY, OffsetZ);
        Pose.mulPose(new Quaternionf().slerp(Undo, Mth.clamp(Share, 0.0F, 1.0F)));
        Pose.translate(-OffsetX, -OffsetY, -OffsetZ);
    }

    public static float partial() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    public static float aim(LivingEntity Holder, boolean Aiming, boolean Snap, float Partial) {
        double Now = clock(Holder, Partial);
        Track Found = Tracks.get(Holder.getId());
        if (Found == null) {
            if (Tracks.size() > Prune) {
                Tracks.values().removeIf(Old -> Now - Old.Seen > Forget || Old.Seen > Now);
            }
            Found = new Track();
            Found.Aiming = Aiming;
            Found.From = Aiming ? 1.0F : 0.0F;
            Found.Since = Now - BlendTicks;
            Tracks.put(Holder.getId(), Found);
        }
        if (Found.Seen > Now) {
            Found.Since = Now - BlendTicks;
        }
        Found.Seen = Now;
        if (Found.Aiming != Aiming) {
            Found.From = value(Found, Now);
            Found.Aiming = Aiming;
            Found.Since = Now;
        }
        if (Snap && Aiming) {
            Found.From = 1.0F;
            Found.Since = Now - BlendTicks;
        }
        return value(Found, Now);
    }

    public static float peek(LivingEntity Holder, float Partial) {
        Track Found = Tracks.get(Holder.getId());
        return Found == null ? 0.0F : value(Found, clock(Holder, Partial));
    }

    private static float value(Track Found, double Now) {
        float Progress = (float) Mth.clamp((Now - Found.Since) / BlendTicks, 0.0, 1.0);
        float Eased = Progress * Progress * (3.0F - 2.0F * Progress);
        return Found.Aiming ? Found.From + (1.0F - Found.From) * Eased : Found.From * (1.0F - Eased);
    }

    private static double clock(LivingEntity Holder, float Partial) {
        return Holder.level().getGameTime() + Partial;
    }
}
