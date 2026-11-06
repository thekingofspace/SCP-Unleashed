package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.function.Function;

public final class NpcGunPose {
    public static final float ModelScale = 1.0F;
    public static final float HeadPitchLimit = 50.0F;
    public static final float HeadYawLimit = 75.0F;
    public static final float KickTicks = 4.0F;
    public static final float KickStrength = 1.6F;
    public static final float OffKickShare = 0.7F;
    public static final float MainYaw = 0.3F;
    public static final float OffYaw = 0.6F;
    public static final float MainLift = 0.1F;
    public static final float OffReach = 1.5F;
    public static final float HandX = 1.0F / 16.0F;
    public static final float HandY = 0.125F;
    public static final float HandZ = -0.625F;
    private static int PosedId = Integer.MIN_VALUE;
    private static float PosedAge = Float.NaN;
    private static float PosedShare;

    private NpcGunPose() {
    }

    @Nullable
    public static GunKind held(LivingEntity Holder) {
        return GunItem.kindOf(Holder.getMainHandItem());
    }

    public static boolean aiming(LivingEntity Holder) {
        return Holder instanceof Mob Body && Body.isAggressive() || GunClientState.firedRecently(Holder.getId()) || GunItem.getCharging(Holder.getMainHandItem()) != null;
    }

    public static float kick(LivingEntity Holder, GunKind Kind, float Partial) {
        float Age = GunClientState.shotAge(Holder.getId(), Partial);
        if (Age < 0.0F || Age >= KickTicks) {
            return 0.0F;
        }
        float Fade = 1.0F - Age / KickTicks;
        return Kind.getStats().getRecoilPitch() * Mth.DEG_TO_RAD * KickStrength * Fade * Fade;
    }

    public static float[] hold(LivingEntity Holder, GunKind Kind, boolean Main, float HeadX, float HeadY, float Partial) {
        boolean RightHanded = Holder.getMainArm() == HumanoidArm.RIGHT;
        boolean RightArm = RightHanded == Main;
        float Kick = kick(Holder, Kind, Partial);
        float Age = Holder.tickCount + Partial;
        float Bob = RightArm ? 1.0F : -1.0F;
        float X;
        float Y;
        if (Main) {
            X = -Mth.HALF_PI + HeadX + MainLift - Kick;
            Y = (RightHanded ? -MainYaw : MainYaw) + HeadY;
        } else {
            X = -OffReach + HeadX - Kick * OffKickShare;
            Y = (RightHanded ? OffYaw : -OffYaw) + HeadY;
        }
        X += Bob * Mth.sin(Age * 0.067F) * 0.05F;
        float Z = Bob * (Mth.cos(Age * 0.09F) * 0.05F + 0.05F);
        return new float[]{X, Y, Z};
    }

    public static float[] mainArm(LivingEntity Holder, GunKind Kind, float Partial) {
        float BodyYaw = Mth.rotLerp(Partial, Holder.yBodyRotO, Holder.yBodyRot);
        float HeadYaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(Partial, Holder.yHeadRotO, Holder.yHeadRot) - BodyYaw), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD;
        float HeadPitch = Mth.clamp(Holder.getViewXRot(Partial), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD;
        float[] Aim = hold(Holder, Kind, true, HeadPitch, HeadYaw, Partial);
        if (Kind.getStats().isLongGun()) {
            return Aim;
        }
        float Weight = GunStance.peek(Holder, Partial);
        if (Weight >= 1.0F) {
            return Aim;
        }
        boolean RightHanded = Holder.getMainArm() == HumanoidArm.RIGHT;
        float[] Low = GunStance.low(RightHanded, Holder.getOffhandItem().isEmpty(), true, Holder.walkAnimation.position(Partial), Holder.walkAnimation.speed(Partial),
                Holder.tickCount + Partial, true);
        return GunStance.mix(Low, Aim, Weight);
    }

    public static boolean apply(Function<String, GeoBone> Bones, LivingEntity Holder, boolean Aiming, float HeadX, float HeadY, float Partial) {
        GunKind Kind = held(Holder);
        if (Kind == null) {
            return false;
        }
        boolean RightHanded = Holder.getMainArm() == HumanoidArm.RIGHT;
        GeoBone Main = Bones.apply(RightHanded ? "right_arm" : "left_arm");
        GeoBone Off = Bones.apply(RightHanded ? "left_arm" : "right_arm");
        if (Kind.getStats().isLongGun()) {
            PosedId = Integer.MIN_VALUE;
            set(Main, hold(Holder, Kind, true, HeadX, HeadY, Partial));
            set(Off, hold(Holder, Kind, false, HeadX, HeadY, Partial));
            return true;
        }
        boolean Snap = GunClientState.firedRecently(Holder.getId()) || GunItem.getCharging(Holder.getMainHandItem()) != null;
        float Weight = GunStance.aim(Holder, Aiming, Snap, Partial);
        boolean Support = Holder.getOffhandItem().isEmpty();
        float WalkPos = Holder.walkAnimation.position(Partial);
        float WalkSpeed = Holder.walkAnimation.speed(Partial);
        float Age = Holder.tickCount + Partial;
        set(Main, GunStance.mix(GunStance.low(RightHanded, Support, true, WalkPos, WalkSpeed, Age, true), hold(Holder, Kind, true, HeadX, HeadY, Partial), Weight));
        if (Off != null && (Support || Weight > 0.0F)) {
            float[] From = Support ? GunStance.low(RightHanded, true, false, WalkPos, WalkSpeed, Age, true) : new float[]{-Off.getRotX(), -Off.getRotY(), Off.getRotZ()};
            set(Off, GunStance.mix(From, hold(Holder, Kind, false, HeadX, HeadY, Partial), Weight));
        }
        PosedId = Holder.getId();
        PosedAge = Age;
        PosedShare = 1.0F - Weight;
        return true;
    }

    public static boolean posed(LivingEntity Holder, float Partial) {
        return PosedId == Holder.getId() && PosedAge == Holder.tickCount + Partial;
    }

    public static void level(PoseStack Pose, LivingEntity Holder, ItemStack Stack, @Nullable GeoBone Arm, boolean Left, float Partial) {
        GunKind Kind = GunItem.kindOf(Stack);
        if (Stack != Holder.getMainHandItem() || Kind == null || Kind.getStats().isLongGun()) {
            return;
        }
        LivingEntity Source = Holder;
        float Share;
        if (posed(Holder, Partial)) {
            Share = PosedShare;
        } else if (Holder instanceof DClassLike Body && Body.isPuppet() && Body.puppetOwner() instanceof Player Driver) {
            Source = Driver;
            Share = 1.0F - GunStance.peek(Driver, Partial);
        } else {
            return;
        }
        if (Share <= 0.0F) {
            return;
        }
        float Side = Left ? -1.0F : 1.0F;
        Pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        Pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        Pose.translate(Side * HandX, HandY, HandZ);
        GunStance.straighten(Pose, Source, Stack, Left, Share);
        Pose.translate(-Side * HandX, -HandY, -HandZ);
        Pose.mulPose(Axis.YP.rotationDegrees(-180.0F));
        Pose.mulPose(Axis.XP.rotationDegrees(90.0F));
    }

    private static void set(@Nullable GeoBone Bone, float[] Vanilla) {
        if (Bone == null) {
            return;
        }
        Bone.setRotX(-Vanilla[0]);
        Bone.setRotY(-Vanilla[1]);
        Bone.setRotZ(Vanilla[2]);
    }
}
