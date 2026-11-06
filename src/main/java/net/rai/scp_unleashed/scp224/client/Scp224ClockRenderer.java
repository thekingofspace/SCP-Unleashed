package net.rai.scp_unleashed.scp224.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp224.Scp224ClockBlockEntity;
import net.rai.scp_unleashed.scp224.Scp224Motion;

import java.util.List;

public class Scp224ClockRenderer implements BlockEntityRenderer<Scp224ClockBlockEntity> {
    public static final ResourceLocation Pendulum = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp224/pendulum");
    public static final ResourceLocation HourHand = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp224/hour_hand");
    public static final ResourceLocation MinuteHand = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp224/minute_hand");
    public static final List<ResourceLocation> Models = List.of(Pendulum, HourHand, MinuteHand);

    public Scp224ClockRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp224ClockBlockEntity Clock, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Clock.getLevel();
        if (World == null) {
            return;
        }
        Direction Facing = Clock.facing();
        boolean Still = Clock.isStilled();
        double Time = World.getGameTime() + (double) PartialTick;
        double DayTime = Still ? Clock.getFrozenTime() : World.getDayTime() + (double) PartialTick;
        float Swing = Still ? 0.0f : Scp224Motion.swing(Time);
        BlockPos Upper = Clock.getBlockPos().above();
        int UpperLight = LevelRenderer.getLightColor(World, Upper);
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Facing.toYRot()));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        part(Pose, Consumer, Pendulum, Scp224Motion.PivotX, Scp224Motion.PivotY, Swing, PackedLight);
        part(Pose, Consumer, HourHand, Scp224Motion.DialX, Scp224Motion.DialY, Scp224Motion.hourAngle(DayTime), UpperLight);
        part(Pose, Consumer, MinuteHand, Scp224Motion.DialX, Scp224Motion.DialY, Scp224Motion.minuteAngle(DayTime), UpperLight);
        Pose.popPose();
    }

    private static void part(PoseStack Pose, VertexConsumer Consumer, ResourceLocation Location, float PivotX, float PivotY, float Angle, int Light) {
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(Location));
        ModelBlockRenderer Renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        Pose.pushPose();
        Pose.translate(PivotX / 16.0f, PivotY / 16.0f, 0.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(Angle));
        Pose.translate(-PivotX / 16.0f, -PivotY / 16.0f, 0.0f);
        Renderer.renderModel(Pose.last(), Consumer, null, Model, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        Pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(Scp224ClockBlockEntity Clock) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(Scp224ClockBlockEntity Clock) {
        BlockPos Pos = Clock.getBlockPos();
        return new AABB(Pos.getX(), Pos.getY(), Pos.getZ(), Pos.getX() + 1.0, Pos.getY() + 2.0, Pos.getZ() + 1.0);
    }
}
