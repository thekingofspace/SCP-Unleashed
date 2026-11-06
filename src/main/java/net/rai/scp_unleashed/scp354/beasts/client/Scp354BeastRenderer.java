package net.rai.scp_unleashed.scp354.beasts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp354BeastRenderer<T extends Scp354Beast> extends GeoEntityRenderer<T> {
    public static final float AlphaSteps = 16.0f;
    private final float Shadow;

    public Scp354BeastRenderer(EntityRendererProvider.Context Context, String Name, float Scale, float Shadow) {
        super(Context, new Scp354BeastGeoModel<>(Name));
        withScale(Scale);
        this.Shadow = Shadow;
        shadowRadius = Shadow;
        addRenderLayer(new Scp354BeastGlowLayer<>(this, Name));
    }

    public static float alpha(Scp354Beast Creature, float PartialTick) {
        return Math.round(Creature.fade(PartialTick) * AlphaSteps) / AlphaSteps;
    }

    @Override
    public void render(T Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        Creature.applyLockedYaw();
        shadowRadius = Shadow * Creature.emergeShare(PartialTick) * Creature.fade(PartialTick);
        if (alpha(Creature, PartialTick) <= 0.0f) {
            return;
        }
        super.render(Creature, Creature.locksYaw() ? Creature.getYRot() : Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(T Creature, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        float Alpha = alpha(Creature, PartialTick);
        if (Alpha < 0.5f && ShaderCompat.renderingShadowPass()) {
            return null;
        }
        return Alpha < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(T Creature, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha(Creature, PartialTick));
    }

    @Override
    public int getPackedOverlay(T Creature, float U, float PartialTick) {
        return Creature.deathTime > 0 ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(Creature, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(T Creature) {
        return 0.0f;
    }
}
