package net.rai.scp_unleashed.orbital.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.orbital.OrbitalStrikeEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class OrbitalStrikeRenderer extends EntityRenderer<OrbitalStrikeEntity> {
    public static final float[] Marker = {1.0F, 0.16F, 0.1F};
    public static final float[] Outer = {0.95F, 0.16F, 0.05F};
    public static final float[] Middle = {1.0F, 0.48F, 0.12F};
    public static final float[] Core = {1.0F, 0.93F, 0.78F};
    public static final float[] Hot = {1.0F, 0.98F, 0.94F};
    public static final int FenceSegments = 32;
    public static final int RingTurnTicks = 80;
    public static final int DescendTicks = 5;
    public static final int BurstTicks = 4;
    public static final float ShockSpeed = 2.4F;
    public static final int ShockTicks = 16;
    public static final double SkyMargin = 48.0;

    public OrbitalStrikeRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(OrbitalStrikeEntity Strike) {
        return OrbitalRenderTypes.BeamTexture;
    }

    @Override
    public boolean shouldRender(OrbitalStrikeEntity Strike, Frustum View, double CameraX, double CameraY, double CameraZ) {
        double Dx = Strike.getX() - CameraX;
        double Dz = Strike.getZ() - CameraZ;
        return Dx * Dx + Dz * Dz < OrbitalStrikeEntity.RenderReach * OrbitalStrikeEntity.RenderReach;
    }

    @Override
    public void render(OrbitalStrikeEntity Strike, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Age = Strike.getAge() + Partial;
        int Charge = Strike.getCharge();
        float Radius = Strike.getRadius();
        Vec3 Camera = this.entityRenderDispatcher.camera.getPosition();
        Vec3 Toward = new Vec3(Camera.x - Strike.getX(), 0.0, Camera.z - Strike.getZ());
        float SideX = 1.0F;
        float SideZ = 0.0F;
        if (Toward.lengthSqr() > 1.0E-4) {
            Vec3 Side = new Vec3(-Toward.z, 0.0, Toward.x).normalize();
            SideX = (float) Side.x;
            SideZ = (float) Side.z;
        }
        float Top = (float) (Strike.level().getMaxBuildHeight() + SkyMargin - Strike.getY());
        PoseStack.Pose Last = Pose.last();
        if (Age < Charge) {
            warning(Strike, Last, Buffers, Age, Charge, Radius, SideX, SideZ, Top);
        } else {
            beam(Strike, Pose, Buffers, Age - Charge, Radius, SideX, SideZ, Top);
        }
    }

    private void warning(OrbitalStrikeEntity Strike, PoseStack.Pose Last, MultiBufferSource Buffers, float Age, int Charge, float Radius, float SideX, float SideZ, float Top) {
        float Progress = Mth.clamp(Age / Charge, 0.0F, 1.0F);
        float Pulse = 0.75F + 0.25F * Mth.sin(Age * (0.6F + Progress * 1.4F));
        float RingRadius = Radius * (1.7F - 0.7F * Progress);
        float Turn = Age / RingTurnTicks * Mth.TWO_PI;
        float Scroll = -Age * 0.12F;
        VertexConsumer Ring = Buffers.getBuffer(OrbitalRenderTypes.tint(OrbitalRenderTypes.RingTexture));
        flat(Ring, Last, 0.05F, RingRadius, Turn, Marker, 1.0F, 0.85F * Pulse);
        VertexConsumer Fence = Buffers.getBuffer(OrbitalRenderTypes.tint(OrbitalRenderTypes.FenceTexture));
        fence(Fence, Last, RingRadius, 1.0F + 2.0F * Progress, Marker, 0.75F * Pulse);
        VertexConsumer AimTint = Buffers.getBuffer(OrbitalRenderTypes.tint(OrbitalRenderTypes.BeamTexture));
        column(AimTint, Last, SideX, SideZ, 0.3F + 0.35F * Progress, 0.0F, Top, Scroll, Marker, 0.75F + 0.2F * Progress);
        VertexConsumer Glow = Buffers.getBuffer(OrbitalRenderTypes.glow(OrbitalRenderTypes.RingTexture));
        flat(Glow, Last, 0.07F, RingRadius, Turn, Marker, 0.95F * Pulse, 1.0F);
        flat(Glow, Last, 0.09F, Radius * 0.55F, -Turn * 1.7F, Marker, 0.55F * Pulse * Progress, 1.0F);
        VertexConsumer FenceGlow = Buffers.getBuffer(OrbitalRenderTypes.glow(OrbitalRenderTypes.FenceTexture));
        fence(FenceGlow, Last, RingRadius, 1.4F + 2.6F * Progress, Marker, 0.7F * Pulse);
        VertexConsumer Aim = Buffers.getBuffer(OrbitalRenderTypes.glow(OrbitalRenderTypes.BeamTexture));
        column(Aim, Last, SideX, SideZ, 0.9F + 1.2F * Progress, 0.0F, Top, Scroll, Marker, 0.55F + 0.35F * Pulse);
        float Left = Charge - Age;
        if (Left < DescendTicks) {
            float Head = Top * Mth.clamp(Left / DescendTicks, 0.0F, 1.0F);
            column(Aim, Last, SideX, SideZ, Radius * 0.6F, Head, Top, Scroll * 3.0F, Core, 0.9F);
            column(Aim, Last, SideX, SideZ, Radius * 1.2F, Head, Top, Scroll * 2.0F, Middle, 0.7F);
        }
    }

    private void beam(OrbitalStrikeEntity Strike, PoseStack Pose, MultiBufferSource Buffers, float Since, float Radius, float SideX, float SideZ, float Top) {
        float Hold = OrbitalStrikeEntity.LingerTicks;
        float Life = Since < Hold ? 1.0F : Mth.clamp(1.0F - (Since - Hold) / OrbitalStrikeEntity.FadeTicks, 0.0F, 1.0F);
        if (Life <= 0.0F) {
            return;
        }
        float Fade = Life * Life * (3.0F - 2.0F * Life);
        float Burst = Since < BurstTicks ? 1.6F - 0.15F * Since : 1.0F;
        float Flicker = 0.9F + 0.1F * Mth.sin(Since * 2.9F);
        float Thin = 0.35F + 0.65F * Mth.sqrt(Life);
        float Width = Radius * Burst * Thin;
        float Scroll = -Since * 0.35F;
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Tint = Buffers.getBuffer(OrbitalRenderTypes.tint(OrbitalRenderTypes.BeamTexture));
        column(Tint, Last, SideX, SideZ, Width * 0.6F, -0.5F, Top, Scroll * 1.6F, Middle, 0.8F * Fade);
        VertexConsumer Glow = Buffers.getBuffer(OrbitalRenderTypes.glow(OrbitalRenderTypes.BeamTexture));
        column(Glow, Last, SideX, SideZ, Width * 1.6F, -0.5F, Top, Scroll * 0.5F, Outer, 0.7F * Fade * Flicker);
        column(Glow, Last, SideX, SideZ, Width * 1.0F, -0.5F, Top, Scroll, Middle, 0.85F * Fade * Flicker);
        column(Glow, Last, SideX, SideZ, Width * 0.5F, -0.5F, Top, Scroll * 1.6F, Core, Fade);
        column(Glow, Last, SideX, SideZ, Width * 0.25F, -0.5F, Top, Scroll * 2.2F, Hot, Fade);
        VertexConsumer Floor = Buffers.getBuffer(OrbitalRenderTypes.glow(OrbitalRenderTypes.FlareTexture));
        flat(Floor, Last, 0.12F, Radius * 1.5F * Burst, Since * 0.05F, Middle, 0.9F * Fade, 1.0F);
        flat(Floor, Last, 0.14F, Radius * 0.9F * Burst, -Since * 0.08F, Core, Fade, 1.0F);
        if (Since < ShockTicks) {
            float Wave = 1.0F - Since / ShockTicks;
            VertexConsumer Shock = Buffers.getBuffer(OrbitalRenderTypes.glow(OrbitalRenderTypes.ShockTexture));
            flat(Shock, Last, 0.3F, Radius + Since * ShockSpeed * 2.0F, 0.0F, Middle, Wave, 1.0F);
            flat(Shock, Last, 1.4F, Radius * 0.8F + Since * ShockSpeed * 1.4F, 0.4F, Core, Wave * 0.7F, 1.0F);
        }
        if (Since < Hold) {
            Pose.pushPose();
            Pose.translate(0.0F, 1.2F, 0.0F);
            Pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            VertexConsumer Flare = Buffers.getBuffer(OrbitalRenderTypes.glow(OrbitalRenderTypes.FlareTexture));
            billboard(Flare, Pose.last(), Radius * 1.5F * Burst, Middle, 0.6F * Fade);
            billboard(Flare, Pose.last(), Radius * 0.8F * Burst, Hot, 0.9F * Fade);
            Pose.popPose();
        }
    }

    private static void column(VertexConsumer Consumer, PoseStack.Pose Last, float SideX, float SideZ, float Half, float Bottom, float Top, float Scroll, float[] Colour,
                               float Strength) {
        float Ax = SideX * Half;
        float Az = SideZ * Half;
        float Repeat = (Top - Bottom) / 16.0F;
        vertex(Consumer, Last, -Ax, Bottom, -Az, 0.0F, Scroll + Repeat, Colour, Strength);
        vertex(Consumer, Last, Ax, Bottom, Az, 1.0F, Scroll + Repeat, Colour, Strength);
        vertex(Consumer, Last, Ax, Top, Az, 1.0F, Scroll, Colour, Strength);
        vertex(Consumer, Last, -Ax, Top, -Az, 0.0F, Scroll, Colour, Strength);
    }

    private static void flat(VertexConsumer Consumer, PoseStack.Pose Last, float Height, float Radius, float Turn, float[] Colour, float Strength, float Alpha) {
        float[] Us = {0.0F, 1.0F, 1.0F, 0.0F};
        float[] Vs = {0.0F, 0.0F, 1.0F, 1.0F};
        for (int Corner = 0; Corner < 4; Corner++) {
            float Angle = Turn + Mth.PI * 0.25F + Corner * Mth.HALF_PI;
            float X = Mth.cos(Angle) * Radius * Mth.SQRT_OF_TWO;
            float Z = Mth.sin(Angle) * Radius * Mth.SQRT_OF_TWO;
            vertexAlpha(Consumer, Last, X, Height, Z, Us[Corner], Vs[Corner], Colour, Strength, Alpha);
        }
    }

    private static void fence(VertexConsumer Consumer, PoseStack.Pose Last, float Radius, float Height, float[] Colour, float Strength) {
        for (int Segment = 0; Segment < FenceSegments; Segment++) {
            float A = Segment * Mth.TWO_PI / FenceSegments;
            float B = (Segment + 1) * Mth.TWO_PI / FenceSegments;
            float X0 = Mth.cos(A) * Radius;
            float Z0 = Mth.sin(A) * Radius;
            float X1 = Mth.cos(B) * Radius;
            float Z1 = Mth.sin(B) * Radius;
            float U0 = (Segment % 4) * 0.25F;
            float U1 = U0 + 0.25F;
            vertexAlpha(Consumer, Last, X0, 0.02F, Z0, U0, 1.0F, Colour, Strength, Strength);
            vertexAlpha(Consumer, Last, X1, 0.02F, Z1, U1, 1.0F, Colour, Strength, Strength);
            vertexAlpha(Consumer, Last, X1, Height, Z1, U1, 0.0F, Colour, Strength, Strength);
            vertexAlpha(Consumer, Last, X0, Height, Z0, U0, 0.0F, Colour, Strength, Strength);
        }
    }

    private static void billboard(VertexConsumer Consumer, PoseStack.Pose Last, float Size, float[] Colour, float Strength) {
        vertex(Consumer, Last, -Size, -Size, 0.0F, 0.0F, 1.0F, Colour, Strength);
        vertex(Consumer, Last, Size, -Size, 0.0F, 1.0F, 1.0F, Colour, Strength);
        vertex(Consumer, Last, Size, Size, 0.0F, 1.0F, 0.0F, Colour, Strength);
        vertex(Consumer, Last, -Size, Size, 0.0F, 0.0F, 0.0F, Colour, Strength);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float[] Colour, float Strength) {
        vertexAlpha(Consumer, Last, X, Y, Z, U, V, Colour, Strength, Strength);
    }

    private static void vertexAlpha(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float[] Colour, float Strength, float Alpha) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Mth.clamp(Colour[0] * Strength, 0.0F, 1.0F), Mth.clamp(Colour[1] * Strength, 0.0F, 1.0F), Mth.clamp(Colour[2] * Strength, 0.0F, 1.0F),
                        Mth.clamp(Alpha, 0.0F, 1.0F))
                .setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0F, 1.0F, 0.0F);
    }
}
