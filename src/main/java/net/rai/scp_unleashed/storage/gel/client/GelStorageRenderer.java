package net.rai.scp_unleashed.storage.gel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp513.client.Scp513Display;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.StorageLight;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageQuads;
import net.rai.scp_unleashed.storage.gel.GelStatus;
import net.rai.scp_unleashed.storage.gel.GelStorageContainerBlock;
import net.rai.scp_unleashed.storage.gel.GelStorageContainerBlockEntity;

public class GelStorageRenderer implements BlockEntityRenderer<GelStorageContainerBlockEntity> {
    public static final float FillFloor = 4.0f;
    public static final float SwayPeriod = 120.0f;
    public static final int GelSteps = 20;
    public static final float[] SetColour = {1.0f, 0.86f, 0.52f};
    public static final float[] FailColour = {0.62f, 0.58f, 0.5f};
    public static final float SetAlpha = 1.0f;
    public static final float FailAlpha = 0.85f;
    public static final float GlassAlpha = 1.0f;

    public GelStorageRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(GelStorageContainerBlockEntity Locker, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Locker.getLevel();
        if (World == null || !(Locker.getBlockState().getBlock() instanceof GelStorageContainerBlock)) {
            return;
        }
        float Angle = Locker.getDoorAngle(PartialTick);
        int Interior = StorageLight.interior(PackedLight);
        float Share = Math.round(Mth.clamp(Locker.gelShare(), 0.0f, 1.0f) * GelSteps) / (float) GelSteps;
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(StorageGeometry.yaw(Locker.getFacing())));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        ItemStack Held = Locker.peekContents();
        if (!Held.isEmpty()) {
            float Time = (World.getGameTime() % 72000L) + PartialTick;
            float Sway = Share > 0.0f ? Mth.sin(Time / SwayPeriod * Mth.TWO_PI) * Share : 0.0f;
            StorageLayout Layout = Scp513Display.interior(Held, Sway);
            Layout.render(new StorageLayoutContext(Locker, Held, false, Pose, Buffers, Interior, PackedOverlay, PartialTick, World));
        }
        ModelManager Models = Minecraft.getInstance().getModelManager();
        if (Share > 0.0f) {
            boolean Failing = Locker.getStatus() == GelStatus.Failing || Locker.getStatus() == GelStatus.Idle;
            float[] Colour = Failing ? FailColour : SetColour;
            Pose.pushPose();
            float Floor = FillFloor / 16.0f;
            Pose.translate(0.0f, Floor, 0.0f);
            Pose.scale(1.0f, Share, 1.0f);
            Pose.translate(0.0f, -Floor, 0.0f);
            StorageQuads.render(Pose.last(), Buffers.getBuffer(Sheets.translucentCullBlockSheet()), Models.getModel(GelModels.Fill), null, World,
                    Colour[0], Colour[1], Colour[2], Failing ? FailAlpha : SetAlpha, Interior, PackedOverlay, true);
            Pose.popPose();
        }
        float PivotX = StorageGeometry.PivotX / 16.0f;
        float PivotZ = StorageGeometry.PivotZ / 16.0f;
        Pose.pushPose();
        Pose.translate(PivotX, 0.0f, PivotZ);
        Pose.mulPose(Axis.YP.rotationDegrees(-Angle));
        Pose.translate(-PivotX, 0.0f, -PivotZ);
        StorageQuads.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Models.getModel(GelModels.Door), null, World,
                1.0f, 1.0f, 1.0f, 1.0f, PackedLight, PackedOverlay, true);
        StorageQuads.render(Pose.last(), Buffers.getBuffer(Sheets.translucentCullBlockSheet()), Models.getModel(GelModels.Glass), null, World,
                1.0f, 1.0f, 1.0f, GlassAlpha, PackedLight, PackedOverlay, false);
        Pose.popPose();
        Pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(GelStorageContainerBlockEntity Locker) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(GelStorageContainerBlockEntity Locker) {
        return Locker.getRenderBounds();
    }
}
