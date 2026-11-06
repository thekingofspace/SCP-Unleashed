package net.rai.scp_unleashed.scp149.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp149.Scp149CocoonBlock;
import net.rai.scp_unleashed.scp149.Scp149CocoonBlockEntity;

public class Scp149CocoonRenderer implements BlockEntityRenderer<Scp149CocoonBlockEntity> {
    public static final float[] Swell = {0.035f, 0.045f, 0.06f, 0.08f};
    public static final float Across = 0.65f;
    public static final float SplitWobble = 2.5f;
    public static final float CrowdSwell = 0.03f;
    public static final float CrowdRise = 0.01f;
    public static final int CrowdStart = 3;
    public static final int CrowdMax = 6;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public Scp149CocoonRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp149CocoonBlockEntity Cocoon, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Cocoon.getLevel();
        BlockState State = Cocoon.getBlockState();
        if (World == null || !(State.getBlock() instanceof Scp149CocoonBlock)) {
            return;
        }
        int Stage = State.getValue(Scp149CocoonBlock.Stage);
        float Beat = Cocoon.pulse(PartialTick);
        int Extra = Mth.clamp(Cocoon.waiting() - CrowdStart, 0, CrowdMax - CrowdStart);
        float Long = (1.0f + Swell[Stage] * Beat) * (1.0f + CrowdRise * Extra);
        float Wide = (1.0f + Swell[Stage] * Across * Beat) * (1.0f + CrowdSwell * Extra);
        BakedModel Model = Minecraft.getInstance().getBlockRenderer().getBlockModel(State);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        if (Stage == Scp149CocoonBlock.MaxStage) {
            float Seconds = (World.getGameTime() + PartialTick) / 20.0f;
            Pose.mulPose(Axis.ZP.rotationDegrees(SplitWobble * Beat * Mth.sin(Seconds * 9.0f)));
        }
        Pose.scale(Wide, Long, Wide);
        Pose.translate(-0.5f, 0.0f, -0.5f);
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(State, Side, Random, ModelData.EMPTY, null)) {
                float Shade = World.getShade(Quad.getDirection(), Quad.isShade());
                Consumer.putBulkData(Pose.last(), Quad, Shade, Shade, Shade, 1.0f, PackedLight, PackedOverlay);
            }
        }
        Pose.popPose();
    }
}
