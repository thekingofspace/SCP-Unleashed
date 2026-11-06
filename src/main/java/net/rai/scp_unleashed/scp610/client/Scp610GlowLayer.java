package net.rai.scp_unleashed.scp610.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp610GlowLayer<T extends Scp610Creature> extends GeoRenderLayer<T> {
    private final ResourceLocation Texture;
    private final RenderType Glow;
    @Nullable
    private Boolean Present;

    public Scp610GlowLayer(GeoRenderer<T> Renderer, String Name) {
        super(Renderer);
        Texture = Scp610GeoModel.glow(Name);
        Glow = RenderType.eyes(Texture);
    }

    private boolean present() {
        if (Present == null) {
            Present = Minecraft.getInstance().getResourceManager().getResource(Texture).isPresent();
        }
        return Present;
    }

    @Override
    public void render(PoseStack Pose, T Creature, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (!present() || Creature.isInvisible() || Creature.deathTime > 0 || ShaderCompat.renderingShadowPass()) {
            return;
        }
        getRenderer().reRender(Model, Pose, Buffers, Creature, Glow, Buffers.getBuffer(Glow), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, 0xFFFFFFFF);
    }
}
