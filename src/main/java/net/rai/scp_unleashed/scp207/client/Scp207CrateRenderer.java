package net.rai.scp_unleashed.scp207.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp207.Scp207CrateBlock;
import net.rai.scp_unleashed.scp207.Scp207CrateBlockEntity;
import net.rai.scp_unleashed.scp207.Scp207Ration;
import net.rai.scp_unleashed.scp207.Scp207Registry;

public class Scp207CrateRenderer implements BlockEntityRenderer<Scp207CrateBlockEntity> {
    public static final ModelResourceLocation BottleModel = ModelResourceLocation.standalone(Scp207Registry.location("block/scp207/crate_bottle"));
    public static final int[] TakeOrder = {5, 10, 0, 15, 3, 12, 9, 6, 1, 14, 11, 4, 2, 13, 7, 8};
    public static final int Columns = 4;
    public static final float FirstCell = 2.0f;
    public static final float CellPitch = 4.0f;
    public static final float FloorY = 1.0f;

    public Scp207CrateRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp207CrateBlockEntity Crate, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Crate.getLevel();
        BlockState State = Crate.getBlockState();
        if (World == null || !(State.getBlock() instanceof Scp207CrateBlock)) {
            return;
        }
        int Left = Crate.leftOn(Scp207Ration.dayOf(World.getDayTime()));
        if (Left <= 0) {
            return;
        }
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(BottleModel);
        ModelBlockRenderer Renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        RenderType Type = Sheets.translucentCullBlockSheet();
        VertexConsumer Consumer = Buffers.getBuffer(Type);
        Direction Front = State.getValue(Scp207CrateBlock.Facing);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-(Front.toYRot() + 180.0f)));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        for (int Index = Scp207Ration.Daily - Left; Index < Scp207Ration.Daily; Index++) {
            int Slot = TakeOrder[Index % TakeOrder.length];
            float X = FirstCell + (Slot % Columns) * CellPitch;
            float Z = FirstCell + (Slot / Columns) * CellPitch;
            Pose.pushPose();
            Pose.translate((X - 8.0f) / 16.0f, FloorY / 16.0f, (Z - 8.0f) / 16.0f);
            Renderer.renderModel(Pose.last(), Consumer, null, Model, 1.0f, 1.0f, 1.0f, PackedLight, PackedOverlay, ModelData.EMPTY, Type);
            Pose.popPose();
        }
        Pose.popPose();
    }
}
