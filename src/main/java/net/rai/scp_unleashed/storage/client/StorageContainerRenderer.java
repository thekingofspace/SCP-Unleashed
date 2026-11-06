package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageEnvironment;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.StorageLight;

public class StorageContainerRenderer implements BlockEntityRenderer<StorageContainerBlockEntity> {
    public static final int ClearGlass = 0xFFFFFFFF;
    public static final int TintedGlass = 0xFF4A4452;

    public StorageContainerRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(StorageContainerBlockEntity Locker, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Locker.getLevel();
        BlockState State = Locker.getBlockState();
        if (World == null || !(State.getBlock() instanceof StorageContainerBlock)) {
            return;
        }
        boolean Tall = Locker.isTall();
        float Angle = Locker.getDoorAngle(PartialTick);
        boolean Glazed = Locker.hasGlassFront();
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(StorageGeometry.yaw(Locker.getFacing())));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        if (Locker.isInteriorShown()) {
            ItemStack Held = Locker.peekContents();
            StorageLayout Layout = StorageLayouts.forStack(Held, Tall);
            Layout.render(new StorageLayoutContext(Locker, Held, Tall, Pose, Buffers, interiorLight(PackedLight), PackedOverlay, PartialTick, World));
        }
        ModelManager Models = Minecraft.getInstance().getModelManager();
        float PivotX = StorageGeometry.PivotX / 16.0f;
        float PivotZ = StorageGeometry.PivotZ / 16.0f;
        Pose.pushPose();
        Pose.translate(PivotX, 0.0f, PivotZ);
        Pose.mulPose(Axis.YP.rotationDegrees(-Angle));
        Pose.translate(-PivotX, 0.0f, -PivotZ);
        StorageQuads.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Models.getModel(StorageModels.door(Tall, Glazed)), null, World,
                1.0f, 1.0f, 1.0f, 1.0f, PackedLight, PackedOverlay, true);
        if (Glazed) {
            int Tint = glassTint(Locker.peekGlass());
            boolean Frosted = Locker.getEnvironment() == StorageEnvironment.Cold;
            StorageQuads.render(Pose.last(), Buffers.getBuffer(Sheets.translucentCullBlockSheet()), Models.getModel(StorageModels.glass(Tall, Frosted)), null, World,
                    (Tint >> 16 & 255) / 255.0f, (Tint >> 8 & 255) / 255.0f, (Tint & 255) / 255.0f, 1.0f, PackedLight, PackedOverlay, false);
        }
        Pose.popPose();
        Pose.popPose();
    }

    public static int interiorLight(int Packed) {
        return StorageLight.interior(Packed);
    }

    public static int glassTint(ItemStack Glass) {
        if (Glass.getItem() instanceof BlockItem Placed) {
            if (Placed.getBlock() instanceof BeaconBeamBlock Stained) {
                return Stained.getColor().getTextureDiffuseColor() | 0xFF000000;
            }
            if (Placed.getBlock() == Blocks.TINTED_GLASS) {
                return TintedGlass;
            }
        }
        return ClearGlass;
    }

    @Override
    public boolean shouldRenderOffScreen(StorageContainerBlockEntity Locker) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(StorageContainerBlockEntity Locker) {
        return Locker.getRenderBounds();
    }
}
