package net.rai.scp_unleashed.microhid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlock;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlockEntity;
import net.rai.scp_unleashed.microhid.MicroChargingStatus;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.tesla.client.TeslaRenderTypes;

import java.util.ArrayList;
import java.util.List;

public class MicroChargingPlatformRenderer implements BlockEntityRenderer<MicroChargingPlatformBlockEntity> {
    public static final double[][] ProngTips = {{4.5, 9.5, 8.6}, {11.5, 9.5, 8.6}};
    public static final double GunX = 8.0;
    public static final double GunY = 6.0;
    public static final double GunZ = 6.5;
    public static final float GunScale = 0.3F;
    public static final float BellyY = 2.5F;
    public static final double[] CoilLow = {1.6, 7.4, 5.2};
    public static final double[] CoilHigh = {5.4, 9.6, 7.8};
    public static final int ArcLevels = 4;
    public static final double ArcRoughness = 0.22;
    public static final int ArcTicks = 2;
    public static final float[] Core = {0.92F, 0.97F, 1.0F};
    public static final float[] Halo = {0.36F, 0.6F, 1.0F};

    public MicroChargingPlatformRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static Vec3 place(double X, double Y, double Z, Direction Facing) {
        return switch (Facing) {
            case EAST -> new Vec3((16.0 - Z) / 16.0, Y / 16.0, X / 16.0);
            case SOUTH -> new Vec3((16.0 - X) / 16.0, Y / 16.0, (16.0 - Z) / 16.0);
            case WEST -> new Vec3(Z / 16.0, Y / 16.0, (16.0 - X) / 16.0);
            default -> new Vec3(X / 16.0, Y / 16.0, Z / 16.0);
        };
    }

    public static float turn(Direction Facing) {
        return switch (Facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    @Override
    public void render(MicroChargingPlatformBlockEntity Platform, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        ItemStack Docked = Platform.getDocked();
        Level World = Platform.getLevel();
        if (Docked.isEmpty() || World == null) {
            return;
        }
        Direction Facing = Platform.getBlockState().getBlock() instanceof MicroChargingPlatformBlock ? Platform.getBlockState().getValue(MicroChargingPlatformBlock.Facing) : Direction.NORTH;
        boolean Charging = Platform.getBlockState().getBlock() instanceof MicroChargingPlatformBlock
                && Platform.getBlockState().getValue(MicroChargingPlatformBlock.Status) == MicroChargingStatus.Charging;
        Pose.pushPose();
        Pose.translate(0.5F, 0.0F, 0.5F);
        Pose.mulPose(Axis.YP.rotationDegrees(turn(Facing)));
        Pose.translate(-0.5F, 0.0F, -0.5F);
        Pose.translate(GunX / 16.0, GunY / 16.0, GunZ / 16.0);
        Pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        Pose.scale(GunScale, GunScale, GunScale);
        Pose.translate(0.0F, (8.0F - BellyY) / 16.0F, 0.0F);
        Minecraft.getInstance().getItemRenderer().renderStatic(Docked, ItemDisplayContext.NONE, PackedLight, OverlayTexture.NO_OVERLAY, Pose, Buffers, World,
                Charging ? MicroHidClientEvents.ChargingSeed : 0);
        Pose.popPose();
        if (!Charging || ShaderCompat.renderingShadowPass()) {
            return;
        }
        BlockPos Pos = Platform.getBlockPos();
        Vec3 Eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(Vec3.atLowerCornerOf(Pos));
        long Step = World.getGameTime() / ArcTicks;
        RandomSource Random = RandomSource.create(Pos.asLong() * 31L + Step);
        VertexConsumer Consumer = Buffers.getBuffer(TeslaRenderTypes.additive(MicroHidEffects.ArcTexture));
        PoseStack.Pose Last = Pose.last();
        float Flicker = 0.7F + 0.3F * Random.nextFloat();
        for (double[] Tip : ProngTips) {
            if (Random.nextFloat() < 0.25F) {
                continue;
            }
            Vec3 From = place(Tip[0], Tip[1], Tip[2], Facing);
            double TargetX = Mth.lerp(Random.nextDouble(), CoilLow[0], CoilHigh[0]);
            if (Tip[0] > 8.0) {
                TargetX = 16.0 - TargetX;
            }
            Vec3 To = place(TargetX, Mth.lerp(Random.nextDouble(), CoilLow[1], CoilHigh[1]), Mth.lerp(Random.nextDouble(), CoilLow[2], CoilHigh[2]), Facing);
            List<Vec3> Path = path(Random, From, To);
            ribbon(Consumer, Last, Path, Eye, 0.012F, Core, Flicker);
            ribbon(Consumer, Last, Path, Eye, 0.04F, Halo, 0.6F * Flicker);
        }
        VertexConsumer Glow = Buffers.getBuffer(TeslaRenderTypes.additive(MicroHidEffects.GlowTexture));
        for (double[] Tip : ProngTips) {
            Vec3 Centre = place(Tip[0], Tip[1], Tip[2], Facing);
            sprite(Glow, Pose.last(), Centre, Eye, 0.08F, Halo, 0.7F * Flicker);
        }
    }

    private static List<Vec3> path(RandomSource Random, Vec3 Start, Vec3 End) {
        List<Vec3> Points = new ArrayList<>();
        Points.add(Start);
        Points.add(End);
        double Amount = Start.distanceTo(End) * ArcRoughness;
        for (int Round = 0; Round < ArcLevels; Round++) {
            List<Vec3> Next = new ArrayList<>(Points.size() * 2);
            Next.add(Points.getFirst());
            for (int Index = 0; Index + 1 < Points.size(); Index++) {
                Vec3 A = Points.get(Index);
                Vec3 B = Points.get(Index + 1);
                Next.add(A.add(B).scale(0.5).add((Random.nextDouble() * 2.0 - 1.0) * Amount, (Random.nextDouble() * 2.0 - 1.0) * Amount, (Random.nextDouble() * 2.0 - 1.0) * Amount));
                Next.add(B);
            }
            Points = Next;
            Amount *= 0.5;
        }
        return Points;
    }

    private static void ribbon(VertexConsumer Consumer, PoseStack.Pose Last, List<Vec3> Path, Vec3 Eye, float HalfWidth, float[] Colour, float Strength) {
        int Count = Path.size();
        Vec3[] Sides = new Vec3[Count];
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Along = Path.get(Math.min(Count - 1, Index + 1)).subtract(Path.get(Math.max(0, Index - 1)));
            Vec3 Side = Along.cross(Eye.subtract(Path.get(Index)));
            Sides[Index] = Side.lengthSqr() < 1.0E-12 ? (Index > 0 ? Sides[Index - 1] : Vec3.ZERO) : Side.normalize().scale(HalfWidth);
            if (Index > 0 && Sides[Index].dot(Sides[Index - 1]) < 0.0) {
                Sides[Index] = Sides[Index].scale(-1.0);
            }
        }
        float Red = Mth.clamp(Colour[0] * Strength, 0.0F, 1.0F);
        float Green = Mth.clamp(Colour[1] * Strength, 0.0F, 1.0F);
        float Blue = Mth.clamp(Colour[2] * Strength, 0.0F, 1.0F);
        for (int Index = 0; Index + 1 < Count; Index++) {
            Vec3 A = Path.get(Index);
            Vec3 B = Path.get(Index + 1);
            Vec3 Normal = Eye.subtract(A).normalize();
            vertex(Consumer, Last, A.subtract(Sides[Index]), 0.0F, 1.0F, Red, Green, Blue, Normal);
            vertex(Consumer, Last, A.add(Sides[Index]), 1.0F, 1.0F, Red, Green, Blue, Normal);
            vertex(Consumer, Last, B.add(Sides[Index + 1]), 1.0F, 0.0F, Red, Green, Blue, Normal);
            vertex(Consumer, Last, B.subtract(Sides[Index + 1]), 0.0F, 0.0F, Red, Green, Blue, Normal);
        }
    }

    private static void sprite(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, Vec3 Eye, float Radius, float[] Colour, float Strength) {
        Vec3 Facing = Eye.subtract(Centre).normalize();
        Vec3 Across = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Across = Across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        Vec3 Over = Across.cross(Facing).normalize();
        Vec3 U = Across.scale(Radius);
        Vec3 V = Over.scale(Radius);
        float Red = Mth.clamp(Colour[0] * Strength, 0.0F, 1.0F);
        float Green = Mth.clamp(Colour[1] * Strength, 0.0F, 1.0F);
        float Blue = Mth.clamp(Colour[2] * Strength, 0.0F, 1.0F);
        vertex(Consumer, Last, Centre.subtract(U).subtract(V), 0.0F, 1.0F, Red, Green, Blue, Facing);
        vertex(Consumer, Last, Centre.add(U).subtract(V), 1.0F, 1.0F, Red, Green, Blue, Facing);
        vertex(Consumer, Last, Centre.add(U).add(V), 1.0F, 0.0F, Red, Green, Blue, Facing);
        vertex(Consumer, Last, Centre.subtract(U).add(V), 0.0F, 0.0F, Red, Green, Blue, Facing);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float U, float V, float Red, float Green, float Blue, Vec3 Normal) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Red, Green, Blue, 1.0F).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }
}
