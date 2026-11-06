package net.rai.scp_unleashed.scp4666.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp4666.Scp4666YuleMan;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp4666GlowLayer extends AutoGlowingGeoLayer<Scp4666YuleMan> {
    public Scp4666GlowLayer(GeoRenderer<Scp4666YuleMan> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(Scp4666YuleMan Man, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback() || Man.isInvisible()) {
            return super.getRenderType(Man, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Man)));
    }

    @Override
    public void render(PoseStack Pose, Scp4666YuleMan Man, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                       float PartialTick, int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        super.render(Pose, Man, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
    }
}
