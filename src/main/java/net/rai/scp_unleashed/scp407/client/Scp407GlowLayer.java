package net.rai.scp_unleashed.scp407.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp407GlowLayer<T extends LivingEntity & GeoAnimatable> extends AutoGlowingGeoLayer<T> {
    public Scp407GlowLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(T Owner, @Nullable MultiBufferSource Buffers) {
        if (Owner.isInvisible() || Owner.isDeadOrDying()) {
            return null;
        }
        if (!ShaderCompat.useFallback()) {
            return super.getRenderType(Owner, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Owner)));
    }
}
