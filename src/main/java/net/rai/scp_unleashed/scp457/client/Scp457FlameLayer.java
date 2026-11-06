package net.rai.scp_unleashed.scp457.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Registry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class Scp457FlameLayer extends GeoRenderLayer<Scp457> {
    public static final float Visible = 0.03f;
    public static final float PrimeBoost = 0.35f;
    public static final ResourceLocation[] Frames = new ResourceLocation[Scp457.FlameFrames];

    static {
        for (int Index = 0; Index < Frames.length; Index++) {
            Frames[Index] = Scp457Registry.location("textures/entity/scp457/glow_" + Index + ".png");
        }
    }

    public Scp457FlameLayer(GeoRenderer<Scp457> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp457 Owner, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Owner.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Strength = Owner.flameStrength(PartialTick);
        if (Strength < Visible) {
            return;
        }
        ResourceLocation Frame = Frames[Owner.flameFrame()];
        glow(Pose, Owner, Model, Buffers, PartialTick, Frame, Strength);
        if (Owner.isPrime()) {
            glow(Pose, Owner, Model, Buffers, PartialTick, Frame, Strength * PrimeBoost);
        }
    }

    private void glow(PoseStack Pose, Scp457 Owner, BakedGeoModel Model, MultiBufferSource Buffers, float PartialTick, ResourceLocation Frame, float Strength) {
        if (ShaderCompat.useFallback()) {
            RenderType Glow = RenderType.entityTranslucentEmissive(Frame);
            int Colour = Color.ofRGBA(1.0f, 1.0f, 1.0f, Strength).argbInt();
            getRenderer().reRender(Model, Pose, Buffers, Owner, Glow, Buffers.getBuffer(Glow), PartialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Colour);
            return;
        }
        RenderType Glow = RenderType.eyes(Frame);
        int Colour = Color.ofRGBA(Strength, Strength, Strength, 1.0f).argbInt();
        getRenderer().reRender(Model, Pose, Buffers, Owner, Glow, Buffers.getBuffer(Glow), PartialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Colour);
    }
}
