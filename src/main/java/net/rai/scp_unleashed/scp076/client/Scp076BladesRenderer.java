package net.rai.scp_unleashed.scp076.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp076.Scp076Blades;

public class Scp076BladesRenderer extends EntityRenderer<Scp076Blades> {
    public static final float[][] Cluster = {{0.0f, 0.0f, 0.0f, 0.0f, 1.9f}, {0.22f, 0.1f, 16.0f, 40.0f, 1.5f}, {-0.2f, 0.15f, -14.0f, -30.0f, 1.35f},
            {0.05f, -0.24f, 12.0f, 150.0f, 1.2f}};
    public static final float Buried = 5.0f / 16.0f;

    public Scp076BladesRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp076Blades Blades, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Rise = Blades.rise(Partial);
        if (Rise > 0.0f) {
            VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
            Pose.pushPose();
            Pose.mulPose(Axis.YP.rotationDegrees(-Blades.getYRot()));
            for (float[] Blade : Cluster) {
                float Scale = Blade[4];
                Pose.pushPose();
                Pose.translate(Blade[0], 0.0f, Blade[1]);
                Pose.mulPose(Axis.YP.rotationDegrees(Blade[3]));
                Pose.mulPose(Axis.ZP.rotationDegrees(Blade[2]));
                Pose.translate(0.0f, -(1.0f - Rise) * Scale - Buried * Scale, 0.0f);
                Pose.scale(Scale, Scale, Scale);
                Pose.translate(-0.5f, 0.0f, -0.5f);
                Scp076Models.render(Pose.last(), Consumer, Scp076Models.get(Scp076Models.Knife), Light, OverlayTexture.NO_OVERLAY, 1.0f, 0.0f, Blades.level(),
                        Direction.SOUTH);
                Pose.popPose();
            }
            Pose.popPose();
        }
        super.render(Blades, Yaw, Partial, Pose, Buffers, Light);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp076Blades Blades) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
