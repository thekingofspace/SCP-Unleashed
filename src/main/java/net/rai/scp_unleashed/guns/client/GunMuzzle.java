package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunKind;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GunMuzzle {
    private static final float HandFovScale = (float) (1.0 / Math.tan(Math.toRadians(35.0)));

    private GunMuzzle() {
    }

    public static Vector3f muzzlePoint(GunKind Kind) {
        GunView View = GunView.of(Kind);
        return new Vector3f(0.5F, View.MuzzleY() / 16.0F, View.MuzzleZ() / 16.0F);
    }

    public static BakedModel modelFor(LivingEntity User, ItemStack Stack) {
        return Minecraft.getInstance().getItemRenderer().getModel(Stack, User.level(), User, User.getId());
    }

    public static Vector3f handPoint(LocalPlayer User, ItemStack Stack, GunKind Kind, float Partial) {
        BakedModel Model = modelFor(User, Stack);
        HumanoidArm Arm = User.getMainArm();
        boolean Left = Arm == HumanoidArm.LEFT;
        PoseStack Hand = new PoseStack();
        Hand.mulPose(Axis.XP.rotationDegrees((User.getViewXRot(Partial) - Mth.lerp(Partial, User.xBobO, User.xBob)) * 0.1F));
        Hand.mulPose(Axis.YP.rotationDegrees((User.getViewYRot(Partial) - Mth.lerp(Partial, User.yBobO, User.yBob)) * 0.1F));
        GunPose.apply(Hand, Model, Stack, Kind, Arm, Partial, 0.0F);
        Model.applyTransform(Left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, Hand, Left);
        Hand.translate(-0.5F, -0.5F, -0.5F);
        return Hand.last().pose().transformPosition(muzzlePoint(Kind), new Vector3f());
    }

    public static Vec3 firstPersonWorld(LocalPlayer User, ItemStack Stack, GunKind Kind, Camera View, Matrix4f Projection, float Partial) {
        Vector3f Tip = handPoint(User, Stack, Kind, Partial);
        float Spread = HandFovScale / Math.max(0.05F, Projection.m11());
        Vector3f Look = View.getLookVector();
        Vector3f Up = View.getUpVector();
        Vector3f Left = View.getLeftVector();
        double Across = Tip.x() * Spread;
        double Rise = Tip.y() * Spread;
        double Depth = -Tip.z();
        return View.getPosition().add(
                Look.x() * Depth + Up.x() * Rise - Left.x() * Across,
                Look.y() * Depth + Up.y() * Rise - Left.y() * Across,
                Look.z() * Depth + Up.z() * Rise - Left.z() * Across);
    }

    public static Vec3 thirdPersonWorld(LivingEntity User, ItemStack Stack, GunKind Kind, float Partial) {
        BakedModel Model = modelFor(User, Stack);
        boolean LeftHand = User.getMainArm() == HumanoidArm.LEFT;
        float BodyYaw = Mth.rotLerp(Partial, User.yBodyRotO, User.yBodyRot);
        float HeadYaw = Mth.wrapDegrees(Mth.rotLerp(Partial, User.yHeadRotO, User.yHeadRot) - BodyYaw);
        float HeadPitch = User.getViewXRot(Partial);
        boolean Crouching = User.isCrouching();
        boolean Slim = User instanceof AbstractClientPlayer Client && Client.getSkin().model() == PlayerSkin.Model.SLIM;
        float Age = User.tickCount + Partial;
        float Side = LeftHand ? -1.0F : 1.0F;
        float PivotX = -Side * (Slim ? 4.5F : 5.0F);
        float PivotY = Crouching ? 5.2F : 2.0F;
        float ArmX = -Mth.HALF_PI + HeadPitch * Mth.DEG_TO_RAD + 0.1F + (Crouching ? 0.4F : 0.0F) + Side * Mth.sin(Age * 0.067F) * 0.05F;
        float ArmY = -Side * 0.3F + HeadYaw * Mth.DEG_TO_RAD;
        float ArmZ = Side * (Mth.cos(Age * 0.09F) * 0.05F + 0.05F);
        if (!(User instanceof Player)) {
            float[] Held = NpcGunPose.mainArm(User, Kind, Partial);
            ArmX = Held[0];
            ArmY = Held[1];
            ArmZ = Held[2];
        }
        float Scale = User.getScale();
        Vec3 Position = User.getPosition(Partial);
        PoseStack Pose = new PoseStack();
        Pose.translate(Position.x, Position.y + (Crouching ? Scale * -2.0F / 16.0F : 0.0F), Position.z);
        Pose.scale(Scale, Scale, Scale);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0F - BodyYaw));
        Pose.scale(-1.0F, -1.0F, 1.0F);
        float Body = User instanceof Player ? 0.9375F : NpcGunPose.ModelScale;
        Pose.scale(Body, Body, Body);
        Pose.translate(0.0F, -1.501F, 0.0F);
        Pose.translate(PivotX / 16.0F, PivotY / 16.0F, 0.0F);
        Pose.mulPose(new Quaternionf().rotationZYX(ArmZ, ArmY, ArmX));
        Pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        Pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        Pose.translate(Side / 16.0F, 0.125F, -0.625F);
        Model.applyTransform(LeftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, Pose, LeftHand);
        Pose.translate(-0.5F, -0.5F, -0.5F);
        Vector3f Tip = Pose.last().pose().transformPosition(muzzlePoint(Kind), new Vector3f());
        return new Vec3(Tip.x(), Tip.y(), Tip.z());
    }

    public static Vec3 thirdPersonForward(LivingEntity User, float Partial) {
        return Vec3.directionFromRotation(User.getViewXRot(Partial), User.getViewYRot(Partial));
    }
}
