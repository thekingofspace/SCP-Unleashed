package net.rai.scp_unleashed.mtf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.mtf.MtfHelicopter;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class MtfHelicopterGlowLayer extends AutoGlowingGeoLayer<MtfHelicopter> {
    public MtfHelicopterGlowLayer(GeoRenderer<MtfHelicopter> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(MtfHelicopter Animatable, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback()) {
            return super.getRenderType(Animatable, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Animatable)));
    }

    @Override
    public void render(PoseStack Pose, MtfHelicopter Animatable, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                       float PartialTick, int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        super.render(Pose, Animatable, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
    }
}
