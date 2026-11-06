package net.rai.scp_unleashed.scp087.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp087.Scp087Face;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp087FaceGlowLayer extends AutoGlowingGeoLayer<Scp087Face> {
    private float Fade = 1.0f;

    public Scp087FaceGlowLayer(GeoRenderer<Scp087Face> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(Scp087Face Face, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback() && Fade >= 0.999f) {
            return super.getRenderType(Face, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Face)));
    }

    @Override
    public void render(PoseStack Pose, Scp087Face Face, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                       float PartialTick, int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        Fade = Face.fade(PartialTick);
        super.render(Pose, Face, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
    }
}
