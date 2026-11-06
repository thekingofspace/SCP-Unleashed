package net.rai.scp_unleashed.scp354.beasts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp354BeastGlowLayer<T extends Scp354Beast> extends GeoRenderLayer<T> {
    private final RenderType Glow;

    public Scp354BeastGlowLayer(GeoRenderer<T> Renderer, String Name) {
        super(Renderer);
        Glow = RenderType.eyes(Scp354BeastGeoModel.glow(Name));
    }

    @Override
    public void render(PoseStack Pose, T Creature, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Creature.isInvisible() || Creature.deathTime > 0 || ShaderCompat.renderingShadowPass()) {
            return;
        }
        getRenderer().reRender(Model, Pose, Buffers, Creature, Glow, Buffers.getBuffer(Glow), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, 0xFFFFFFFF);
    }
}
