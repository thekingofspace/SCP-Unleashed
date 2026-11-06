package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.GridStatus;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlockEntity;
import net.rai.scp_unleashed.rifttech.PylonGrid;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.ArrayList;
import java.util.List;

public class PylonRenderer implements BlockEntityRenderer<InstabilityPylonBlockEntity> {
    public static final int[][] FlatEdges = {{0, 1}, {0, 2}, {1, 3}, {2, 3}};
    public static final int[][] BoxEdges = {{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final long FlickerTicks = 2L;

    public PylonRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float[] tint(InstabilityPylonBlockEntity Pylon) {
        boolean Golden = Pylon.getClientStatus() == GridStatus.Apocalyptic || Pylon.getClientStatus() == GridStatus.Holding;
        int Colour = Golden ? ApocalypticRiftEntity.Colour : Pylon.getClientTier().getColour();
        return new float[]{((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f};
    }

    @Override
    public void render(InstabilityPylonBlockEntity Pylon, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        if (Pylon.getLevel() == null || Pylon.getClientLayout() == null || !Pylon.isClientMaster() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        PoseStack.Pose Saved = Pose.last().copy();
        RiftTechDeferred.queue(Own -> draw(Pylon, PartialTick, Saved, Own, true));
        RiftTechDeferred.queueLate(Own -> draw(Pylon, PartialTick, Saved, Own, false));
    }

    private void draw(InstabilityPylonBlockEntity Pylon, float PartialTick, PoseStack.Pose Last, MultiBufferSource Buffers, boolean CorePass) {
        Level World = Pylon.getLevel();
        PylonGrid.Layout Grid = Pylon.getClientLayout();
        if (World == null || Grid == null) {
            return;
        }
        float Glow = Pylon.getGlow(PartialTick);
        float Flash = Pylon.getFlash(PartialTick);
        if (Glow < 0.02f && Flash < 0.02f) {
            return;
        }
        BlockPos Origin = Pylon.getBlockPos();
        Vec3 Base = Vec3.atLowerCornerOf(Origin);
        Vec3 Eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(Base);
        GridStatus Status = Pylon.getClientStatus();
        boolean Charging = Status.isCharging();
        boolean Golden = Status == GridStatus.Apocalyptic || Status == GridStatus.Holding;
        float[] Tint = tint(Pylon);
        float[] Core = {mix(Tint[0], 1.0f, 0.65f), mix(Tint[1], 1.0f, 0.65f), mix(Tint[2], 1.0f, 0.65f)};
        float[] Halo = {mix(Tint[0], 0.55f, 0.15f), mix(Tint[1], 0.75f, 0.15f), mix(Tint[2], 1.0f, 0.15f)};
        List<BlockPos> Corners = Grid.corners();
        List<Vec3> Points = new ArrayList<>(Corners.size());
        for (BlockPos Corner : Corners) {
            Points.add(InstabilityPylonBlockEntity.emitterOf(Corner).subtract(Base));
        }
        int[][] Edges = Grid.isBox() ? BoxEdges : FlatEdges;
        long Window = World.getGameTime() / FlickerTicks;
        RandomSource Random = RandomSource.create(Origin.asLong() * 31L + Window * 977L);
        VertexConsumer Arc = Buffers.getBuffer(CorePass ? RiftTechRenderTypes.ArcCore : RiftTechRenderTypes.Arc);
        float Strength = Charging ? Glow : Glow * 0.8f;
        for (int[] Edge : Edges) {
            Vec3 From = Points.get(Edge[0]);
            Vec3 To = Points.get(Edge[1]);
            if (Charging) {
                List<Vec3> Path = path(Random, From, To, 5, 0.09);
                ribbon(CorePass, Arc, Last, Path, Eye, 0.06f, Core, 0.9f * Strength);
                ribbon(!CorePass, Arc, Last, Path, Eye, 0.2f, Halo, 0.45f * Strength);
                if (Random.nextFloat() < 0.35f * Glow) {
                    ribbon(CorePass, Arc, Last, path(Random, From, To, 5, 0.16), Eye, 0.04f, Core, 0.5f * Strength);
                }
            } else {
                List<Vec3> Straight = path(Random, From, To, 3, 0.012);
                ribbon(CorePass, Arc, Last, Straight, Eye, 0.035f, Core, 0.55f * Strength);
            }
        }
        Vec3 Centre = Grid.centre().subtract(Base);
        if (Golden && Charging) {
            float Charge = Pylon.getClientApocalyptic();
            if (Charge > 0.2f) {
                float Pull = Mth.clamp((Charge - 0.2f) / 0.8f, 0.0f, 1.0f);
                for (Vec3 Point : Points) {
                    if (Random.nextFloat() < 0.25f + 0.6f * Pull) {
                        List<Vec3> Path = path(Random, Point, Centre, 5, 0.14);
                        ribbon(CorePass, Arc, Last, Path, Eye, 0.05f, Core, 0.7f * Pull * Glow);
                        ribbon(!CorePass, Arc, Last, Path, Eye, 0.16f, Halo, 0.35f * Pull * Glow);
                    }
                }
            }
        }
        Vec3 Surge = Pylon.getSurgeSpot();
        if (Flash > 0.05f && Surge != null) {
            Vec3 Spot = Surge.subtract(Base);
            float[] SurgeCore = Pylon.isSurgeGolden() ? new float[]{1.0f, 0.86f, 0.45f} : Core;
            for (Vec3 Point : Points) {
                List<Vec3> Path = path(Random, Point, Spot, 5, 0.12);
                ribbon(CorePass, Arc, Last, Path, Eye, 0.07f, SurgeCore, Flash);
                ribbon(!CorePass, Arc, Last, Path, Eye, 0.24f, Halo, 0.5f * Flash);
            }
        }
        VertexConsumer Lamp = Buffers.getBuffer(RiftTechRenderTypes.Glow);
        float Pulse = 0.85f + 0.15f * Mth.sin((Pylon.getClientTicks() + PartialTick) * (Charging ? 0.35f : 0.12f));
        for (Vec3 Point : Points) {
            sprite(!CorePass, Lamp, Last, Point, Eye, 0.35f + 0.2f * Glow, Halo, Glow * Pulse * 0.45f);
            sprite(!CorePass, Lamp, Last, Point, Eye, 0.16f + 0.08f * Glow, Core, Glow * Pulse * 0.8f);
        }
        if (Golden && Charging) {
            float Charge = Pylon.getClientApocalyptic();
            sprite(!CorePass, Lamp, Last, Centre, Eye, 0.3f + 1.4f * Charge, Halo, (0.25f + 0.6f * Charge) * Glow);
            sprite(!CorePass, Lamp, Last, Centre, Eye, 0.12f + 0.5f * Charge, Core, (0.35f + 0.65f * Charge) * Glow * Pulse);
        }
    }

    private static float mix(float From, float To, float Amount) {
        return From + (To - From) * Amount;
    }

    public static List<Vec3> path(RandomSource Random, Vec3 Start, Vec3 End, int Levels, double Roughness) {
        return Ribbons.path(Random, Start, End, Levels, Roughness);
    }

    private static void ribbon(boolean Show, VertexConsumer Consumer, PoseStack.Pose Last, List<Vec3> Path, Vec3 Eye, float HalfWidth, float[] Colour, float Strength) {
        if (Show && Strength > 0.01f) {
            Ribbons.ribbon(Consumer, Last, Path, Eye, HalfWidth, 1.0f, Colour[0] * Strength, Colour[1] * Strength, Colour[2] * Strength, 1.0f);
        }
    }

    private static void sprite(boolean Show, VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, Vec3 Eye, float Radius, float[] Colour, float Strength) {
        if (Show && Strength > 0.01f) {
            Ribbons.sprite(Consumer, Last, Centre, Eye, Radius, Colour[0] * Strength, Colour[1] * Strength, Colour[2] * Strength, 1.0f);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(InstabilityPylonBlockEntity Pylon) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(InstabilityPylonBlockEntity Pylon) {
        return Pylon.getRenderBounds();
    }
}
