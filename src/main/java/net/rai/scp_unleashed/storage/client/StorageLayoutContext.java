package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageEnvironment;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.StorageLight;

public record StorageLayoutContext(StorageContainerBlockEntity Locker, ItemStack Stack, boolean Tall, PoseStack Pose, MultiBufferSource Buffers,
                                   int Light, int Overlay, float PartialTick, Level World) {
    public static final long TimeWrap = 72000L;
    public static final float PixelHalf = 8.0f;
    private static StorageCanvas Canvas;

    public static StorageCanvas canvas() {
        return Canvas != null ? Canvas : MinecraftStorageCanvas.Instance;
    }

    public static void useCanvas(StorageCanvas Wanted) {
        Canvas = Wanted;
    }

    public boolean isEmpty() {
        return Stack.isEmpty();
    }

    public float floor() {
        return StorageGeometry.CavityFloor;
    }

    public float ceiling() {
        return StorageGeometry.cavityTop(Tall);
    }

    public float left() {
        return StorageGeometry.CavityLeft;
    }

    public float right() {
        return StorageGeometry.CavityRight;
    }

    public float front() {
        return StorageGeometry.CavityFront;
    }

    public float back() {
        return StorageGeometry.CavityBack;
    }

    public float centreX() {
        return StorageGeometry.CentreX;
    }

    public float centreZ() {
        return StorageGeometry.CentreZ;
    }

    public float time() {
        return World.getGameTime() % TimeWrap + PartialTick;
    }

    public long seed() {
        return Locker.getBlockPos().asLong();
    }

    public float doorAngle() {
        return Locker.getDoorAngle(PartialTick);
    }

    public StorageEnvironment environment() {
        return Locker.getEnvironment();
    }

    public int blockLight() {
        return StorageLight.block(Light);
    }

    public StorageLayoutContext withLight(int BlockLight) {
        return new StorageLayoutContext(Locker, Stack, Tall, Pose, Buffers, StorageLight.atLeast(Light, BlockLight), Overlay, PartialTick, World);
    }

    public StorageLayoutContext withTint(int Argb) {
        return new StorageLayoutContext(Locker, Stack, Tall, Pose, new TintedBuffers(Buffers, Argb), Light, Overlay, PartialTick, World);
    }

    public boolean isBlockLike(ItemStack Shown) {
        return canvas().isBlockLike(this, Shown);
    }

    public float halfHeight(ItemStack Shown, ItemDisplayContext Display) {
        return canvas().halfHeight(this, Shown, Display);
    }

    public void drawModel(ModelResourceLocation Location) {
        canvas().drawModel(this, Location, true);
    }

    public void drawRawModel(ModelResourceLocation Location) {
        canvas().drawModel(this, Location, false);
    }

    public void drawBlock(BlockState Shown) {
        canvas().drawBlock(this, Shown);
    }

    public void drawItem(ItemStack Shown, ItemDisplayContext Display) {
        canvas().drawItem(this, Shown, Display);
    }

    public void model(ModelResourceLocation Location, Placement Where) {
        Pose.pushPose();
        Where.apply(Pose);
        drawModel(Location);
        Pose.popPose();
    }

    public void block(BlockState Shown, Placement Where) {
        Pose.pushPose();
        Where.apply(Pose);
        drawBlock(Shown);
        Pose.popPose();
    }

    public void item(ItemStack Shown, Placement Where, ItemDisplayContext Display, ItemAnchor Anchor) {
        Pose.pushPose();
        Where.apply(Pose);
        float Half = halfHeight(Shown, Display);
        if (Anchor == ItemAnchor.Bottom) {
            Pose.translate(0.0f, Half / 16.0f, 0.0f);
        } else if (Anchor == ItemAnchor.Top) {
            Pose.translate(0.0f, -Half / 16.0f, 0.0f);
        }
        drawItem(Shown, Display);
        Pose.popPose();
    }

    public void platform() {
        InteriorLayout.platform(Tall).render(this, LayoutMotion.Still);
    }
}
