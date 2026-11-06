package net.rai.scp_unleashed.scp076.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp076.Scp076DoorBlock;
import net.rai.scp_unleashed.scp076.Scp076DoorBlockEntity;
import org.joml.Quaternionf;

public class Scp076DoorRenderer implements BlockEntityRenderer<Scp076DoorBlockEntity> {
    public static final float BoltTravel = 7.0f / 16.0f;
    public static final float DoorHeight = Scp076DoorBlock.Rows;

    public Scp076DoorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp076DoorBlockEntity Door, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Door.getLevel();
        if (World == null) {
            return;
        }
        float Drop = Door.drop(Partial);
        if (Drop >= 0.999f) {
            return;
        }
        Direction Front = Door.front();
        Direction Across = Scp076DoorBlock.across(Front);
        BlockPos Origin = Door.getBlockPos();
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-Front.toYRot()));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.translate(0.0f, -Drop * DoorHeight, 0.0f);
        int Sunk = (int) Math.floor(Drop * DoorHeight);
        for (int Column = 0; Column < Scp076DoorBlock.Columns; Column++) {
            for (int Row = Sunk; Row < Scp076DoorBlock.Rows; Row++) {
                int Light = lightAt(World, Origin, Front, Across, Column, Row - Sunk);
                Pose.pushPose();
                Pose.translate(Column, Row, 0.0f);
                Scp076Models.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Scp076Models.get(Scp076Models.panel(Column, Row)), Light, PackedOverlay, 1.0f, 1.0f, World, Front);
                Pose.popPose();
            }
        }
        for (int Lock = 0; Lock < Scp076DoorBlockEntity.LockCount; Lock++) {
            int[] Spot = Scp076DoorBlockEntity.Locks[Lock];
            if (Spot[1] / 16.0f + 0.5f < Drop * DoorHeight) {
                continue;
            }
            float Engaged = Door.engaged(Lock, Partial);
            int Light = lightAt(World, Origin, Front, Across, Spot[0] / 16, Math.max(0, Spot[1] / 16 - Sunk));
            Pose.pushPose();
            Pose.translate((Spot[0] - 8) / 16.0f, (Spot[1] - 8) / 16.0f, 0.0f);
            Scp076Models.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Scp076Models.get(Scp076Models.LockHousing), Light, PackedOverlay, 1.0f, 1.0f, World, Front);
            Pose.pushPose();
            spin(Pose, Axis.ZP.rotationDegrees((1.0f - Engaged) * -90.0f));
            Scp076Models.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Scp076Models.get(Scp076Models.LockDial), Light, PackedOverlay, 1.0f, 1.0f, World, Front);
            Scp076Models.render(Pose.last(), Buffers.getBuffer(RenderType.cutout()), Scp076Models.get(Scp076Models.glyph(Lock)), LightTexture.FULL_BRIGHT, PackedOverlay, 0.18f + 0.82f * Engaged, 0.0f, World,
                    Front);
            Pose.popPose();
            Pose.pushPose();
            float Turn = Spot[2] == 0 ? 0.0f : Spot[2] == 1 ? 180.0f : -90.0f;
            spin(Pose, Axis.ZP.rotationDegrees(Turn));
            Pose.translate((1.0f - Engaged) * BoltTravel, 0.0f, 0.0f);
            Scp076Models.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Scp076Models.get(Scp076Models.LockBolt), Light, PackedOverlay, 1.0f, 1.0f, World, Front);
            Pose.popPose();
            Pose.popPose();
        }
        if (Drop < 0.75f) {
            float Glow = Door.glow(Partial);
            Pose.translate(1.0f, 1.0f, 0.0f);
            Scp076Models.render(Pose.last(), Buffers.getBuffer(RenderType.cutout()), Scp076Models.get(Scp076Models.DoorGlyph), LightTexture.FULL_BRIGHT, PackedOverlay, 0.15f + 0.85f * Glow, 0.0f, World, Front);
        }
        Pose.popPose();
    }

    private static void spin(PoseStack Pose, Quaternionf Turn) {
        Pose.translate(0.5f, 0.5f, 0.0f);
        Pose.mulPose(Turn);
        Pose.translate(-0.5f, -0.5f, 0.0f);
    }

    private static int lightAt(Level World, BlockPos Origin, Direction Front, Direction Across, int Column, int Row) {
        BlockPos Cell = Origin.relative(Across, Math.max(0, Math.min(Scp076DoorBlock.Columns - 1, Column))).above(Math.max(0, Math.min(Scp076DoorBlock.Rows - 1, Row)))
                .relative(Front);
        return LevelRenderer.getLightColor(World, Cell);
    }

    @Override
    public boolean shouldRenderOffScreen(Scp076DoorBlockEntity Door) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(Scp076DoorBlockEntity Door) {
        return Door.renderBounds();
    }
}
