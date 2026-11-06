package net.rai.scp_unleashed.z0002.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class Z0002FingerExtensions implements IClientItemExtensions {
    public static final Z0002FingerExtensions Instance = new Z0002FingerExtensions();
    public static final float Lift = 0.28f;
    public static final float Tilt = 38.0f;

    private Z0002FingerExtensions() {
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity Holder, InteractionHand Hand, ItemStack Stack) {
        return Z0002ClientChannel.channelling(Holder) ? HumanoidModel.ArmPose.THROW_SPEAR : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack Matrices, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess, float SwingProcess) {
        float Raise = Z0002ClientChannel.raise(User, PartialTick);
        if (Raise <= 0.0f) {
            return false;
        }
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        Matrices.translate(Side * 0.56f, -0.52f + EquipProcess * -0.6f + Lift * Raise, -0.72f - 0.12f * Raise);
        Matrices.mulPose(Axis.XP.rotationDegrees(-Tilt * Raise));
        Matrices.mulPose(Axis.ZP.rotationDegrees(Side * 6.0f * Raise));
        return true;
    }
}
