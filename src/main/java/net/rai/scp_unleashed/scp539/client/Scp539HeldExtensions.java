package net.rai.scp_unleashed.scp539.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class Scp539HeldExtensions implements IClientItemExtensions {
    public static final Scp539HeldExtensions Instance = new Scp539HeldExtensions();
    public static final float RestX = 0.56f;
    public static final float RestY = -0.52f;
    public static final float RestZ = -0.72f;
    public static final float EquipDrop = -0.6f;
    public static final float SwingReach = -0.4f;
    public static final float SwingLift = 0.2f;
    public static final float SwingPush = -0.2f;
    public static final float SwingTurn = 45.0f;
    public static final float SwingEase = -20.0f;
    public static final float SwingRoll = -20.0f;
    public static final float SwingPitch = -80.0f;

    private Scp539HeldExtensions() {
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack Matrices, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess, float SwingProcess) {
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        float Root = Mth.sqrt(SwingProcess);
        Matrices.translate(Side * SwingReach * Mth.sin(Root * Mth.PI), SwingLift * Mth.sin(Root * Mth.TWO_PI), SwingPush * Mth.sin(SwingProcess * Mth.PI));
        Matrices.translate(Side * RestX, RestY + EquipProcess * EquipDrop, RestZ);
        float Ease = Mth.sin(SwingProcess * SwingProcess * Mth.PI);
        float Arc = Mth.sin(Root * Mth.PI);
        Matrices.mulPose(Axis.YP.rotationDegrees(Side * (SwingTurn + Ease * SwingEase)));
        Matrices.mulPose(Axis.ZP.rotationDegrees(Side * Arc * SwingRoll));
        Matrices.mulPose(Axis.XP.rotationDegrees(Arc * SwingPitch));
        Matrices.mulPose(Axis.YP.rotationDegrees(Side * -SwingTurn));
        return true;
    }
}
