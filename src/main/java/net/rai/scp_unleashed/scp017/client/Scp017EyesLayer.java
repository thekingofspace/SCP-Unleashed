package net.rai.scp_unleashed.scp017.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp017.Scp017Shadow;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class Scp017EyesLayer extends GeoRenderLayer<Scp017Shadow> {
    public static final RenderType Eyes = RenderType.eyes(Scp017GeoModel.EyesPath);
    public static final float GlowSteps = 12.0f;
    public static final float Visible = 0.04f;

    public Scp017EyesLayer(GeoRenderer<Scp017Shadow> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp017Shadow Figure, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Figure.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Glow = Math.round(Figure.eyeGlow(PartialTick) * GlowSteps) / GlowSteps;
        if (Glow < Visible) {
            return;
        }
        int Colour = Color.ofRGBA(Glow, Glow, Glow, 1.0f).argbInt();
        getRenderer().reRender(Model, Pose, Buffers, Figure, Eyes, Buffers.getBuffer(Eyes), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
    }
}
