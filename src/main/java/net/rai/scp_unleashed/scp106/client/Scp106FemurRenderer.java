package net.rai.scp_unleashed.scp106.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp106.Scp106FemurBreakerBlockEntity;
import net.rai.scp_unleashed.scp106.Scp106FemurLayout;

public class Scp106FemurRenderer implements BlockEntityRenderer<Scp106FemurBreakerBlockEntity> {
    public static final ModelResourceLocation ArmModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp106/femur_breaker_arm"));
    public static final ModelResourceLocation LeverModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp106/femur_breaker_lever"));
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public Scp106FemurRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp106FemurBreakerBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Machine.getLevel();
        if (World == null) {
            return;
        }
        Direction Front = Machine.getFront();
        int Turns = (Front.get2DDataValue() + 2) % 4;
        int Light = LevelRenderer.getLightColor(World, Machine.getBlockPos().above());
        Minecraft Client = Minecraft.getInstance();
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * Turns));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.pushPose();
        Pose.translate(Scp106FemurLayout.PivotX / 16.0f, Scp106FemurLayout.PivotY / 16.0f, Scp106FemurLayout.PivotZ / 16.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(Machine.armAngle(PartialTick)));
        draw(Pose, Buffers, Client.getModelManager().getModel(ArmModel), World, Light, PackedOverlay);
        Pose.popPose();
        Pose.pushPose();
        Pose.translate(Scp106FemurLayout.LeverX / 16.0f, Scp106FemurLayout.LeverY / 16.0f, (Scp106FemurLayout.LeverZ + 5.0f) / 16.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(-Machine.leverAngle(PartialTick)));
        draw(Pose, Buffers, Client.getModelManager().getModel(LeverModel), World, Light, PackedOverlay);
        Pose.popPose();
        Pose.popPose();
    }

    private void draw(PoseStack Pose, MultiBufferSource Buffers, BakedModel Model, Level World, int Light, int Overlay) {
        PoseStack.Pose Last = Pose.last();
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = World.getShade(Quad.getDirection(), Quad.isShade());
                Buffers.getBuffer(RenderType.solid()).putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, Light, Overlay);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(Scp106FemurBreakerBlockEntity Machine) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(Scp106FemurBreakerBlockEntity Machine) {
        return Machine.getRenderBounds();
    }
}
