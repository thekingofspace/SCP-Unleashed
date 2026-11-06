package net.rai.scp_unleashed.scp001gate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp001gate.Scp001GateGuardian;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp001GateGlowLayer extends AutoGlowingGeoLayer<Scp001GateGuardian> {
    public Scp001GateGlowLayer(GeoRenderer<Scp001GateGuardian> Renderer) {
        super(Renderer);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(Scp001GateGuardian Owner, @Nullable MultiBufferSource Buffers) {
        if (!ShaderCompat.useFallback()) {
            return super.getRenderType(Owner, Buffers);
        }
        return RenderType.entityTranslucentEmissive(AutoGlowingTexture.getEmissiveResource(getTextureResource(Owner)));
    }

    @Override
    public void render(PoseStack Pose, Scp001GateGuardian Owner, BakedGeoModel Model, @Nullable RenderType Kind, MultiBufferSource Buffers, @Nullable VertexConsumer Builder,
                       float PartialTick, int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        super.render(Pose, Owner, Model, Kind, Buffers, Builder, PartialTick, Light, Overlay);
    }
}
