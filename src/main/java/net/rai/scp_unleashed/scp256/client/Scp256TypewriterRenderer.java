package net.rai.scp_unleashed.scp256.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp256.Scp256Registry;
import net.rai.scp_unleashed.scp256.Scp256Schedule;
import net.rai.scp_unleashed.scp256.Scp256TypewriterBlock;
import net.rai.scp_unleashed.scp256.Scp256TypewriterBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.List;

public class Scp256TypewriterRenderer implements BlockEntityRenderer<Scp256TypewriterBlockEntity> {
    public static final ResourceLocation Carriage = Scp256Registry.location("block/scp256/typewriter_carriage");
    public static final ResourceLocation Key = Scp256Registry.location("block/scp256/typewriter_key");
    public static final ResourceLocation Space = Scp256Registry.location("block/scp256/typewriter_space");
    public static final ResourceLocation Typebar = Scp256Registry.location("block/scp256/typewriter_typebar");
    public static final List<ResourceLocation> Models = List.of(Carriage, Key, Space, Typebar);
    public static final float[][] Keys = {
            {3.0f, 2.75f, 4.75f}, {5.5f, 2.75f, 4.75f}, {8.0f, 2.75f, 4.75f}, {10.5f, 2.75f, 4.75f}, {13.0f, 2.75f, 4.75f},
            {4.25f, 3.75f, 7.25f}, {6.75f, 3.75f, 7.25f}, {9.25f, 3.75f, 7.25f}, {11.75f, 3.75f, 7.25f},
            {3.0f, 4.75f, 9.75f}, {5.5f, 4.75f, 9.75f}, {8.0f, 4.75f, 9.75f}, {10.5f, 4.75f, 9.75f}, {13.0f, 4.75f, 9.75f}};
    public static final float PressDepth = 0.5f;
    public static final float PivotX = 8.0f;
    public static final float PivotY = 11.5f;
    public static final float PivotZ = 13.25f;
    public static final float PaperLean = 22.5f;
    public static final float PaperHeight = 6.0f;
    public static final float TextMargin = 5.0f;
    public static final float TextBottom = 0.55f;
    public static final float GlyphTop = 0.8f;
    public static final float LineStep = 0.9f;
    public static final float TopClearance = 0.15f;
    public static final float InkLift = 0.02f;
    public static final float[] TypebarPivot = {8.0f, 4.25f, 11.5f};
    public static final int Ink = 0xFF2A2522;
    public static final int InkWorn = 0xFF4E4741;

    public Scp256TypewriterRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp256TypewriterBlockEntity Typewriter, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Typewriter.getLevel();
        if (World == null) {
            return;
        }
        Font Text = Minecraft.getInstance().font;
        Scp256Sheet.Frame Shown = Scp256Sheet.frame(Text, Typewriter, Typewriter.elapsed(World, PartialTick));
        Direction Facing = Typewriter.getBlockState().getValue(Scp256TypewriterBlock.FACING);
        Pose.pushPose();
        Pose.translate(0.5, 0.0, 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(-angle(Facing)));
        Pose.translate(-0.5, 0.0, -0.5);
        Pose.pushPose();
        Pose.translate(Shown.Offset() / 16.0f, 0.0f, 0.0f);
        draw(Pose, Buffers, Carriage, PackedLight);
        if (!ShaderCompat.renderingShadowPass()) {
            sheet(Text, Pose, Buffers, Shown, Typewriter.getSeed(), PackedLight);
        }
        Pose.popPose();
        for (int Index = 0; Index < Keys.length; Index++) {
            float Drop = Index == Shown.Key() ? Shown.KeyDepth() * PressDepth : 0.0f;
            Pose.pushPose();
            Pose.translate(Keys[Index][0] / 16.0f, (Keys[Index][1] - Drop) / 16.0f, Keys[Index][2] / 16.0f);
            draw(Pose, Buffers, Key, PackedLight);
            Pose.popPose();
        }
        Pose.pushPose();
        Pose.translate(0.0f, -Shown.SpaceDepth() * PressDepth / 16.0f, 0.0f);
        draw(Pose, Buffers, Space, PackedLight);
        Pose.popPose();
        if (Shown.Striking()) {
            Pose.pushPose();
            Pose.translate(TypebarPivot[0] / 16.0f, TypebarPivot[1] / 16.0f, TypebarPivot[2] / 16.0f);
            Pose.mulPose(Axis.XP.rotationDegrees(Shown.Typebar()));
            draw(Pose, Buffers, Typebar, PackedLight);
            Pose.popPose();
        }
        Pose.popPose();
    }

    private static void draw(PoseStack Pose, MultiBufferSource Buffers, ResourceLocation Location, int PackedLight) {
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(Location));
        ModelBlockRenderer Renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        Renderer.renderModel(Pose.last(), Buffers.getBuffer(Sheets.cutoutBlockSheet()), null, Model, 1.0f, 1.0f, 1.0f, PackedLight, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
    }

    private static void sheet(Font Text, PoseStack Pose, MultiBufferSource Buffers, Scp256Sheet.Frame Shown, int Seed, int PackedLight) {
        if (Shown.Rows().isEmpty()) {
            return;
        }
        Pose.pushPose();
        Pose.translate(PivotX / 16.0f, PivotY / 16.0f, PivotZ / 16.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(PaperLean));
        for (Scp256Sheet.Row Line : Shown.Rows()) {
            float Top = TextBottom + Line.Slot() * LineStep + GlyphTop;
            if (Line.Slot() < -0.01f || Top > PaperHeight - TopClearance) {
                continue;
            }
            Pose.pushPose();
            Pose.translate(TextMargin / 16.0f, Top / 16.0f, -InkLift / 16.0f);
            Pose.mulPose(Axis.YP.rotationDegrees(180.0f));
            float Scale = Scp256Sheet.TextScale / 16.0f;
            Pose.scale(Scale, -Scale, Scale);
            ink(Text, Pose, Buffers, Line.Text(), Seed + Math.round(Line.Slot() * 31.0f), PackedLight);
            Pose.popPose();
        }
        Pose.popPose();
    }

    private static void ink(Font Text, PoseStack Pose, MultiBufferSource Buffers, String Line, int Seed, int PackedLight) {
        float Cursor = 0.0f;
        for (int Index = 0; Index < Line.length(); Index++) {
            String Glyph = String.valueOf(Line.charAt(Index));
            int Colour = Scp256Schedule.mix(Seed, Index) % 5 == 0 ? InkWorn : Ink;
            Text.drawInBatch(Glyph, Cursor, 0.0f, Colour, false, Pose.last().pose(), Buffers, Font.DisplayMode.POLYGON_OFFSET, 0, PackedLight);
            Cursor += Text.width(Glyph);
        }
    }

    public static float angle(Direction Facing) {
        return switch (Facing) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(Scp256TypewriterBlockEntity Typewriter) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(Scp256TypewriterBlockEntity Typewriter) {
        return Typewriter.getRenderBounds();
    }
}
