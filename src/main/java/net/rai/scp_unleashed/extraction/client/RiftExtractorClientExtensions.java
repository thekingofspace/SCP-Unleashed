package net.rai.scp_unleashed.extraction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;

public class RiftExtractorClientExtensions implements IClientItemExtensions {
    public static final float ShiftX = -0.08F;
    public static final float ShiftY = 0.05F;
    public static final float ShiftZ = 0.0F;
    public static final float RaiseYaw = 5.0F;
    public static final float RaisePitch = 3.0F;
    public static final float AimYaw = 0.3F;
    public static final float AimPitch = 0.1F;

    @Nullable
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity Holder, InteractionHand Hand, ItemStack Stack) {
        if (!Holder.isUsingItem() || Holder.getUsedItemHand() != Hand || unseenShadowCaster(Holder)) {
            return null;
        }
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

    public static boolean unseenShadowCaster(@Nullable LivingEntity Holder) {
        if (Holder == null || !ShaderCompat.renderingShadowPass()) {
            return false;
        }
        Minecraft Client = Minecraft.getInstance();
        return Holder == Client.getCameraEntity() && Client.options.getCameraType().isFirstPerson();
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack Pose, LocalPlayer Holder, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess, float SwingProcess) {
        InteractionHand Hand = Arm == Holder.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        boolean InUse = Holder.isUsingItem() && Holder.getUsedItemHand() == Hand;
        float Raise = ExtractionTracker.raise(PartialTick);
        if (Raise > 0.0F && ExtractionTracker.raisedArm() == Arm) {
            applyHeldPose(Pose, Arm, InUse ? 0.0F : EquipProcess, Raise, ExtractionTracker.tremor(PartialTick, 0.0F), ExtractionTracker.tremor(PartialTick, 1.7F));
            return true;
        }
        if (InUse) {
            applyHeldPose(Pose, Arm, 0.0F, 0.0F, 0.0F, 0.0F);
            return true;
        }
        return false;
    }

    public static void applyHeldPose(PoseStack Pose, HumanoidArm Arm, float EquipProcess, float Raise, float TremorX, float TremorY) {
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        Pose.translate(Side * 0.56F, -0.52F + EquipProcess * -0.6F, -0.72F);
        Pose.translate(Side * ShiftX * Raise + TremorX, ShiftY * Raise + TremorY, ShiftZ * Raise);
        Pose.mulPose(Axis.YP.rotationDegrees(Side * RaiseYaw * Raise));
        Pose.mulPose(Axis.XP.rotationDegrees(RaisePitch * Raise));
    }
}
