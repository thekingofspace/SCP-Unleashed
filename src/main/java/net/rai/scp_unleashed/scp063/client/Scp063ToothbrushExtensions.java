package net.rai.scp_unleashed.scp063.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.scp063.Scp063Scrubbing;
import net.rai.scp_unleashed.scp063.Scp063ToothbrushItem;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class Scp063ToothbrushExtensions implements IClientItemExtensions {
    public static final Scp063ToothbrushExtensions Instance = new Scp063ToothbrushExtensions();
    public static final float RestX = 0.56f;
    public static final float RestY = -0.52f;
    public static final float RestZ = -0.72f;
    public static final float BrushX = 0.4827f;
    public static final float BrushY = -0.3438f;
    public static final float BrushZ = -0.524f;
    public static final float BrushPitch = -0.44f;
    public static final float BrushYaw = 29.31f;
    public static final float BrushRoll = 28.84f;
    public static final float RaiseTicks = 4.0f;
    public static final float StrokeTicks = 8.0f;
    public static final float StrokeReach = 0.05f;
    public static final float StrokeLift = 0.012f;
    public static final float StrokeRoll = 6.0f;
    public static final float ScrubX = 0.4775f;
    public static final float ScrubY = -0.3252f;
    public static final float ScrubZ = -0.3792f;
    public static final float ScrubPitch = 88.07f;
    public static final float ScrubYaw = 16.58f;
    public static final float ScrubRoll = 90.48f;
    public static final float ScrubTicks = 10.0f;
    public static final float ScrubReach = 0.09f;
    public static final float ScrubLift = 0.02f;
    public static final float ScrubRollSwing = 10.0f;
    public static final float EquipDrop = -0.6f;
    public static final int StrokeStep = 2;
    public static final int ScrubStep = 5;
    public static final float StrokeMiddle = 0.25f;
    public static final float StrokeIn = 0.5f;
    public static final float StrokeOut = 0.75f;
    public static final float ScrubLeft = 0.875f;
    public static final float ScrubRight = 1.0f;
    private static final Grip Teeth = new Grip(BrushX, BrushY, BrushZ, BrushPitch, BrushYaw, BrushRoll, StrokeTicks, StrokeReach, StrokeLift, StrokeRoll);
    private static final Grip Scrub = new Grip(ScrubX, ScrubY, ScrubZ, ScrubPitch, ScrubYaw, ScrubRoll, ScrubTicks, ScrubReach, ScrubLift, ScrubRollSwing);

    private Scp063ToothbrushExtensions() {
    }

    private static boolean usingWith(LivingEntity Holder, InteractionHand Hand) {
        return Holder.isUsingItem() && Holder.getUsedItemHand() == Hand && Scp063ToothbrushItem.is(Holder.getUseItem());
    }

    public static boolean brushingWith(LivingEntity Holder, InteractionHand Hand) {
        return usingWith(Holder, Hand) && !Scp063Scrubbing.isScrubbing(Holder);
    }

    public static boolean scrubbingWith(LivingEntity Holder, InteractionHand Hand) {
        return usingWith(Holder, Hand) && Scp063Scrubbing.isScrubbing(Holder);
    }

    public static float strokePhase(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Holder, int Seed) {
        if (Holder == null || !Holder.isUsingItem() || Holder.getItemInHand(Holder.getUsedItemHand()) != Stack || !Scp063ToothbrushItem.is(Holder.getUseItem())) {
            return 0.0f;
        }
        if (Scp063Scrubbing.isScrubbing(Holder)) {
            return (Holder.getTicksUsingItem() / ScrubStep) % 2 == 0 ? ScrubLeft : ScrubRight;
        }
        int Phase = (Holder.getTicksUsingItem() / StrokeStep) % 4;
        return Phase == 1 ? StrokeIn : Phase == 3 ? StrokeOut : StrokeMiddle;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity Holder, InteractionHand Hand, ItemStack Stack) {
        if (scrubbingWith(Holder, Hand)) {
            return HumanoidModel.ArmPose.BRUSH;
        }
        return brushingWith(Holder, Hand) ? HumanoidModel.ArmPose.SPYGLASS : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack Matrices, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess, float SwingProcess) {
        InteractionHand Hand = Arm == User.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!usingWith(User, Hand)) {
            return false;
        }
        Grip Held = Scp063Scrubbing.isScrubbing(User) ? Scrub : Teeth;
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        float Ticks = User.getTicksUsingItem() + PartialTick;
        float Raise = smooth(Ticks / RaiseTicks);
        float Stroke = Mth.sin(Ticks * Mth.TWO_PI / Held.Period()) * Raise;
        Matrices.translate(Side * (Mth.lerp(Raise, RestX, Held.X()) + Stroke * Held.Reach()),
                Mth.lerp(Raise, RestY, Held.Y()) + Math.abs(Stroke) * Held.Lift() + EquipProcess * EquipDrop,
                Mth.lerp(Raise, RestZ, Held.Z()));
        Quaternionf Target = new Quaternionf().rotationXYZ(Held.Pitch() * Mth.DEG_TO_RAD, Side * Held.Yaw() * Mth.DEG_TO_RAD, Side * Held.Roll() * Mth.DEG_TO_RAD);
        Matrices.mulPose(new Quaternionf().slerp(Target, Raise));
        Matrices.mulPose(Axis.ZP.rotationDegrees(Side * Stroke * Held.Swing()));
        return true;
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    private record Grip(float X, float Y, float Z, float Pitch, float Yaw, float Roll, float Period, float Reach, float Lift, float Swing) {
    }
}
