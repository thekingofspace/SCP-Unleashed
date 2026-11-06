package net.rai.scp_unleashed.scp3199.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp3199.Scp3199;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp3199GlowLayer extends AutoGlowingGeoLayer<Scp3199> {
    public Scp3199GlowLayer(GeoRenderer<Scp3199> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(Scp3199 Owner, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback() || Owner.isInvisible()) {
            return super.getRenderType(Owner, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Owner)));
    }

    @Override
    public void render(PoseStack Pose, Scp3199 Owner, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                       float PartialTick, int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass() || Owner.isDeadOrDying()) {
            return;
        }
        super.render(Pose, Owner, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
    }
}
