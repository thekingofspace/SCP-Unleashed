package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunTimer;

public final class GunPose {
    public static final float EquipDrop = 0.6F;
    public static final float ReloadDrop = 0.08F;
    public static final float ReloadShift = 0.04F;
    public static final float ReloadYaw = 10.0F;
    public static final float ReloadPitch = 14.0F;
    public static final float ReloadRoll = 28.0F;
    public static final float SprintDrop = 0.05F;
    public static final float SprintYaw = 26.0F;
    public static final float SprintPitch = -16.0F;
    public static final float SprintRoll = 8.0F;
    public static final float JoltLift = 0.018F;
    public static final float JoltPitch = 5.0F;
    public static final float ChargeShake = 0.0035F;

    private GunPose() {
    }

    public static void apply(PoseStack Pose, BakedModel Model, ItemStack Stack, GunKind Kind, HumanoidArm Arm, float Partial, float EquipProcess) {
        GunView View = GunView.of(Kind);
        int Side = Arm == HumanoidArm.RIGHT ? 1 : -1;
        float Aim = GunClientState.aimEased(Partial);
        float Hip = 1.0F - Aim;
        float Kick = GunClientState.kick(Partial);
        float Lowered = GunClientState.lowered(Partial);
        float Sprint = GunClientState.sprint(Partial);
        float X = Side * View.HipX() * Hip;
        float Y = View.HipY() * Hip - EquipProcess * EquipDrop;
        float Z = Mth.lerp(Aim, View.HipZ(), -View.AimDepth());
        float Yaw = Side * View.HipYaw() * Hip;
        float Pitch = View.HipPitch() * Hip;
        float Roll = Side * View.HipRoll() * Hip;
        Z += View.KickBack() * Kick * (1.0F - 0.4F * Aim);
        Pitch += View.KickPitch() * Kick * (1.0F - 0.5F * Aim);
        X -= Side * ReloadShift * Lowered;
        Y -= ReloadDrop * Lowered;
        Yaw += Side * ReloadYaw * Lowered;
        Pitch += ReloadPitch * Lowered;
        Roll += Side * ReloadRoll * Lowered;
        long Now = gameTime();
        float Time = (Now % 100000L) + Partial;
        GunTimer Reload = GunItem.getReload(Stack);
        if (Reload != null) {
            float Progress = Reload.progress(Now, Partial);
            float Jolt = (float) Math.exp(-Math.pow((Progress - 0.72F) / 0.05F, 2.0));
            Y += JoltLift * Jolt;
            Pitch += JoltPitch * Jolt;
        }
        Y -= SprintDrop * Sprint;
        Yaw += Side * SprintYaw * Sprint;
        Pitch += SprintPitch * Sprint;
        Roll += Side * SprintRoll * Sprint;
        GunTimer Charging = GunItem.getCharging(Stack);
        if (Charging != null) {
            float Progress = Charging.progress(Now, Partial);
            float Shake = ChargeShake * Progress * Progress;
            X += Shake * Mth.sin(Time * 9.7F);
            Y += Shake * Mth.cos(Time * 13.1F);
            Z += 0.02F * Progress;
        }
        Pose.translate(X, Y, Z);
        Pose.mulPose(Axis.YP.rotationDegrees(Yaw));
        Pose.mulPose(Axis.XP.rotationDegrees(Pitch));
        Pose.mulPose(Axis.ZP.rotationDegrees(Roll));
        float Scale = scaleOf(Model, Arm);
        Pose.translate(0.0F, -Scale * (View.SightY() / 16.0F - 0.5F), -Scale * (View.RearZ() / 16.0F - 0.5F));
    }

    public static float scaleOf(BakedModel Model, HumanoidArm Arm) {
        ItemTransforms Transforms = Model.getTransforms();
        ItemTransform Transform = Arm == HumanoidArm.RIGHT ? Transforms.firstPersonRightHand : Transforms.firstPersonLeftHand;
        return Transform.scale.y();
    }

    public static long gameTime() {
        Minecraft Game = Minecraft.getInstance();
        return Game.level == null ? 0L : Game.level.getGameTime();
    }
}
