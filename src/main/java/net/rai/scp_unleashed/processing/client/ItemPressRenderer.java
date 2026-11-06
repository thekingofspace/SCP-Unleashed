package net.rai.scp_unleashed.processing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.processing.ItemPressBlockEntity;
import net.rai.scp_unleashed.processing.PressDie;
import net.rai.scp_unleashed.processing.ProcessingMachineBlock;

public class ItemPressRenderer implements BlockEntityRenderer<ItemPressBlockEntity> {
    public static final ModelResourceLocation RamModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/item_press_ram"));
    public static final ModelResourceLocation RodRamModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/item_press_ram_rod"));
    public static final float Pixel = 1.0f / 16.0f;
    public static final float Travel = 4.75f * Pixel;
    public static final float DieTop = 4.5f * Pixel;
    public static final float PieceScale = 0.375f;
    public static final float PieceLift = 0.25f * Pixel;
    public static final long ModelSeed = 42L;

    private final BlockRenderDispatcher BlockDispatcher;
    private final ItemRenderer ItemPainter;

    public ItemPressRenderer(BlockEntityRendererProvider.Context Context) {
        BlockDispatcher = Context.getBlockRenderDispatcher();
        ItemPainter = Context.getItemRenderer();
    }

    public static float facingTurn(BlockState State) {
        Direction Front = State.hasProperty(ProcessingMachineBlock.Facing) ? State.getValue(ProcessingMachineBlock.Facing) : Direction.NORTH;
        return 180.0f - Front.toYRot();
    }

    @Override
    public void render(ItemPressBlockEntity Press, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Press.getLevel();
        if (World == null) {
            return;
        }
        BlockState State = Press.getBlockState();
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(facingTurn(State)));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        renderPiece(Press, World, Pose, Buffers, PackedLight, PackedOverlay);
        Pose.translate(0.0f, -Press.getHeadDrop(PartialTick) * Travel, 0.0f);
        BakedModel Ram = Minecraft.getInstance().getModelManager().getModel(Press.getDie() == PressDie.Rod ? RodRamModel : RamModel);
        for (RenderType Type : Ram.getRenderTypes(State, RandomSource.create(ModelSeed), ModelData.EMPTY)) {
            BlockDispatcher.getModelRenderer().tesselateBlock(World, Ram, State, Press.getBlockPos(), Pose, Buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(Type)),
                    false, RandomSource.create(), ModelSeed, PackedOverlay, ModelData.EMPTY, Type);
        }
        Pose.popPose();
    }

    private void renderPiece(ItemPressBlockEntity Press, Level World, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        ItemStack Piece = Press.getShownPiece();
        if (Piece.isEmpty()) {
            return;
        }
        Pose.pushPose();
        Pose.translate(0.5f, DieTop + PieceLift, 0.5f);
        Pose.mulPose(Axis.XP.rotationDegrees(90.0f));
        Pose.scale(PieceScale, PieceScale, PieceScale);
        ItemPainter.renderStatic(Piece, ItemDisplayContext.FIXED, PackedLight, PackedOverlay, Pose, Buffers, World, (int) Press.getBlockPos().asLong());
        Pose.popPose();
    }
}
