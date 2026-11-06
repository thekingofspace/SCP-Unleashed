package net.rai.scp_unleashed.scp215.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp215.Scp215Phantom;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp215GlowLayer extends AutoGlowingGeoLayer<Scp215Phantom> {
    public Scp215GlowLayer(GeoRenderer<Scp215Phantom> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(Scp215Phantom Phantom, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback()) {
            return super.getRenderType(Phantom, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Phantom)));
    }

    @Override
    public void render(PoseStack Pose, Scp215Phantom Phantom, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                       float PartialTick, int Light, int Overlay) {
        if (!Phantom.variant().Glows || ShaderCompat.renderingShadowPass() || Phantom.fade(PartialTick) < 0.999f) {
            return;
        }
        super.render(Pose, Phantom, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
    }
}
