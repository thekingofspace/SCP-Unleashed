package net.rai.scp_unleashed.scp016.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp016.Scp016Shard;

public class Scp016ShardRenderer extends EntityRenderer<Scp016Shard> {
    public static final ResourceLocation Texture = Scp016GeoModel.location("textures/entity/scp016/shard.png");
    public static final float Size = 0.6f;
    public static final float Spin = 24.0f;
    private static final RenderType Layer = RenderType.entityCutoutNoCull(Texture);

    public Scp016ShardRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    protected int getBlockLightLevel(Scp016Shard Shard, BlockPos Pos) {
        return 15;
    }

    @Override
    public void render(Scp016Shard Shard, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (Shard.tickCount < 2 && entityRenderDispatcher.camera.getEntity().distanceToSqr(Shard) < 12.25) {
            return;
        }
        Pose.pushPose();
        Pose.translate(0.0f, 0.15f, 0.0f);
        Pose.scale(Size, Size, Size);
        Pose.mulPose(entityRenderDispatcher.cameraOrientation());
        Pose.mulPose(Axis.ZP.rotationDegrees((Shard.tickCount + PartialTick) * Spin));
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Buffer = Buffers.getBuffer(Layer);
        vertex(Buffer, Last, Light, 0.0f, 0.0f, 0.0f, 1.0f);
        vertex(Buffer, Last, Light, 1.0f, 0.0f, 1.0f, 1.0f);
        vertex(Buffer, Last, Light, 1.0f, 1.0f, 1.0f, 0.0f);
        vertex(Buffer, Last, Light, 0.0f, 1.0f, 0.0f, 0.0f);
        Pose.popPose();
        super.render(Shard, Yaw, PartialTick, Pose, Buffers, Light);
    }

    private static void vertex(VertexConsumer Buffer, PoseStack.Pose Last, int Light, float X, float Y, float U, float V) {
        Buffer.addVertex(Last, X - 0.5f, Y - 0.5f, 0.0f)
                .setColor(-1)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light)
                .setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp016Shard Shard) {
        return Texture;
    }
}
