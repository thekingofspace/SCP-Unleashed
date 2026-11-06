package net.rai.scp_unleashed.scp294.client;

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
import net.rai.scp_unleashed.scp294.Scp294MachineBlockEntity;
import net.rai.scp_unleashed.scp294.Scp294Registry;

public class Scp294MachineRenderer implements BlockEntityRenderer<Scp294MachineBlockEntity> {
    public static final ModelResourceLocation CupModel = ModelResourceLocation.standalone(Scp294Registry.location("block/scp294/cup"));
    public static final ModelResourceLocation LiquidModel = ModelResourceLocation.standalone(Scp294Registry.location("block/scp294/cup_liquid"));
    public static final ModelResourceLocation StreamModel = ModelResourceLocation.standalone(Scp294Registry.location("block/scp294/stream"));
    public static final float CupScale = 0.36f;
    public static final float CupX = 10.5f;
    public static final float CupZ = 5.4f;
    public static final float Floor = 4.0f;
    public static final float ChuteBottom = 9.0f;
    public static final float DropFrom = 10.0f;
    public static final float CupHeight = 9.0f;
    public static final float LiquidFloor = 5.0f;
    public static final float LiquidTop = 8.5f;
    public static final float StreamWidth = 0.6f;
    public static final int MinimumGlow = 9;

    public Scp294MachineRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float cupBottom(float Age) {
        float Rest = Floor;
        float Start = DropFrom - CupHeight * CupScale;
        if (Age <= Scp294MachineBlockEntity.DropStart) {
            return Start;
        }
        if (Age >= Scp294MachineBlockEntity.DropEnd) {
            float Settle = Math.max(0.0f, 1.0f - (Age - Scp294MachineBlockEntity.DropEnd) / 3.0f);
            return Rest + 0.25f * Settle * Settle;
        }
        float Fraction = (Age - Scp294MachineBlockEntity.DropStart) / (Scp294MachineBlockEntity.DropEnd - Scp294MachineBlockEntity.DropStart);
        return Start + (Rest - Start) * Fraction * Fraction;
    }

    public static float fill(float Age) {
        float Span = Scp294MachineBlockEntity.PourEnd - Scp294MachineBlockEntity.PourStart;
        return Math.max(0.0f, Math.min(1.0f, (Age - Scp294MachineBlockEntity.PourStart - 2.0f) / Span));
    }

    public static boolean pouring(float Age) {
        return Age >= Scp294MachineBlockEntity.PourStart && Age < Scp294MachineBlockEntity.PourEnd;
    }

    @Override
    public void render(Scp294MachineBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        if (Machine.getLevel() == null || !Machine.isBusy()) {
            return;
        }
        float Age = Machine.getAge(PartialTick);
        if (Age < Scp294MachineBlockEntity.DropStart || Age >= Scp294MachineBlockEntity.PopTick) {
            return;
        }
        int Light = LightTexture.pack(Math.max(MinimumGlow, LightTexture.block(PackedLight)), LightTexture.sky(PackedLight));
        int Colour = Machine.getColour();
        float Red = ((Colour >> 16) & 255) / 255.0f;
        float Green = ((Colour >> 8) & 255) / 255.0f;
        float Blue = (Colour & 255) / 255.0f;
        Pose.pushPose();
        Pose.translate(0.5, 0.0, 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(-facingAngle(Machine.getFront())));
        Pose.translate(-0.5, 0.0, -0.5);
        float Bottom = cupBottom(Age);
        Pose.pushPose();
        Pose.translate(CupX / 16.0f, Bottom / 16.0f, CupZ / 16.0f);
        Pose.scale(CupScale, CupScale, CupScale);
        Pose.translate(-0.5f, 0.0f, -0.5f);
        model(Pose, Buffers, CupModel, Light, 1.0f, 1.0f, 1.0f);
        float Filled = fill(Age);
        if (Filled > 0.0f) {
            Pose.pushPose();
            Pose.translate(0.0f, LiquidFloor / 16.0f, 0.0f);
            Pose.scale(1.0f, Math.max(0.04f, Filled), 1.0f);
            Pose.translate(0.0f, -LiquidFloor / 16.0f, 0.0f);
            model(Pose, Buffers, LiquidModel, Light, Red, Green, Blue);
            Pose.popPose();
        }
        Pose.popPose();
        if (pouring(Age)) {
            float Surface = Bottom + (LiquidFloor + (LiquidTop - LiquidFloor) * Filled) * CupScale;
            float Length = Math.max(0.2f, ChuteBottom - Surface);
            Pose.pushPose();
            Pose.translate(CupX / 16.0f, Surface / 16.0f, CupZ / 16.0f);
            Pose.scale(StreamWidth, Length / 16.0f, StreamWidth);
            Pose.translate(-0.5f, 0.0f, -0.5f);
            model(Pose, Buffers, StreamModel, Light, Red, Green, Blue);
            Pose.popPose();
        }
        Pose.popPose();
    }

    private static void model(PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Location, int Light, float Red, float Green, float Blue) {
        Minecraft Game = Minecraft.getInstance();
        BakedModel Baked = Game.getModelManager().getModel(Location);
        ModelBlockRenderer Renderer = Game.getBlockRenderer().getModelRenderer();
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        Renderer.renderModel(Pose.last(), Consumer, null, Baked, Red, Green, Blue, Light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
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
    public boolean shouldRenderOffScreen(Scp294MachineBlockEntity Machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(Scp294MachineBlockEntity Machine) {
        return Machine.getRenderBounds();
    }
}
