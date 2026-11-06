package net.rai.scp_unleashed.scp001.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.scp001.Scp001Prototype;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class Scp001GlowLayer extends GeoRenderLayer<Scp001Prototype> {
    public static final RenderType Glow = RenderType.eyes(Scp001GeoModel.GlowPath);
    public static final float GlowSteps = 12.0f;
    public static final float Visible = 0.04f;

    public Scp001GlowLayer(GeoRenderer<Scp001Prototype> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp001Prototype Creature, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Creature.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Level = Math.round(Creature.glow(PartialTick) * GlowSteps) / GlowSteps;
        if (Level < Visible) {
            return;
        }
        int Colour = Color.ofRGBA(Level, Level, Level, 1.0f).argbInt();
        getRenderer().reRender(Model, Pose, Buffers, Creature, Glow, Buffers.getBuffer(Glow), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
    }
}
