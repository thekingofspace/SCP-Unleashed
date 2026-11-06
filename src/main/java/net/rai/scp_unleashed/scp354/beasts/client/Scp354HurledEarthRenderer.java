package net.rai.scp_unleashed.scp354.beasts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp354.beasts.Scp354HurledEarth;

public class Scp354HurledEarthRenderer extends EntityRenderer<Scp354HurledEarth> {
    public static final float Size = 0.8f;
    private final BlockRenderDispatcher Blocks;

    public Scp354HurledEarthRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        Blocks = Context.getBlockRenderDispatcher();
        shadowRadius = 0.35f;
    }

    @Override
    public void render(Scp354HurledEarth Thrown, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Spin = (Thrown.tickCount + PartialTick) * 18.0f;
        Pose.pushPose();
        Pose.translate(0.0f, Size * 0.5f, 0.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(Spin * 0.6f));
        Pose.mulPose(Axis.XP.rotationDegrees(Spin));
        Pose.scale(Size, Size, Size);
        Pose.translate(-0.5f, -0.5f, -0.5f);
        Blocks.renderSingleBlock(Thrown.held(), Pose, Buffers, Light, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
        super.render(Thrown, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp354HurledEarth Thrown) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
