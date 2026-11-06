package net.rai.scp_unleashed.guns.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import org.jetbrains.annotations.Nullable;

public final class PlayerGunStance {
    public static final EnumProxy<HumanoidModel.ArmPose> TwoHandedProxy = new EnumProxy<>(HumanoidModel.ArmPose.class, true,
            (IArmPoseTransformer) PlayerGunStance::twoHands);
    public static final EnumProxy<HumanoidModel.ArmPose> OneHandedProxy = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) PlayerGunStance::oneHand);

    private PlayerGunStance() {
    }

    @Nullable
    public static HumanoidModel.ArmPose armPose(LivingEntity Holder, ItemStack Stack, GunKind Kind) {
        boolean Fired = GunClientState.firedRecently(Holder.getId());
        boolean Charging = GunItem.getCharging(Stack) != null;
        boolean Aiming = Holder.isUsingItem() && Holder.getUsedItemHand() == InteractionHand.MAIN_HAND || Fired || Charging;
        boolean Heavy = Kind.getStats().isHeavy();
        if (Kind.getStats().isLongGun() && !Heavy) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        float Weight = GunStance.aim(Holder, Aiming, Fired || Charging, GunStance.partial());
        if (Weight >= 1.0F) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        if (Holder.isSleeping() || Holder.isVisuallySwimming() || Holder.isFallFlying()) {
            return Aiming ? HumanoidModel.ArmPose.CROSSBOW_HOLD : null;
        }
        return (Heavy || Holder.getOffhandItem().isEmpty() ? TwoHandedProxy : OneHandedProxy).getValue();
    }

    private static void twoHands(HumanoidModel<?> Model, LivingEntity Holder, HumanoidArm Arm) {
        pose(Model, Holder, Arm, true);
    }

    private static void oneHand(HumanoidModel<?> Model, LivingEntity Holder, HumanoidArm Arm) {
        pose(Model, Holder, Arm, false);
    }

    private static void pose(HumanoidModel<?> Model, LivingEntity Holder, HumanoidArm Arm, boolean Support) {
        float Partial = GunStance.partial();
        boolean Right = Arm == HumanoidArm.RIGHT;
        ModelPart Gun = Right ? Model.rightArm : Model.leftArm;
        ModelPart Other = Right ? Model.leftArm : Model.rightArm;
        float Weight = GunStance.peek(Holder, Partial);
        float WalkPos = Holder.walkAnimation.position(Partial);
        float WalkSpeed = Holder.walkAnimation.speed(Partial);
        float Age = Holder.tickCount + Partial;
        float HeadX = Model.head.xRot;
        float HeadY = Model.head.yRot;
        GunKind Held = GunItem.kindOf(Holder.getMainHandItem());
        boolean Heavy = Held != null && Held.getStats().isHeavy();
        float[] Low = Heavy ? GunStance.heavy(Right, true, WalkPos, WalkSpeed, Age) : GunStance.low(Right, Support, true, WalkPos, WalkSpeed, Age, false);
        put(Gun, GunStance.mix(Low, GunStance.aimAngles(Right, true, HeadX, HeadY), Weight));
        if (!Support && Weight <= 0.0F) {
            return;
        }
        float[] From = Heavy ? GunStance.heavy(Right, false, WalkPos, WalkSpeed, Age)
                : Support ? GunStance.low(Right, true, false, WalkPos, WalkSpeed, Age, false) : new float[]{Other.xRot, Other.yRot, Other.zRot};
        put(Other, GunStance.mix(From, GunStance.aimAngles(Right, false, HeadX, HeadY), Weight));
    }

    private static void put(ModelPart Part, float[] Angles) {
        Part.xRot = Angles[0];
        Part.yRot = Angles[1];
        Part.zRot = Angles[2];
    }
}
