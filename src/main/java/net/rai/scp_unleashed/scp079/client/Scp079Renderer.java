package net.rai.scp_unleashed.scp079.client;

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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp079.Scp079Block;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079Mood;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class Scp079Renderer implements BlockEntityRenderer<Scp079BlockEntity> {
    public static final float ScreenLeft = 3.0f;
    public static final float ScreenRight = -9.0f;
    public static final float ScreenBottom = 4.0f;
    public static final float ScreenTop = 13.0f;
    public static final float GlassDepth = 7.0f - 0.06f;
    public static final float BarDepth = 7.0f - 0.09f;
    public static final int BarPeriod = 70;
    public static final int BarTravel = 12;
    public static final double LookThreshold = 1.25;

    public Scp079Renderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp079BlockEntity Computer, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        Scp079Mood Mood = Computer.getShownMood();
        if (!Mood.isFace()) {
            return;
        }
        Direction Front = Computer.facing();
        int Ticks = Computer.getClientTicks();
        int Seed = (int) (Computer.getBlockPos().asLong() & 0xFF);
        int Frame = Scp079Faces.frame(Mood, Ticks, Seed, look(Computer, Front));
        float Flicker = 0.84f + 0.16f * Mth.abs(Mth.sin((Ticks + PartialTick) * 0.9f + Seed) * Mth.sin((Ticks + PartialTick) * 0.37f));
        Pose.pushPose();
        Pose.translate(0.5, 0.0, 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(-facingAngle(Front)));
        Pose.translate(-0.5, 0.0, -0.5);
        Pose.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        PoseStack.Pose Last = Pose.last();
        quad(Buffers.getBuffer(renderType()), Last, ScreenBottom, ScreenTop, GlassDepth, Scp079Faces.u0(Frame), Scp079Faces.u1(Frame),
                Scp079Faces.v0(Mood.getRow()), Scp079Faces.v1(Mood.getRow()), Flicker);
        int Phase = Math.floorMod(Ticks + Seed * 3, BarPeriod);
        if (Phase < BarTravel) {
            float Row = ScreenTop - 1.0f - Phase * (ScreenTop - ScreenBottom - 1.0f) / (BarTravel - 1.0f);
            float Strength = 0.32f * (1.0f - Phase / (float) BarTravel);
            int BarFrame = Math.floorMod(Ticks, 4);
            float V0 = Scp079Faces.v0(Scp079Mood.StaticRow) + (Phase % 8) / (float) Scp079Faces.TextureHeight;
            quad(Buffers.getBuffer(renderType()), Last, Row, Row + 1.0f, BarDepth, Scp079Faces.u0(BarFrame), Scp079Faces.u1(BarFrame),
                    V0, V0 + 1.0f / Scp079Faces.TextureHeight, Strength);
        }
        Pose.popPose();
    }

    private static RenderType renderType() {
        return ShaderCompat.useFallback() ? RenderType.entityTranslucentEmissive(Scp079Faces.Texture) : RenderType.eyes(Scp079Faces.Texture);
    }

    private static int look(Scp079BlockEntity Computer, Direction Front) {
        Player Viewer = Minecraft.getInstance().player;
        if (Viewer == null) {
            return Scp079Faces.LookCentre;
        }
        Direction Right = Scp079Block.rightOf(Front);
        Vec3 Offset = Viewer.position().subtract(Computer.screenCentre());
        double Across = Offset.x * Right.getStepX() + Offset.z * Right.getStepZ();
        if (Across < -LookThreshold) {
            return Scp079Faces.LookLeft;
        }
        return Across > LookThreshold ? Scp079Faces.LookRight : Scp079Faces.LookCentre;
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, float Bottom, float Top, float Depth, float U0, float U1, float V0, float V1, float Bright) {
        vertex(Consumer, Last, ScreenLeft, Bottom, Depth, U0, V1, Bright);
        vertex(Consumer, Last, ScreenRight, Bottom, Depth, U1, V1, Bright);
        vertex(Consumer, Last, ScreenRight, Top, Depth, U1, V0, Bright);
        vertex(Consumer, Last, ScreenLeft, Top, Depth, U0, V0, Bright);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Bright) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Bright, Bright, Bright, Math.min(1.0f, Bright + 0.1f))
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 0.0f, -1.0f);
    }

    public static float facingAngle(Direction Front) {
        return switch (Front) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(Scp079BlockEntity Computer) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(Scp079BlockEntity Computer) {
        return Computer.getRenderBounds();
    }
}
