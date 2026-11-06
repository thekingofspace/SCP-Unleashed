package net.rai.scp_unleashed.scp016.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp016GlowLayer<T extends Scp016Spawn> extends AutoGlowingGeoLayer<T> {
    public Scp016GlowLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(T Creature, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback() || Creature.isInvisible()) {
            return super.getRenderType(Creature, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Creature)));
    }
}
