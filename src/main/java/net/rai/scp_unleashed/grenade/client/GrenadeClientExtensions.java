package net.rai.scp_unleashed.grenade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.grenade.GrenadeItem;
import net.rai.scp_unleashed.grenade.GrenadeTuning;

public class GrenadeClientExtensions implements IClientItemExtensions {
    public static final float LiftUp = 0.14F;
    public static final float PullBack = 0.08F;
    public static final float SwingOut = 0.12F;
    public static final float TiltBack = -10.0F;
    public static final float RollIn = -10.0F;
    public static final float Tremble = 0.006F;

    @Override
    public boolean applyForgeHandTransform(PoseStack Pose, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess,
                                           float SwingProcess) {
        if (!User.isUsingItem() || User.getUseItemRemainingTicks() <= 0 || !(Stack.getItem() instanceof GrenadeItem)) {
            return false;
        }
        InteractionHand Hand = Arm == User.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (User.getUsedItemHand() != Hand) {
            return false;
        }
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        float Held = Stack.getUseDuration(User) - (User.getUseItemRemainingTicks() - PartialTick + 1.0F);
        float Charge = Mth.clamp(Held / (float) GrenadeTuning.FullChargeTicks, 0.0F, 1.0F);
        float Ease = Charge * Charge * (3.0F - 2.0F * Charge);
        Pose.translate(Side * 0.56F, -0.52F + EquipProcess * -0.6F, -0.72F);
        Pose.translate(Side * SwingOut * Ease, LiftUp * Ease, PullBack * Ease);
        Pose.mulPose(Axis.XP.rotationDegrees(TiltBack * Ease));
        Pose.mulPose(Axis.ZP.rotationDegrees(Side * RollIn * Ease));
        if (Charge >= 1.0F) {
            Pose.translate(0.0F, Mth.sin(Held * 1.9F) * Tremble, 0.0F);
        }
        return true;
    }
}
