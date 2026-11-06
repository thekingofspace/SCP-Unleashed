package net.rai.scp_unleashed.scp005.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.scp005.Scp005KeyItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Scp005KeyExtensions implements IClientItemExtensions {
    public static final Scp005KeyExtensions Instance = new Scp005KeyExtensions();
    private static final Vector3f Grip = new Vector3f(-0.5f, 0.0f, 0.0f);
    private static final float PushDepth = 0.11f;
    private static final float TurnAngle = 80.0f;
    private static final float TwistAngle = 28.0f;

    private Scp005KeyExtensions() {
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack Matrices, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, float PartialTick, float EquipProcess, float SwingProcess) {
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        HumanoidArm Turning = Scp005KeyItem.turnHand() == InteractionHand.MAIN_HAND ? User.getMainArm() : User.getMainArm().getOpposite();
        float Progress = Scp005KeyItem.turnProgress(User.level().getGameTime(), PartialTick);
        boolean Animating = Arm == Turning && Progress >= 0.0f && Progress < 1.0f;
        boolean Swinging = !Animating && SwingProcess > 0.0f;
        if (Swinging) {
            float Reach = Mth.sin(Mth.sqrt(SwingProcess) * Mth.PI);
            Matrices.translate(Side * -0.4f * Reach, 0.2f * Mth.sin(Mth.sqrt(SwingProcess) * Mth.PI * 2.0f), -0.2f * Mth.sin(SwingProcess * Mth.PI));
        }
        Matrices.translate(Side * 0.56f, -0.52f + EquipProcess * -0.6f, -0.72f);
        if (Animating) {
            turn(Matrices, User, Arm, Stack, Side, Progress);
        } else if (Swinging) {
            float Snap = Mth.sin(SwingProcess * SwingProcess * Mth.PI);
            float Reach = Mth.sin(Mth.sqrt(SwingProcess) * Mth.PI);
            Matrices.mulPose(Axis.YP.rotationDegrees(Side * (45.0f + Snap * -20.0f)));
            Matrices.mulPose(Axis.ZP.rotationDegrees(Side * Reach * -20.0f));
            Matrices.mulPose(Axis.XP.rotationDegrees(Reach * -80.0f));
            Matrices.mulPose(Axis.YP.rotationDegrees(Side * -45.0f));
        }
        return true;
    }

    private static void turn(PoseStack Matrices, LocalPlayer User, HumanoidArm Arm, ItemStack Stack, int Side, float Progress) {
        BakedModel Baked = Minecraft.getInstance().getItemRenderer().getModel(Stack, User.level(), User, 0);
        ItemTransform Transform = Baked.getTransforms().getTransform(Arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        Quaternionf Rotation = new Quaternionf().rotationXYZ(Transform.rotation.x() * Mth.DEG_TO_RAD, Transform.rotation.y() * Side * Mth.DEG_TO_RAD, Transform.rotation.z() * Side * Mth.DEG_TO_RAD);
        Vector3f Along = Rotation.transform(new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f Pivot = Rotation.transform(new Vector3f(Grip).mul(Transform.scale)).add(Transform.translation.x() * Side, Transform.translation.y(), Transform.translation.z());
        boolean Pushes = Scp005KeyItem.turnPushes();
        float Out = smooth((Progress - 0.72f) / 0.28f);
        float Push = Pushes ? PushDepth * (smooth(Progress / 0.3f) - Out) : 0.0f;
        float Angle = Pushes ? TurnAngle * smooth((Progress - 0.25f) / 0.35f) * (1.0f - Out) : TwistAngle * Mth.sin(Progress * Mth.PI);
        Matrices.translate(Along.x() * Push, Along.y() * Push, Along.z() * Push);
        Matrices.rotateAround(new Quaternionf().rotationAxis(Angle * Side * Mth.DEG_TO_RAD, Along.x(), Along.y(), Along.z()), Pivot.x(), Pivot.y(), Pivot.z());
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }
}
