package net.rai.scp_unleashed.scp343.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp343.Scp343God;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class Scp343GlowLayer extends GeoRenderLayer<Scp343God> {
    public static final RenderType Glow = RenderType.eyes(Scp343GeoModel.GlowPath);
    public static final RenderType FallbackGlow = RenderType.entityTranslucentEmissive(Scp343GeoModel.GlowPath);
    public static final float Strength = 0.6f;
    public static final float Visible = 0.04f;

    public Scp343GlowLayer(GeoRenderer<Scp343God> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp343God God, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (God.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Shine = Strength * Scp343Renderer.alpha(God, PartialTick);
        if (Shine < Visible) {
            return;
        }
        if (ShaderCompat.useFallback()) {
            int Colour = Color.ofRGBA(1.0f, 1.0f, 1.0f, Shine).argbInt();
            getRenderer().reRender(Model, Pose, Buffers, God, FallbackGlow, Buffers.getBuffer(FallbackGlow), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
            return;
        }
        int Colour = Color.ofRGBA(Shine, Shine, Shine, 1.0f).argbInt();
        getRenderer().reRender(Model, Pose, Buffers, God, Glow, Buffers.getBuffer(Glow), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
    }
}
