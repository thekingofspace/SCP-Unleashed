package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Vector3f;

public final class MinecraftStorageCanvas implements StorageCanvas {
    public static final MinecraftStorageCanvas Instance = new MinecraftStorageCanvas();

    private MinecraftStorageCanvas() {
    }

    private static BakedModel itemModel(StorageLayoutContext Context, ItemStack Shown) {
        return Minecraft.getInstance().getItemRenderer().getModel(Shown, Context.World(), null, 0);
    }

    @Override
    public boolean isBlockLike(StorageLayoutContext Context, ItemStack Shown) {
        return itemModel(Context, Shown).isGui3d();
    }

    @Override
    public float halfHeight(StorageLayoutContext Context, ItemStack Shown, ItemDisplayContext Display) {
        PoseStack Probe = new PoseStack();
        itemModel(Context, Shown).applyTransform(Display, Probe, false);
        return StorageLayoutContext.PixelHalf * Probe.last().pose().transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).length();
    }

    @Override
    public void drawModel(StorageLayoutContext Context, ModelResourceLocation Location, boolean Centred) {
        draw(Context, Minecraft.getInstance().getModelManager().getModel(Location), null, Centred);
    }

    @Override
    public void drawBlock(StorageLayoutContext Context, BlockState Shown) {
        draw(Context, Minecraft.getInstance().getBlockRenderer().getBlockModel(Shown), Shown, true);
    }

    @Override
    public void drawItem(StorageLayoutContext Context, ItemStack Shown, ItemDisplayContext Display) {
        if (ShaderCompat.useFallback()) {
            Minecraft.getInstance().getItemRenderer().renderStatic(Shown, Display, Context.Light(), OverlayTexture.NO_OVERLAY, Context.Pose(), new LockerSolidBuffers(Context.Buffers()),
                    Context.World(), (int) Context.seed());
            return;
        }
        MultiBufferSource.BufferSource Source = LockerFlushBuffers.source(Context.Buffers());
        if (Source == null) {
            Minecraft.getInstance().getItemRenderer().renderStatic(Shown, Display, Context.Light(), OverlayTexture.NO_OVERLAY, Context.Pose(), new LockerItemBuffers(Context.Buffers()),
                    Context.World(), (int) Context.seed());
            return;
        }
        LockerFlushBuffers Recorded = new LockerFlushBuffers(Context.Buffers());
        Minecraft.getInstance().getItemRenderer().renderStatic(Shown, Display, Context.Light(), OverlayTexture.NO_OVERLAY, Context.Pose(), Recorded, Context.World(), (int) Context.seed());
        Recorded.flush(Source);
    }

    private static void draw(StorageLayoutContext Context, BakedModel Model, BlockState Shown, boolean Centred) {
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        if (Centred) {
            Pose.translate(-0.5f, 0.0f, -0.5f);
        }
        StorageQuads.render(Pose.last(), Context.Buffers().getBuffer(RenderType.cutout()), Model, Shown, Context.World(), 1.0f, 1.0f, 1.0f, 1.0f, Context.Light(), Context.Overlay(), true);
        Pose.popPose();
    }
}
