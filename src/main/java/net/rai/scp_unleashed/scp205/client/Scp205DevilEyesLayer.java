package net.rai.scp_unleashed.scp205.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp205.Scp205Devil;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class Scp205DevilEyesLayer extends GeoRenderLayer<Scp205Devil> {
    public static final RenderType Eyes = RenderType.eyes(Scp205DevilGeoModel.EyesPath);
    public static final float GlowSteps = 12.0f;
    public static final float Visible = 0.04f;

    public Scp205DevilEyesLayer(GeoRenderer<Scp205Devil> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp205Devil Devil, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Devil.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Glow = Math.round(Devil.eyeGlow(PartialTick) * GlowSteps) / GlowSteps;
        if (Glow < Visible) {
            return;
        }
        int Colour = Color.ofRGBA(Glow, Glow, Glow, 1.0f).argbInt();
        getRenderer().reRender(Model, Pose, Buffers, Devil, Eyes, Buffers.getBuffer(Eyes), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
    }
}
