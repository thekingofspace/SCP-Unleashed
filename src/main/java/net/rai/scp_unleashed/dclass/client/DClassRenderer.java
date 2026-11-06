package net.rai.scp_unleashed.dclass.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.dclass.DClassBeds;
import net.rai.scp_unleashed.dclass.DClassLike;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.Color;

public class DClassRenderer<T extends DClassLike> extends SharedGeoRenderer<T> {
    public static final float RestLift = 0.26f;
    public static final float MattressSink = 0.3f;
    public static final float MattressRise = 1.0f;
    public static final Color Undead = Color.ofRGB(0.62f, 0.78f, 0.58f);

    public DClassRenderer(EntityRendererProvider.Context Context) {
        this(Context, new DClassGeoModel<>());
    }

    protected DClassRenderer(EntityRendererProvider.Context Context, GeoModel<T> Model) {
        super(Context, Model);
        addRenderLayer(new HumanoidGearLayer<>(this));
        shadowRadius = 0.45f;
    }

    @Override
    public void render(T Owner, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Matrices, Buffers, Light);
    }

    private static void nestle(DClassLike Owner, PoseStack Matrices, float PartialTick, float NativeScale) {
        DClassBeds.Spot Found = DClassBeds.sleepingSpot(Owner);
        if (Found == null || Found.Vanilla()) {
            return;
        }
        float Height = (float) (Found.Head().getY() + Found.Mattress() - Mth.lerp(PartialTick, Owner.yOld, Owner.getY()));
        float Lift = Mth.clamp(Height, -MattressSink, MattressRise);
        float Ahead = 0.0f;
        if (Found.Foot() == null) {
            EntityDimensions Standing = Owner.getDimensions(Pose.STANDING);
            Ahead = Standing.eyeHeight() - 0.1f - Standing.height() * 0.5f;
        }
        float Scale = NativeScale <= 0.0f ? 1.0f : NativeScale;
        Matrices.translate(Found.Toward().getStepX() * Ahead / Scale, Lift / Scale, Found.Toward().getStepZ() * Ahead / Scale);
    }

    @Override
    public Color getRenderColor(T Owner, float PartialTick, int PackedLight) {
        return Owner.isUndead() ? Undead : super.getRenderColor(Owner, PartialTick, PackedLight);
    }

    @Override
    protected void applyRotations(T Owner, PoseStack Matrices, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        if (Owner.isResting() && !Owner.isSleeping() && Owner.deathTime <= 0) {
            Matrices.mulPose(Axis.YP.rotationDegrees(180.0f - RotationYaw));
            Matrices.translate(0.0f, RestLift, 0.0f);
            Matrices.mulPose(Axis.XP.rotationDegrees(90.0f));
            Matrices.translate(0.0f, -0.9f, 0.0f);
            return;
        }
        if (Owner.isSleeping() && Owner.deathTime <= 0) {
            nestle(Owner, Matrices, PartialTick, NativeScale);
        }
        super.applyRotations(Owner, Matrices, AgeInTicks, RotationYaw, PartialTick, NativeScale);
        LivingEntity Driver = Owner.isPuppet() ? Owner.puppetOwner() : null;
        if (Driver == null || Owner.deathTime > 0) {
            return;
        }
        if (Driver.isAutoSpinAttack() && !Driver.hasPose(Pose.SLEEPING)) {
            Matrices.mulPose(Axis.XP.rotationDegrees(-90.0f - Driver.getXRot()));
            Matrices.mulPose(Axis.YP.rotationDegrees((Driver.tickCount + PartialTick) * -75.0f));
        }
        float Swim = Driver.getSwimAmount(PartialTick);
        float Pitch = Driver.getViewXRot(PartialTick);
        if (Driver.isFallFlying()) {
            float Time = Driver.getFallFlyingTicks() + PartialTick;
            float Share = Mth.clamp(Time * Time / 100.0f, 0.0f, 1.0f);
            if (!Driver.isAutoSpinAttack()) {
                Matrices.mulPose(Axis.XP.rotationDegrees(Share * (-90.0f - Pitch)));
            }
            Vec3 View = Driver.getViewVector(PartialTick);
            Vec3 Motion = Driver instanceof AbstractClientPlayer Flyer ? Flyer.getDeltaMovementLerped(PartialTick) : Driver.getDeltaMovement();
            double Flat = Motion.horizontalDistanceSqr();
            double ViewFlat = View.horizontalDistanceSqr();
            if (Flat > 0.0 && ViewFlat > 0.0) {
                double Dot = (Motion.x * View.x + Motion.z * View.z) / Math.sqrt(Flat * ViewFlat);
                double Cross = Motion.x * View.z - Motion.z * View.x;
                Matrices.mulPose(Axis.YP.rotation((float) (Math.signum(Cross) * Math.acos(Mth.clamp(Dot, -1.0, 1.0)))));
            }
        } else if (Swim > 0.0f) {
            float Target = Driver.isInWater() || Driver.isInFluidType((Kind, Height) -> Driver.canSwimInFluidType(Kind)) ? -90.0f - Driver.getXRot() : -90.0f;
            Matrices.mulPose(Axis.XP.rotationDegrees(Mth.lerp(Swim, 0.0f, Target)));
            if (Driver.isVisuallySwimming()) {
                Matrices.translate(0.0f, -1.0f, 0.3f);
            }
        }
    }
}
