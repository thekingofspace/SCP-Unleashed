package net.rai.scp_unleashed.scpsign.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scpsign.ScpSignBlock;
import net.rai.scp_unleashed.scpsign.ScpSignBlockEntity;
import net.rai.scp_unleashed.scpsign.ScpSignKind;
import org.joml.Matrix4f;

public class ScpSignRenderer implements BlockEntityRenderer<ScpSignBlockEntity> {
    public static final double DetailRange = 32.0;
    public static final int ViewRange = 64;
    public static final int BacklightLevel = 13;
    private static final float Surface = 15.0f / 16.0f - 0.5f - 0.006f;
    private static final float LayerStep = 0.55f;
    private static final float TextLayer = 3.0f;

    private final BlockEntityRenderDispatcher Dispatcher;
    private final Font TextFont;

    public ScpSignRenderer(BlockEntityRendererProvider.Context Context) {
        Dispatcher = Context.getBlockEntityRenderDispatcher();
        TextFont = Context.getFont();
    }

    @Override
    public void render(ScpSignBlockEntity Sign, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        BlockState State = Sign.getBlockState();
        if (!(State.getBlock() instanceof ScpSignBlock Owner)) {
            return;
        }
        ScpSignKind Kind = Owner.getKind();
        ScpSignLayout Layout = ScpSignLayouts.get(Sign.getDocument(), Kind, TextFont);
        boolean Detail = Dispatcher.camera == null || Vec3.atCenterOf(Sign.getBlockPos()).distanceToSqr(Dispatcher.camera.getPosition()) < DetailRange * DetailRange;
        int Light = State.getValue(ScpSignBlock.Lit) ? LightTexture.pack(Math.max(LightTexture.block(PackedLight), BacklightLevel), LightTexture.sky(PackedLight)) : PackedLight;
        Pose.pushPose();
        Pose.translate(0.5f, 0.5f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-State.getValue(HorizontalDirectionalBlock.FACING).toYRot()));
        Pose.translate(ScpSignKind.Left / 16.0f - 0.5f, Kind.getTop() / 16.0f - 0.5f, -Surface);
        Pose.scale(ScpSignKind.Unit, -ScpSignKind.Unit, ScpSignKind.Unit);
        Matrix4f Matrix = Pose.last().pose();
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.text(ScpSignLayouts.Sheet));
        for (ScpSignLayout.Quad Shape : Layout.SheetQuads()) {
            quad(Consumer, Matrix, Shape, Light);
        }
        if (Layout.Photo() != null && Layout.PhotoQuad() != null) {
            quad(Buffers.getBuffer(RenderType.text(Layout.Photo())), Matrix, Layout.PhotoQuad(), Light);
        }
        if (Detail) {
            for (ScpSignLayout.Line Text : Layout.Lines()) {
                Pose.pushPose();
                Pose.translate(Text.X(), Text.Y(), TextLayer * LayerStep);
                Pose.scale(Text.Scale(), Text.Scale(), 1.0f);
                TextFont.drawInBatch(Text.Text(), 0.0f, 0.0f, 0xFFFFFFFF, false, Pose.last().pose(), Buffers, Font.DisplayMode.POLYGON_OFFSET, 0, Light);
                Pose.popPose();
            }
        }
        Pose.popPose();
    }

    private static void quad(VertexConsumer Consumer, Matrix4f Matrix, ScpSignLayout.Quad Shape, int Light) {
        float Depth = Shape.Layer() * LayerStep;
        Consumer.addVertex(Matrix, Shape.X0(), Shape.Y0(), Depth).setColor(Shape.Colour()).setUv(Shape.U0(), Shape.V0()).setLight(Light);
        Consumer.addVertex(Matrix, Shape.X0(), Shape.Y1(), Depth).setColor(Shape.Colour()).setUv(Shape.U0(), Shape.V1()).setLight(Light);
        Consumer.addVertex(Matrix, Shape.X1(), Shape.Y1(), Depth).setColor(Shape.Colour()).setUv(Shape.U1(), Shape.V1()).setLight(Light);
        Consumer.addVertex(Matrix, Shape.X1(), Shape.Y0(), Depth).setColor(Shape.Colour()).setUv(Shape.U1(), Shape.V0()).setLight(Light);
    }

    @Override
    public int getViewDistance() {
        return ViewRange;
    }

    @Override
    public AABB getRenderBoundingBox(ScpSignBlockEntity Sign) {
        return Sign.getRenderBounds();
    }
}
