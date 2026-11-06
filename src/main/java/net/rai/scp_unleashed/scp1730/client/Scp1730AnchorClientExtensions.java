package net.rai.scp_unleashed.scp1730.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.scp1730.item.RealityAnchorItem;

public class Scp1730AnchorClientExtensions implements IClientItemExtensions {
    public static final float LiftUp = 0.14f;
    public static final float PullBack = 0.08f;
    public static final float SwingOut = 0.12f;
    public static final float TiltBack = -10.0f;
    public static final float RollIn = -10.0f;
    public static final float Tremble = 0.006f;

    @Override
    public boolean applyForgeHandTransform(PoseStack Pose, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess, float SwingProcess) {
        if (!User.isUsingItem() || User.getUseItemRemainingTicks() <= 0 || !(Stack.getItem() instanceof RealityAnchorItem)) {
            return false;
        }
        InteractionHand Hand = Arm == User.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (User.getUsedItemHand() != Hand) {
            return false;
        }
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        float Held = Stack.getUseDuration(User) - (User.getUseItemRemainingTicks() - PartialTick + 1.0f);
        float Charge = Mth.clamp(Held / (float) RealityAnchorItem.FullChargeTicks, 0.0f, 1.0f);
        float Ease = Charge * Charge * (3.0f - 2.0f * Charge);
        Pose.translate(Side * 0.56f, -0.52f + EquipProcess * -0.6f, -0.72f);
        Pose.translate(Side * SwingOut * Ease, LiftUp * Ease, PullBack * Ease);
        Pose.mulPose(Axis.XP.rotationDegrees(TiltBack * Ease));
        Pose.mulPose(Axis.ZP.rotationDegrees(Side * RollIn * Ease));
        if (Charge >= 1.0f) {
            Pose.translate(0.0f, Mth.sin(Held * 1.9f) * Tremble, 0.0f);
        }
        return true;
    }
}
