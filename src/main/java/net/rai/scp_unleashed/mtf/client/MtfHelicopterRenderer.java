package net.rai.scp_unleashed.mtf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.mtf.MtfHelicopter;
import net.rai.scp_unleashed.mtf.MtfLandingZone;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MtfHelicopterRenderer extends GeoEntityRenderer<MtfHelicopter> {
    public static final ResourceLocation RopeTexture = MtfRegistry.location("textures/entity/mtf/mtf_rope.png");
    public static final float RopeHalf = 1.0f / 16.0f;
    public static final float BobHeight = 0.06f;
    public static final float SwayRoll = 0.022f;
    public static final float SwayPitch = 0.014f;

    public MtfHelicopterRenderer(EntityRendererProvider.Context Context) {
        super(Context, new MtfHelicopterGeoModel());
        addRenderLayer(new MtfHelicopterGlowLayer(this));
        shadowRadius = 2.2f;
        shadowStrength = 0.6f;
    }

    @Override
    protected void applyRotations(MtfHelicopter Owner, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        float Yaw = Mth.rotLerp(PartialTick, Owner.yRotO, Owner.getYRot());
        float Age = Owner.tickCount + PartialTick;
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Yaw));
        Pose.translate(0.0f, Mth.sin(Age * 0.11f) * BobHeight, 0.0f);
        Pose.mulPose(Axis.XP.rotation(-Owner.tilt(PartialTick) + Mth.sin(Age * 0.07f) * SwayPitch));
        Pose.mulPose(Axis.ZP.rotation(Mth.sin(Age * 0.05f) * SwayRoll));
    }

    @Override
    public void render(MtfHelicopter Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
        ropes(Owner, PartialTick, Pose, Buffers);
    }

    private void ropes(MtfHelicopter Owner, float PartialTick, PoseStack Pose, MultiBufferSource Buffers) {
        float Facing = Mth.rotLerp(PartialTick, Owner.yRotO, Owner.getYRot());
        VertexConsumer Buffer = null;
        double BaseX = Mth.lerp(PartialTick, Owner.xo, Owner.getX());
        double BaseY = Mth.lerp(PartialTick, Owner.yo, Owner.getY());
        double BaseZ = Mth.lerp(PartialTick, Owner.zo, Owner.getZ());
        for (int Side = 0; Side < 2; Side++) {
            float Length = Owner.shownRope(Side, PartialTick);
            if (Length < 0.05f) {
                continue;
            }
            if (Buffer == null) {
                Buffer = Buffers.getBuffer(RenderType.entityCutoutNoCull(RopeTexture));
            }
            Vec3 Anchor = MtfLandingZone.anchor(Vec3.ZERO, Facing, Side);
            Pose.pushPose();
            Pose.translate(Anchor.x, Anchor.y, Anchor.z);
            PoseStack.Pose Last = Pose.last();
            float Top = 0.0f;
            while (Top > -Length) {
                float Bottom = Math.max(-Length, Top - 1.0f);
                float Share = Top - Bottom;
                int Packed = LevelRenderer.getLightColor(Owner.level(), BlockPos.containing(BaseX + Anchor.x, BaseY + Anchor.y + (Top + Bottom) * 0.5f, BaseZ + Anchor.z));
                strip(Buffer, Last, Top, Bottom, Share, Packed, true);
                strip(Buffer, Last, Top, Bottom, Share, Packed, false);
                Top = Bottom;
            }
            Pose.popPose();
        }
    }

    private static void strip(VertexConsumer Buffer, PoseStack.Pose Last, float Top, float Bottom, float Share, int Light, boolean Facing) {
        float U0 = Facing ? 0.0f : 0.5f;
        float U1 = Facing ? 0.5f : 1.0f;
        float X0 = Facing ? -RopeHalf : 0.0f;
        float X1 = Facing ? RopeHalf : 0.0f;
        float Z0 = Facing ? 0.0f : -RopeHalf;
        float Z1 = Facing ? 0.0f : RopeHalf;
        float Nx = Facing ? 0.0f : 1.0f;
        float Nz = Facing ? 1.0f : 0.0f;
        vertex(Buffer, Last, X0, Top, Z0, U0, 0.0f, Light, Nx, Nz);
        vertex(Buffer, Last, X0, Bottom, Z0, U0, Share, Light, Nx, Nz);
        vertex(Buffer, Last, X1, Bottom, Z1, U1, Share, Light, Nx, Nz);
        vertex(Buffer, Last, X1, Top, Z1, U1, 0.0f, Light, Nx, Nz);
    }

    private static void vertex(VertexConsumer Buffer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, int Light, float Nx, float Nz) {
        Buffer.addVertex(Last, X, Y, Z).setColor(255, 255, 255, 255).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, Nx, 0.0f, Nz);
    }
}
