package net.rai.scp_unleashed.daevite.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteBolt;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class DaeviteBoltRenderer extends EntityRenderer<DaeviteBolt> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/daevite/bolt.png");
    public static final float Size = 0.42f;

    public DaeviteBoltRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(DaeviteBolt Bolt, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        boolean Additive = ShaderCompat.useFallback();
        VertexConsumer Consumer = Buffers.getBuffer(Additive ? RenderType.eyes(Texture) : RenderType.entityTranslucentEmissive(Texture));
        float Age = Bolt.tickCount + Partial;
        float Half = Size * (0.9f + 0.1f * Mth.sin(Age * 1.3f));
        Pose.pushPose();
        Pose.translate(0.0f, Bolt.getBbHeight() * 0.5f, 0.0f);
        Pose.mulPose(entityRenderDispatcher.cameraOrientation());
        Pose.mulPose(Axis.ZP.rotationDegrees(Age * 24.0f));
        PoseStack.Pose Last = Pose.last();
        vertex(Consumer, Last, -Half, -Half, 0.0f, 1.0f);
        vertex(Consumer, Last, Half, -Half, 1.0f, 1.0f);
        vertex(Consumer, Last, Half, Half, 1.0f, 0.0f);
        vertex(Consumer, Last, -Half, Half, 0.0f, 0.0f);
        Pose.popPose();
        super.render(Bolt, Yaw, Partial, Pose, Buffers, PackedLight);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float U, float V) {
        Consumer.addVertex(Last, X, Y, 0.0f)
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(DaeviteBolt Bolt) {
        return Texture;
    }
}
