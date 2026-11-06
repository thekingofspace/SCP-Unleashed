package net.rai.scp_unleashed.scp106.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp106.Scp106Portal;

public class Scp106PortalRenderer extends EntityRenderer<Scp106Portal> {
    private static final ResourceLocation WallTexture = Scp106GeoModel.location("textures/entity/scp106/portal_wall.png");
    private static final ResourceLocation CeilingTexture = Scp106GeoModel.location("textures/entity/scp106/portal_ceiling.png");

    public Scp106PortalRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    @Override
    public void render(Scp106Portal Portal, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Spread = Portal.spread(Partial);
        float Fade = Portal.fade(Partial);
        if (Spread <= 0.0f || Fade <= 0.0f) {
            return;
        }
        Direction Face = Portal.face();
        Pose.pushPose();
        if (Face == Direction.DOWN) {
            Pose.mulPose(Axis.XP.rotationDegrees(90.0f));
        } else if (Face == Direction.UP) {
            Pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
        } else {
            Pose.mulPose(Axis.YP.rotationDegrees(-Face.toYRot()));
        }
        float Half = Portal.portalWidth() * Spread * 0.5f;
        float Low = Face.getAxis().isVertical() ? -Portal.portalHeight() * Spread * 0.5f : 0.0f;
        float High = Face.getAxis().isVertical() ? Portal.portalHeight() * Spread * 0.5f : Portal.portalHeight() * Spread;
        int Alpha = Mth.clamp(Math.round(Fade * 255.0f), 0, 255);
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(Portal)));
        PoseStack.Pose Last = Pose.last();
        corner(Buffer, Last, -Half, Low, 0.0f, 1.0f, Alpha, Light);
        corner(Buffer, Last, Half, Low, 1.0f, 1.0f, Alpha, Light);
        corner(Buffer, Last, Half, High, 1.0f, 0.0f, Alpha, Light);
        corner(Buffer, Last, -Half, High, 0.0f, 0.0f, Alpha, Light);
        Pose.popPose();
    }

    private static void corner(VertexConsumer Buffer, PoseStack.Pose Last, float X, float Y, float U, float V, int Alpha, int Light) {
        Buffer.addVertex(Last, X, Y, 0.0f).setColor(255, 255, 255, Alpha).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp106Portal Portal) {
        return Portal.face().getAxis().isVertical() ? CeilingTexture : WallTexture;
    }
}
