package net.rai.scp_unleashed.daevite.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class DaeviteGlowLayer<T extends DaeviteMob> extends AutoGlowingGeoLayer<T> {
    public DaeviteGlowLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(T Owner, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback() || Owner.isInvisible()) {
            return super.getRenderType(Owner, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Owner)));
    }
}
