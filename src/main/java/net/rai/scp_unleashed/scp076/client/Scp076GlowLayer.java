package net.rai.scp_unleashed.scp076.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp076GlowLayer extends AutoGlowingGeoLayer<Scp076Abel> {
    public Scp076GlowLayer(GeoRenderer<Scp076Abel> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(Scp076Abel Owner, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback() || Owner.isInvisible()) {
            return super.getRenderType(Owner, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Owner)));
    }
}
