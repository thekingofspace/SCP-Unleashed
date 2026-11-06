package net.rai.scp_unleashed.scp207.client;

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
import net.rai.scp_unleashed.scp207.Scp207BottleItem;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class Scp207BottleExtensions implements IClientItemExtensions {
    public static final Scp207BottleExtensions Instance = new Scp207BottleExtensions();
    public static final float RestX = 0.56f;
    public static final float RestY = -0.52f;
    public static final float RestZ = -0.72f;
    public static final float DrinkX = 0.3425f;
    public static final float DrinkY = 0.1265f;
    public static final float DrinkZ = -0.6882f;
    public static final float DrinkPitch = 106.81f;
    public static final float DrinkYaw = 27.06f;
    public static final float DrinkRoll = 19.63f;
    public static final float RaiseTicks = 5.0f;
    public static final float LowerTicks = 4.0f;
    public static final float GulpTicks = 6.0f;
    public static final float GulpTilt = 4.0f;
    public static final float GulpLift = 0.01f;
    public static final float DipY = 0.22f;
    public static final float PushZ = 0.12f;
    public static final float EquipDrop = -0.6f;

    private Scp207BottleExtensions() {
    }

    public static boolean drinkingWith(LivingEntity Holder, InteractionHand Hand) {
        return Holder.isUsingItem() && Holder.getUsedItemHand() == Hand && Scp207BottleItem.is(Holder.getUseItem());
    }

    public static float drinking(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Holder, int Seed) {
        if (Holder == null || !Holder.isUsingItem() || Holder.getItemInHand(Holder.getUsedItemHand()) != Stack || !Scp207BottleItem.is(Holder.getUseItem())) {
            return 0.0f;
        }
        return 1.0f;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity Holder, InteractionHand Hand, ItemStack Stack) {
        return drinkingWith(Holder, Hand) ? HumanoidModel.ArmPose.SPYGLASS : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack Matrices, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess, float SwingProcess) {
        InteractionHand Hand = Arm == User.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!drinkingWith(User, Hand)) {
            return false;
        }
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        float Ticks = User.getTicksUsingItem() + PartialTick;
        float Raise = smooth(Ticks / RaiseTicks) * smooth((Scp207BottleItem.DrinkTicks - Ticks) / LowerTicks);
        float Gulp = (0.5f - 0.5f * Mth.cos(Ticks * Mth.TWO_PI / GulpTicks)) * Raise;
        float Arc = 4.0f * Raise * (1.0f - Raise);
        Matrices.translate(Side * Mth.lerp(Raise, RestX, DrinkX),
                Mth.lerp(Raise, RestY, DrinkY) + Gulp * GulpLift - Arc * DipY + EquipProcess * EquipDrop,
                Mth.lerp(Raise, RestZ, DrinkZ) - Arc * PushZ);
        Quaternionf Target = new Quaternionf().rotationXYZ(DrinkPitch * Mth.DEG_TO_RAD, Side * DrinkYaw * Mth.DEG_TO_RAD, Side * DrinkRoll * Mth.DEG_TO_RAD);
        Matrices.mulPose(new Quaternionf().slerp(Target, Raise));
        Matrices.mulPose(Axis.XP.rotationDegrees(Gulp * GulpTilt));
        return true;
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }
}
