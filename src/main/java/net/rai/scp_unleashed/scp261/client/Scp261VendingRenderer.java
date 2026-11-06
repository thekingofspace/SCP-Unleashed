package net.rai.scp_unleashed.scp261.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp261.Scp261Odds;
import net.rai.scp_unleashed.scp261.Scp261Registry;
import net.rai.scp_unleashed.scp261.Scp261Snack;
import net.rai.scp_unleashed.scp261.Scp261VendingBlockEntity;

import java.util.Arrays;
import java.util.List;

public class Scp261VendingRenderer implements BlockEntityRenderer<Scp261VendingBlockEntity> {
    public static final ModelResourceLocation CoilModel = ModelResourceLocation.standalone(Scp261Registry.location("block/scp261/coil"));
    public static final List<ModelResourceLocation> ProductModels = Arrays.stream(Scp261Snack.values())
            .map(Treat -> ModelResourceLocation.standalone(Scp261Registry.location("block/scp261/product_" + Treat.getName()))).toList();
    public static final float[] ColumnCentre = {13.5f, 10.5f, 7.5f, 4.5f};
    public static final float[] RowFloor = {20.0f, 16.0f, 12.0f, 8.0f};
    public static final float ProductHeight = 3.0f;
    public static final float ProductFront = 3.1f;
    public static final float ProductDepth = 2.0f;
    public static final float PushDistance = 2.05f;
    public static final float BinFloor = 2.0f;
    public static final float CoilPivot = 8.0f;
    public static final float Swell = 1.04f;
    public static final float RestTilt = 18.0f;

    public Scp261VendingRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float coilAngle(float Age) {
        float Span = Scp261VendingBlockEntity.SpinEnd - Scp261VendingBlockEntity.SpinStart;
        float Fraction = Math.max(0.0f, Math.min(1.0f, (Age - Scp261VendingBlockEntity.SpinStart) / Span));
        return 360.0f * Fraction * Fraction * (3.0f - 2.0f * Fraction);
    }

    public static float push(float Age) {
        float Span = Scp261VendingBlockEntity.SpinEnd - Scp261VendingBlockEntity.SpinStart;
        float Fraction = Math.max(0.0f, Math.min(1.0f, (Age - Scp261VendingBlockEntity.SpinStart) / Span));
        return PushDistance * Fraction * Fraction * (3.0f - 2.0f * Fraction);
    }

    public static float drop(Scp261Snack Treat, float Age) {
        if (Age < Scp261VendingBlockEntity.SpinEnd) {
            return 0.0f;
        }
        float Span = Scp261VendingBlockEntity.FallEnd - Scp261VendingBlockEntity.SpinEnd;
        float Fraction = Math.min(1.0f, (Age - Scp261VendingBlockEntity.SpinEnd) / Span);
        return (RowFloor[Treat.getRow()] - BinFloor) * Fraction * Fraction;
    }

    @Override
    public void render(Scp261VendingBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        if (Machine.getLevel() == null) {
            return;
        }
        float Age = Machine.getShownAge(PartialTick);
        boolean Fresh = Age < Scp261VendingBlockEntity.CycleEnd;
        Scp261Snack First = Fresh ? Scp261Snack.byIndex(Machine.getShownFirst()) : null;
        Scp261Snack Second = Fresh ? Scp261Snack.byIndex(Machine.getShownSecond()) : null;
        Scp261Odds.Outcome Result = Machine.getShownOutcome();
        Pose.pushPose();
        Pose.translate(0.5, 0.0, 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(-facingAngle(Machine.getFront())));
        Pose.translate(-0.5, 0.0, -0.5);
        float Turn = coilAngle(Age);
        for (Scp261Snack Treat : Scp261Snack.values()) {
            boolean Spinning = Treat == First || (Treat == Second && Result == Scp261Odds.Outcome.Pair);
            coil(Pose, Buffers, Treat, Spinning ? Turn : 0.0f);
        }
        if (First != null && Result != Scp261Odds.Outcome.Swallowed && Age >= Scp261VendingBlockEntity.SpinStart && Age < Scp261VendingBlockEntity.PopTick) {
            product(Pose, Buffers, First, Age, 0.0f);
            if (Result == Scp261Odds.Outcome.Pair) {
                product(Pose, Buffers, Second == null ? First : Second, Age, Second == null ? 1.2f : 0.0f);
            }
        }
        Pose.popPose();
    }

    private static void coil(PoseStack Pose, MultiBufferSource Buffers, Scp261Snack Treat, float Angle) {
        float CentreX = ColumnCentre[Treat.getColumn()];
        float CentreY = RowFloor[Treat.getRow()] + ProductHeight * 0.5f;
        Pose.pushPose();
        Pose.translate(CentreX / 16.0f, CentreY / 16.0f, 0.0f);
        if (Angle != 0.0f) {
            Pose.mulPose(Axis.ZP.rotationDegrees(Angle));
        }
        Pose.translate(-CoilPivot / 16.0f, -CoilPivot / 16.0f, 0.0f);
        model(Pose, Buffers, CoilModel, LightTexture.FULL_BRIGHT);
        Pose.popPose();
    }

    private static void product(PoseStack Pose, MultiBufferSource Buffers, Scp261Snack Treat, float Age, float Shift) {
        float CentreX = ColumnCentre[Treat.getColumn()];
        float Floor = RowFloor[Treat.getRow()];
        float CentreY = Floor + ProductHeight * 0.5f;
        float CentreZ = ProductFront + ProductDepth * 0.5f;
        float Drop = drop(Treat, Age);
        Pose.pushPose();
        Pose.translate(Shift / 16.0f, -Drop / 16.0f, -push(Age) / 16.0f);
        Pose.translate(CentreX / 16.0f, CentreY / 16.0f, CentreZ / 16.0f);
        if (Age >= Scp261VendingBlockEntity.FallEnd) {
            Pose.mulPose(Axis.XP.rotationDegrees(RestTilt));
        }
        Pose.scale(Swell, Swell, Swell);
        Pose.translate(-CentreX / 16.0f, -CentreY / 16.0f, -CentreZ / 16.0f);
        model(Pose, Buffers, ProductModels.get(Treat.ordinal()), LightTexture.FULL_BRIGHT);
        Pose.popPose();
    }

    private static void model(PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Location, int Light) {
        Minecraft Game = Minecraft.getInstance();
        BakedModel Baked = Game.getModelManager().getModel(Location);
        ModelBlockRenderer Renderer = Game.getBlockRenderer().getModelRenderer();
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        Renderer.renderModel(Pose.last(), Consumer, null, Baked, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
    }

    private static float facingAngle(Direction Front) {
        return switch (Front) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(Scp261VendingBlockEntity Machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(Scp261VendingBlockEntity Machine) {
        return Machine.getRenderBounds();
    }
}
