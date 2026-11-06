package net.rai.scp_unleashed.scp1730.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;

public class Scp1730FieldRenderer extends EntityRenderer<RealityAnchorField> {
    public static final float PeakAlpha = 0.11f;

    public Scp1730FieldRenderer(EntityRendererProvider.Context Context) {
        super(Context);
    }

    @Override
    public void render(RealityAnchorField Field, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Share = Field.share();
        float Grow = Mth.clamp((1.0f - Share) * 12.0f, 0.0f, 1.0f);
        float Fade = Mth.clamp(Share * 6.0f, 0.0f, 1.0f);
        Scp1730Dome.render(Pose, Buffers, Field.radius() * (0.4 + 0.6 * Grow), PeakAlpha * Fade, (Field.tickCount + Partial) * 0.01f);
    }

    @Override
    public boolean shouldRender(RealityAnchorField Field, Frustum View, double CamX, double CamY, double CamZ) {
        return View.isVisible(Field.getBoundingBox().inflate(Field.radius()));
    }

    @Override
    public ResourceLocation getTextureLocation(RealityAnchorField Field) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
