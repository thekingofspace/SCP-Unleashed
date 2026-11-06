package net.rai.scp_unleashed.scp337.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp337.Scp337Mass;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp337Renderer extends GeoEntityRenderer<Scp337Mass> {
    public static final float Shadow = 1.05f;
    public static final float Scale = 1.12f;

    public Scp337Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp337GeoModel());
        shadowRadius = Shadow;
        withScale(Scale);
    }

    @Override
    public void render(Scp337Mass Mass, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Mass.setRenderPartial(PartialTick);
        float Fade = Mass.fade(PartialTick);
        shadowRadius = Shadow * Fade;
        if (Fade > 0.0f) {
            super.render(Mass, Yaw, PartialTick, Pose, Buffers, Light);
        }
        Scp337LashRenderer.render(Mass, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(Scp337Mass Mass, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        float Fade = Mass.fade(PartialTick);
        if (Fade < 0.5f && ShaderCompat.renderingShadowPass()) {
            return null;
        }
        return Fade < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(Scp337Mass Mass, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Mass.fade(PartialTick));
    }

    @Override
    public int getPackedOverlay(Scp337Mass Mass, float U, float PartialTick) {
        if (Mass.isDeadOrDying()) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(Mass, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(Scp337Mass Mass) {
        return 0.0f;
    }
}
