package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.Scp5001Event;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001DeviceBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001DeviceBlockEntity;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001RodEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;
import org.joml.Matrix4f;

public class Scp5001RodRenderer implements BlockEntityRenderer<Scp5001DeviceBlockEntity> {
    public static final ResourceLocation Texture = Scp5001EventRegistry.location("textures/entity/scp5001_event/rod.png");
    public static final ResourceLocation GlowTexture = Scp5001EventRegistry.location("textures/entity/scp5001_event/rod_glow.png");
    public static final float TextureWidth = 32.0f;
    public static final float TextureHeight = 64.0f;
    public static final float SocketDepth = 3.0f / 16.0f;
    public static final double LowHull = 2.5;
    public static final double HighHull = 11.5;
    public static final int ViewDistance = 160;

    public Scp5001RodRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static Vec3 socket(BlockPos Pos, Direction Facing, double Out) {
        return Vec3.atCenterOf(Pos).add(Facing.getStepX() * (0.5 + Out), 0.0, Facing.getStepZ() * (0.5 + Out));
    }

    public static Vec3 anchor(Scp5001Vessel Vessel, Vec3 Start, float Partial) {
        Vec3 Base = Scp5001Beams.vesselAt(Vessel, Partial);
        double Height = Mth.clamp(Start.y - Base.y, LowHull, HighHull);
        Vec3 Axis = new Vec3(Base.x, Base.y + Height, Base.z);
        Vec3 Toward = Start.subtract(Axis);
        Vec3 Flat = new Vec3(Toward.x, 0.0, Toward.z);
        if (Flat.lengthSqr() < 1.0E-4) {
            return Axis;
        }
        return Axis.add(Flat.normalize().scale(Scp5001Vessel.HullRadius));
    }

    @Override
    public void render(Scp5001DeviceBlockEntity Rod, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        BlockState State = Rod.getBlockState();
        if (!State.hasProperty(Scp5001RodEmitterBlock.Spent) || State.getValue(Scp5001RodEmitterBlock.Spent)) {
            return;
        }
        float Extension = Scp5001ClientState.rodExtension(Partial);
        Scp5001Vessel Vessel = Scp5001Beams.vessel();
        if (Extension <= 0.001f || Vessel == null) {
            return;
        }
        BlockPos Pos = Rod.getBlockPos();
        Direction Facing = State.getValue(Scp5001DeviceBlock.Facing);
        Vec3 Start = socket(Pos, Facing, SocketDepth);
        Vec3 Stop = anchor(Vessel, Start, Partial);
        Vec3 Along = Stop.subtract(Start);
        double Full = Along.length();
        float Eased = 1.0f - (1.0f - Extension) * (1.0f - Extension) * (1.0f - Extension);
        float Length = (float) (Full * Scp5001Event.LatchShare * Eased);
        float Time = Scp5001ClientState.now() + Partial;
        float Strain = Scp5001ClientState.phase() == Scp5001State.Phase.Rising ? Scp5001ClientState.lift() : 0.25f;
        float Shudder = Extension >= 1.0f ? Mth.sin(Time * 2.3f + Pos.hashCode()) * 0.015f * (0.5f + Strain) : 0.0f;
        Pose.pushPose();
        Vec3 Local = Start.subtract(Vec3.atLowerCornerOf(Pos));
        Pose.translate(Local.x, Local.y, Local.z);
        Scp5001Beams.aim(Pose, Along);
        Pose.translate(Shudder, -Shudder, 0.0f);
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Body = Buffers.getBuffer(RenderType.entityCutoutNoCull(Texture));
        int Colour = 0xFFFFFFFF;
        float JointA = Math.min(Length, Length * 0.42f + 0.3f);
        float JointB = Length * 0.78f;
        Scp5001Beams.box(Last, Body, 0.21f, 0.0f, JointA, 0.0f, 7.0f, TextureWidth, TextureHeight, Light, Colour);
        Scp5001Beams.box(Last, Body, 0.27f, Math.max(0.0f, JointA - 0.18f), JointA, 32.0f, 41.0f, TextureWidth, TextureHeight, Light, Colour);
        if (Length > 0.6f) {
            Scp5001Beams.box(Last, Body, 0.16f, JointA - 0.1f, Math.max(JointA, JointB), 8.0f, 14.0f, TextureWidth, TextureHeight, Light, Colour);
            Scp5001Beams.box(Last, Body, 0.21f, Math.max(JointA, JointB) - 0.14f, Math.max(JointA, JointB), 32.0f, 41.0f, TextureWidth, TextureHeight, Light, Colour);
            Scp5001Beams.box(Last, Body, 0.11f, Math.max(JointA, JointB) - 0.1f, Length, 16.0f, 20.0f, TextureWidth, TextureHeight, Light, Colour);
        }
        Scp5001Beams.box(Last, Body, 0.19f, Length - 0.05f, Length + 0.22f, 42.0f, 48.0f, TextureWidth, TextureHeight, Light, Colour);
        for (int Index = 0; Index < 4; Index++) {
            Pose.pushPose();
            Pose.translate(0.0f, 0.0f, Length + 0.2f);
            Pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Index * 90.0f + 45.0f));
            Pose.translate(0.0f, 0.2f + (Extension >= 1.0f ? 0.06f : 0.0f), 0.0f);
            Pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-25.0f));
            Scp5001Beams.box(Pose.last(), Body, 0.05f, 0.0f, 0.34f, 50.0f, 52.0f, TextureWidth, TextureHeight, Light, Colour);
            Pose.popPose();
        }
        VertexConsumer Glow = Buffers.getBuffer(RenderType.eyes(GlowTexture));
        float Pulse = 0.6f + 0.4f * Scp5001Beams.flicker(Time * 0.2f, Pos.hashCode());
        int Lit = 0xFF000000 | (int) (255 * Pulse) << 16 | (int) (255 * Pulse) << 8 | (int) (255 * Pulse);
        Scp5001Beams.box(Last, Glow, 0.212f, 0.0f, JointA, 0.0f, 7.0f, TextureWidth, TextureHeight, LightTexture.FULL_BRIGHT, Lit);
        if (Length > 0.6f) {
            Scp5001Beams.box(Last, Glow, 0.162f, JointA - 0.1f, Math.max(JointA, JointB), 8.0f, 14.0f, TextureWidth, TextureHeight, LightTexture.FULL_BRIGHT, Lit);
        }
        Pose.popPose();
        if (Extension >= 1.0f) {
            beam(Pose, Buffers, Pos, Start.add(Along.normalize().scale(Length + 0.3)), Stop, Time, Strain);
        }
    }

    private static void beam(PoseStack Pose, MultiBufferSource Buffers, BlockPos Pos, Vec3 Tip, Vec3 Stop, float Time, float Strain) {
        Vec3 Origin = Vec3.atLowerCornerOf(Pos);
        Vec3 From = Tip.subtract(Origin);
        Vec3 To = Stop.subtract(Origin);
        VertexConsumer Light = Buffers.getBuffer(RenderType.lightning());
        Matrix4f Matrix = Pose.last().pose();
        int Seed = Pos.hashCode();
        float Flicker = Scp5001Beams.flicker(Time * 0.5f, Seed);
        boolean Drop = Strain > 0.6f && Mth.sin(Time * 0.73f + Seed) > 0.96f;
        if (Drop) {
            return;
        }
        float Wobble = 0.05f + 0.18f * Strain;
        Scp5001Beams.strained(Matrix, Light, From, To, 0.09f, Wobble * 0.4f, Time, Seed, 200, 245, 255, (int) (210 * Flicker));
        Scp5001Beams.strained(Matrix, Light, From, To, 0.05f, Wobble, Time * 1.3f, Seed + 7, 143, 227, 255, (int) (150 * Flicker));
        Scp5001Beams.strained(Matrix, Light, From, To, 0.05f, Wobble * 1.2f, Time * 0.9f, Seed + 13, 156, 140, 240, (int) (130 * Flicker));
        Scp5001Beams.ribbon(Matrix, Light, From, To, 0.32f, 120, 200, 255, (int) (28 * Flicker));
        Scp5001Beams.star(Matrix, Light, To, 0.35f + 0.1f * Flicker, Time * 0.2f, 200, 240, 255, (int) (160 * Flicker));
    }

    @Override
    public boolean shouldRenderOffScreen(Scp5001DeviceBlockEntity Rod) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return ViewDistance;
    }

    @Override
    public AABB getRenderBoundingBox(Scp5001DeviceBlockEntity Rod) {
        return new AABB(Rod.getBlockPos()).inflate(48.0);
    }
}
