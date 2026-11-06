package net.rai.scp_unleashed.gateway.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlockEntity;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayGeometry;
import net.rai.scp_unleashed.gateway.GatewayRegistry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class ContainedGatewayRenderer implements BlockEntityRenderer<ContainedGatewayBlockEntity> {
    public static final ResourceLocation PortalTexture = GatewayRegistry.location("textures/block/gateway/portal.png");
    public static final ResourceLocation LedTexture = GatewayRegistry.location("textures/block/gateway/led.png");
    public static final RenderType Portal = RenderType.entityTranslucentEmissive(PortalTexture);
    public static final RenderType Led = RenderType.entityTranslucentEmissive(LedTexture);
    public static final float[] Green = {0.35f, 1.0f, 0.45f};
    public static final float[] Amber = {1.0f, 0.66f, 0.15f};
    public static final float[] Red = {1.0f, 0.2f, 0.14f};
    public static final float[] Dark = {0.16f, 0.17f, 0.2f};
    public static final float ShardScale = 0.42f;
    public static final float LedSize = 2.0f;

    public ContainedGatewayRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(ContainedGatewayBlockEntity Gate, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Gate.getLevel();
        if (World == null) {
            return;
        }
        Direction Front = Gate.front();
        BlockPos Origin = Gate.origin();
        Pose.pushPose();
        Pose.translate(Origin.getX() - Gate.getBlockPos().getX() + 0.5, Origin.getY() - Gate.getBlockPos().getY(), Origin.getZ() - Gate.getBlockPos().getZ() + 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Front.toYRot()));
        Pose.translate((GatewayGeometry.PixelsWide / 2 - GatewayGeometry.OriginCentreX) / 16.0, 0.0, (GatewayGeometry.PixelsDeep / 2 - GatewayGeometry.OriginCentreZ) / 16.0);
        Pose.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        Pose.translate(-GatewayGeometry.PixelsWide / 2.0, 0.0, -GatewayGeometry.PixelsDeep / 2.0);
        float Clock = Gate.ticks() + PartialTick;
        ItemStack Shard = Gate.shard();
        if (!Shard.isEmpty()) {
            renderShard(Gate, Shard, Pose, Buffers, PackedLight, Clock);
        }
        if (!ShaderCompat.renderingShadowPass()) {
            renderLeds(Gate, Pose, Buffers, Clock);
            float Bloom = Gate.bloom(PartialTick);
            GatewayDestination Destination = Gate.destination();
            if (Bloom > 0.01f && Destination != null) {
                renderPortal(Pose, Buffers, Destination, Bloom, Clock);
            }
        }
        Pose.popPose();
    }

    private void renderShard(ContainedGatewayBlockEntity Gate, ItemStack Shard, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, float Clock) {
        Pose.pushPose();
        Pose.translate(GatewayGeometry.Socket.x, GatewayGeometry.Socket.y, GatewayGeometry.Socket.z);
        Pose.scale(16.0f, 16.0f, 16.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        float Bob = Gate.isOpen() ? Mth.sin(Clock * 0.08f) * 0.01f : 0.0f;
        Pose.translate(0.0, Bob, 0.0);
        Pose.scale(ShardScale, ShardScale, ShardScale);
        int Light = Gate.isOpen() ? LightTexture.FULL_BRIGHT : PackedLight;
        Minecraft.getInstance().getItemRenderer().renderStatic(Shard, ItemDisplayContext.FIXED, Light, OverlayTexture.NO_OVERLAY, Pose, Buffers, Gate.getLevel(), 4666);
        Pose.popPose();
    }

    private void renderLeds(ContainedGatewayBlockEntity Gate, PoseStack Pose, MultiBufferSource Buffers, float Clock) {
        float[] Colour = switch (Gate.status()) {
            case ContainedGatewayBlockEntity.StatusOpen -> Green;
            case ContainedGatewayBlockEntity.StatusStandby -> Amber;
            case ContainedGatewayBlockEntity.StatusFault -> ((int) (Clock / 6.0f)) % 2 == 0 ? Red : Dark;
            default -> Dark;
        };
        float Alpha = Colour == Dark ? 0.6f : 1.0f;
        VertexConsumer Consumer = Buffers.getBuffer(Led);
        PoseStack.Pose Last = Pose.last();
        for (double Across : GatewayGeometry.LampColumns) {
            float X0 = (float) Across - LedSize / 2;
            float Y0 = (float) GatewayGeometry.LampHeight - LedSize / 2;
            float Z = (float) GatewayGeometry.Lintel.minZ - 0.05f;
            quad(Consumer, Last, X0, Y0, Z, X0 + LedSize, Y0 + LedSize, Z, 0.0f, 0.0f, 1.0f, 1.0f, Colour, Alpha);
            float Back = (float) GatewayGeometry.Lintel.maxZ + 0.05f;
            quad(Consumer, Last, X0 + LedSize, Y0, Back, X0, Y0 + LedSize, Back, 0.0f, 0.0f, 1.0f, 1.0f, Colour, Alpha);
        }
    }

    private void renderPortal(PoseStack Pose, MultiBufferSource Buffers, GatewayDestination Destination, float Bloom, float Clock) {
        VertexConsumer Consumer = Buffers.getBuffer(Portal);
        PoseStack.Pose Last = Pose.last();
        AABB Face = GatewayGeometry.Surface;
        float[] Tint = {Destination.red(), Destination.green(), Destination.blue()};
        float Height = (float) (Face.maxY - Face.minY);
        float Width = (float) (Face.maxX - Face.minX);
        for (int Layer = 0; Layer < 3; Layer++) {
            float Z = (float) (7.4 + Layer * 0.6);
            float Drift = Clock * (0.004f + Layer * 0.003f) * (Layer % 2 == 0 ? 1.0f : -1.0f);
            float Sway = Mth.sin(Clock * 0.03f + Layer * 2.1f) * 0.06f;
            float U0 = Sway;
            float U1 = Sway + Width / 32.0f;
            float V0 = Drift;
            float V1 = Drift + Height / 64.0f;
            float Alpha = Bloom * (Layer == 1 ? 0.55f : 0.32f) * (0.9f + 0.1f * Mth.sin(Clock * 0.11f + Layer));
            float Shade = Layer == 1 ? 1.0f : 0.8f;
            float[] Colour = {Tint[0] * Shade, Tint[1] * Shade, Tint[2] * Shade};
            float Inset = (1.0f - Bloom) * Width * 0.5f;
            quad(Consumer, Last, (float) Face.minX + Inset, (float) Face.minY, Z, (float) Face.maxX - Inset, (float) Face.maxY, Z, U0, V0, U1, V1, Colour, Alpha);
        }
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, float X0, float Y0, float Z0, float X1, float Y1, float Z1, float U0, float V0, float U1, float V1,
                             float[] Colour, float Alpha) {
        vertex(Consumer, Last, X0, Y0, Z0, U0, V1, Colour, Alpha);
        vertex(Consumer, Last, X1, Y0, Z1, U1, V1, Colour, Alpha);
        vertex(Consumer, Last, X1, Y1, Z1, U1, V0, Colour, Alpha);
        vertex(Consumer, Last, X0, Y1, Z0, U0, V0, Colour, Alpha);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float[] Colour, float Alpha) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Colour[0], Colour[1], Colour[2], Alpha)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 0.0f, -1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(ContainedGatewayBlockEntity Gate) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ContainedGatewayBlockEntity Gate) {
        return Gate.getRenderBounds();
    }
}
