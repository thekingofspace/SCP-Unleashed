package net.rai.scp_unleashed.scp280.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp280.Scp280Shade;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class Scp280ShadeEyesLayer extends GeoRenderLayer<Scp280Shade> {
    public static final RenderType Eyes = RenderType.eyes(Scp280ShadeGeoModel.EyesPath);
    public static final float GlowSteps = 8.0f;
    public static final float Visible = 0.04f;

    public Scp280ShadeEyesLayer(GeoRenderer<Scp280Shade> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp280Shade Shade, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Buffers == null || Shade.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Glow = Math.round(Scp280ShadeEyes.glow(Shade, PartialTick) * GlowSteps) / GlowSteps;
        if (Glow < Visible) {
            return;
        }
        int Colour = Color.ofRGBA(Glow, Glow, Glow, 1.0f).argbInt();
        getRenderer().reRender(Model, Pose, Buffers, Shade, Eyes, Buffers.getBuffer(Eyes), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
    }
}
