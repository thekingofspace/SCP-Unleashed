package net.rai.scp_unleashed.daevitegod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.daevitegod.DaeviteGod;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class DaeviteGodRenderer extends GeoEntityRenderer<DaeviteGod> {
    public static final float GodShadow = 1.3f;
    public static final float KingShadow = 0.9f;
    public static final float PillarHeight = 96.0f;

    public DaeviteGodRenderer(EntityRendererProvider.Context Context) {
        super(Context, new DaeviteGodGeoModel());
        addRenderLayer(new DaeviteGodGlowLayer<>(this));
        shadowRadius = GodShadow;
    }

    @Override
    public void render(DaeviteGod God, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        God.setRenderPartial(Partial);
        shadowRadius = God.king() ? KingShadow : GodShadow;
        shadowStrength = God.king() ? 1.0f : 0.55f;
        float Collapse = God.collapseLight(Partial);
        if (Collapse < 0.97f) {
            super.render(God, Yaw, Partial, Pose, Buffers, Light);
        }
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Time = God.tickCount + Partial;
        float Pillar = God.pillar(Partial);
        if (Pillar > 0.0f) {
            DaeviteGodLight.beam(Buffers, Pose, 1.1f + 0.15f * Mth.sin(Time * 0.4f), -1.0f, PillarHeight, Time * 4.0f, -Time * 0.05f, 0.85f * Pillar);
            DaeviteGodLight.beam(Buffers, Pose, 2.2f + 0.25f * Mth.sin(Time * 0.25f + 1.0f), -1.0f, PillarHeight, -Time * 2.5f, -Time * 0.03f, 0.35f * Pillar);
            float Here = (float) Mth.lerp(Partial, God.yo, God.getY());
            DaeviteGodLight.flat(Buffers, Pose, DaeviteGodLight.Ring, 3.6f, groundOffset(God) - Here, Time * 2.0f, 1.0f, 0.3f, 0.2f, 0.8f * Pillar);
        }
        if (Collapse > 0.0f) {
            Pose.pushPose();
            Pose.translate(0.0f, God.king() ? 1.8f : 2.6f, 0.0f);
            DaeviteGodLight.orb(Buffers, Pose, 1.2f + 2.4f * Collapse + 0.2f * Mth.sin(Time * 0.9f), Time * 6.0f, Collapse);
            Pose.popPose();
        }
        float Dying = God.deathProgress(Partial);
        if (Dying > 0.0f) {
            float Rise = Mth.clamp(Dying * 1.6f - 0.3f, 0.0f, 1.0f) * Mth.clamp((1.0f - Dying) * 3.0f, 0.0f, 1.0f);
            DaeviteGodLight.beam(Buffers, Pose, 0.6f + Dying * 0.8f, 0.0f, 6.0f + 30.0f * Dying, Time * 5.0f, -Time * 0.06f, 0.7f * Rise);
        }
    }

    private static float groundOffset(DaeviteGod God) {
        return God.home() != null ? God.home().getY() + 0.03f : (float) God.getY() - (float) DaeviteGod.HoverHeight + 0.03f;
    }

    @Override
    public int getPackedOverlay(DaeviteGod God, float U, float Partial) {
        return God.deathTime > 0 ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(God, U, Partial);
    }

    @Override
    protected float getDeathMaxRotation(DaeviteGod God) {
        return 0.0f;
    }

    @Override
    public Color getRenderColor(DaeviteGod God, float PartialTick, int PackedLight) {
        Color Base = super.getRenderColor(God, PartialTick, PackedLight);
        float Dying = God.deathProgress(PartialTick);
        if (Dying <= 0.0f) {
            return Base;
        }
        float Ash = Mth.clamp(Dying * 1.5f, 0.0f, 1.0f);
        int Red = (int) (Base.getRed() * (1.0f - 0.55f * Ash));
        int Green = (int) (Base.getGreen() * (1.0f - 0.6f * Ash));
        int Blue = (int) (Base.getBlue() * (1.0f - 0.6f * Ash));
        return Color.ofARGB(Base.getAlpha(), Red, Green, Blue);
    }
}
