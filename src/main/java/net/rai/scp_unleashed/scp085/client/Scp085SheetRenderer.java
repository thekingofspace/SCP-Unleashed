package net.rai.scp_unleashed.scp085.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.scp085.Scp085Scene;
import net.rai.scp_unleashed.scp085.Scp085SheetBlock;
import net.rai.scp_unleashed.scp085.Scp085SheetBlockEntity;

import java.util.List;

public class Scp085SheetRenderer implements BlockEntityRenderer<Scp085SheetBlockEntity> {
    public static final ResourceLocation FigureTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp085/figure");
    public static final float PaperFront = 15.75f;
    public static final float TexelsPerPixel = 4.0f;

    public Scp085SheetRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp085SheetBlockEntity Sheet, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Sheet.getLevel();
        BlockState State = Sheet.getBlockState();
        if (World == null || !Sheet.isHere() || !(State.getBlock() instanceof Scp085SheetBlock)) {
            return;
        }
        List<Scp085Poser.Placed> Parts = Scp085Poser.pose(Sheet, World.getGameTime(), PartialTick);
        if (Parts.isEmpty()) {
            return;
        }
        Direction Front = State.getValue(Scp085SheetBlock.Facing);
        TextureAtlasSprite Sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FigureTexture);
        float Shade = World.getShade(Front, true);
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.cutout());
        Pose.pushPose();
        Pose.translate(0.5f, 0.5f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * PropShapes.quarterTurns(Front)));
        Pose.translate(-0.5f, -0.5f, -0.5f);
        PoseStack.Pose Last = Pose.last();
        for (Scp085Poser.Placed Part : Parts) {
            quad(Last, Consumer, Sprite, Part, Shade, PackedLight);
        }
        Pose.popPose();
    }

    private static void quad(PoseStack.Pose Last, VertexConsumer Consumer, TextureAtlasSprite Sprite, Scp085Poser.Placed Part, float Shade, int Light) {
        int Left = Part.left();
        int Top = Part.top();
        int ClipLeft = Math.max(Left, Scp085Scene.SheetLeft);
        int ClipTop = Math.max(Top, Scp085Scene.SheetTop);
        int ClipRight = Math.min(Left + Scp085Clip.Cell, Scp085Scene.SheetRight);
        int ClipBottom = Math.min(Top + Scp085Clip.Cell, Scp085Scene.SheetBottom);
        if (ClipLeft >= ClipRight || ClipTop >= ClipBottom) {
            return;
        }
        int Cell = Part.Clip().cell(Part.Frame());
        float CellU = (Cell % Scp085Clip.Columns) * Scp085Clip.Cell;
        float CellV = (Cell / Scp085Clip.Columns) * Scp085Clip.Cell;
        float LocalLeft = ClipLeft - Left;
        float LocalRight = ClipRight - Left;
        if (Part.Mirror()) {
            LocalLeft = Scp085Clip.Cell - LocalLeft;
            LocalRight = Scp085Clip.Cell - LocalRight;
        }
        float U0 = Sprite.getU((CellU + LocalLeft) / Scp085Clip.AtlasWidth);
        float U1 = Sprite.getU((CellU + LocalRight) / Scp085Clip.AtlasWidth);
        float V0 = Sprite.getV((CellV + ClipTop - Top) / Scp085Clip.AtlasHeight);
        float V1 = Sprite.getV((CellV + ClipBottom - Top) / Scp085Clip.AtlasHeight);
        float X0 = modelX(ClipLeft);
        float X1 = modelX(ClipRight);
        float Y0 = modelY(ClipTop);
        float Y1 = modelY(ClipBottom);
        float Z = (PaperFront - Part.Depth()) / 16.0f;
        vertex(Last, Consumer, X0, Y0, Z, U0, V0, Shade, Light);
        vertex(Last, Consumer, X0, Y1, Z, U0, V1, Shade, Light);
        vertex(Last, Consumer, X1, Y1, Z, U1, V1, Shade, Light);
        vertex(Last, Consumer, X1, Y0, Z, U1, V0, Shade, Light);
    }

    private static float modelX(int Texel) {
        return (16.0f - Texel / TexelsPerPixel) / 16.0f;
    }

    private static float modelY(int Texel) {
        return (16.0f - Texel / TexelsPerPixel) / 16.0f;
    }

    private static void vertex(PoseStack.Pose Last, VertexConsumer Consumer, float X, float Y, float Z, float U, float V, float Shade, int Light) {
        Consumer.addVertex(Last, X, Y, Z).setColor(Shade, Shade, Shade, 1.0f).setUv(U, V).setLight(Light).setNormal(Last, 0.0f, 0.0f, -1.0f);
    }
}
