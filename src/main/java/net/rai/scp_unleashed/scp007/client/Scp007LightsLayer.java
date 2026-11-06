package net.rai.scp_unleashed.scp007.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp007.Scp007Entity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class Scp007LightsLayer extends GeoRenderLayer<Scp007Entity> {
    public static final ResourceLocation LightsTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp007/scp007_lights.png");
    public static final RenderType Lights = RenderType.eyes(LightsTexture);
    public static final float Dusk = 0.1f;
    public static final float FadeSpan = 0.3f;
    public static final float GlowSteps = 8.0f;
    public static final float Visible = 0.05f;

    public Scp007LightsLayer(GeoRenderer<Scp007Entity> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp007Entity Owner, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Buffers == null || Owner.isInvisible() || ShaderCompat.renderingShadowPass() || !(getRenderer() instanceof Scp007Renderer Planet)) {
            return;
        }
        Level World = Owner.level();
        if (!World.dimensionType().hasSkyLight() || World.dimensionType().hasFixedTime()) {
            return;
        }
        float Angle = World.getSunAngle(PartialTick);
        Vector3f Sun = new Vector3f(-Mth.sin(Angle), Mth.cos(Angle), 0.0f);
        float Glow = Math.round(Mth.clamp((Dusk - Sun.y()) / FadeSpan, 0.0f, 1.0f) * GlowSteps) / GlowSteps;
        if (Glow < Visible) {
            return;
        }
        int Colour = Color.ofRGBA(Glow, Glow, Glow, 1.0f).argbInt();
        Planet.beginGlow(Sun);
        try {
            getRenderer().reRender(Model, Pose, Buffers, Owner, Lights, Buffers.getBuffer(Lights), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
        } finally {
            Planet.endGlow();
        }
    }
}
