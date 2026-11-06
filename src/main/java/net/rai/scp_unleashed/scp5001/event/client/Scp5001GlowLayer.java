package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class Scp5001GlowLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private final Function<T, ResourceLocation> Texture;
    private final ToDoubleFunction<T> Strength;

    public Scp5001GlowLayer(GeoRenderer<T> Renderer, Function<T, ResourceLocation> Texture, ToDoubleFunction<T> Strength) {
        super(Renderer);
        this.Texture = Texture;
        this.Strength = Strength;
    }

    @Override
    public void render(PoseStack Pose, T Owner, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder, float Partial,
                       int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        ResourceLocation Glow = Texture.apply(Owner);
        RenderType Emissive = ShaderCompat.useFallback() ? RenderType.entityTranslucentEmissive(Glow) : RenderType.eyes(Glow);
        float Shown = (float) Math.max(0.0, Math.min(1.0, Strength.applyAsDouble(Owner)));
        int Channel = Math.round(255 * Shown);
        int Colour = 0xFF000000 | Channel << 16 | Channel << 8 | Channel;
        getRenderer().reRender(Model, Pose, Buffers, Owner, Emissive, Buffers.getBuffer(Emissive), Partial, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Colour);
    }
}
