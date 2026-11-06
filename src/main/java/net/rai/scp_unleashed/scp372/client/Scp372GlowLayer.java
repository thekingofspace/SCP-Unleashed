package net.rai.scp_unleashed.scp372.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.function.Predicate;

public class Scp372GlowLayer<T extends GeoAnimatable> extends AutoGlowingGeoLayer<T> {
    private final Predicate<T> Shows;

    public Scp372GlowLayer(GeoRenderer<T> Renderer, Predicate<T> Shows) {
        super(Renderer);
        this.Shows = Shows;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(T Animatable, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback()) {
            return super.getRenderType(Animatable, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Animatable)));
    }

    @Override
    public void render(PoseStack Pose, T Animatable, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                       float PartialTick, int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass() || !Shows.test(Animatable)) {
            return;
        }
        super.render(Pose, Animatable, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
    }
}
