package net.rai.scp_unleashed.daevitegod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.daevitegod.DaeviteGodSigil;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class DaeviteGodSigilRenderer extends EntityRenderer<DaeviteGodSigil> {
    public static final int SpikeCount = 6;
    public static final float SpikeBuried = 0.35f;

    public DaeviteGodSigilRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(DaeviteGodSigil Sigil, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        byte Kind = Sigil.kind();
        if (Kind == DaeviteGodSigil.KindSpikes) {
            spikes(Sigil, Partial, Pose, Buffers, Light);
        }
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Appear = Sigil.appear(Partial);
        float Charge = Sigil.charge(Partial);
        float Elapsed = Sigil.elapsed(Partial);
        float Since = Sigil.sinceTrigger(Partial);
        float Radius = Sigil.radius();
        if (Appear > 0.0f) {
            float Grow = Mth.clamp(Elapsed / DaeviteGodSigil.FadeIn, 0.0f, 1.0f);
            float Size = Radius * (0.55f + 0.45f * Grow * Grow * (3.0f - 2.0f * Grow));
            float Blink = Charge > 0.72f && Since < 0.0f ? 0.25f * Mth.sin(Elapsed * 2.2f) : 0.0f;
            float Strength = Appear * Mth.clamp(0.6f + 0.4f * Charge + Blink, 0.0f, 1.0f);
            float Flash = Since >= 0.0f ? Mth.clamp(1.0f - Since / 6.0f, 0.0f, 1.0f) : 0.0f;
            float Seed = Sigil.seed() % 360;
            ResourceLocation Glyph = switch (Kind) {
                case DaeviteGodSigil.KindLightning -> DaeviteGodLight.LightningGlyph;
                case DaeviteGodSigil.KindSpikes -> DaeviteGodLight.SpikeGlyph;
                default -> DaeviteGodLight.SpiritGlyph;
            };
            float Spin = Elapsed * (1.6f + 5.0f * Charge * Charge);
            DaeviteGodLight.flat(Buffers, Pose, DaeviteGodLight.Ring, Size, 0.03f, Seed + Spin, 1.0f, 0.28f + 0.3f * Flash, 0.2f + 0.25f * Flash, Strength);
            DaeviteGodLight.flat(Buffers, Pose, Glyph, Size * 0.82f, 0.04f, Seed - Spin * 1.4f, 1.0f, 0.35f + 0.25f * Charge, 0.25f + 0.2f * Charge, Strength);
            if (Flash > 0.0f) {
                DaeviteGodLight.flat(Buffers, Pose, DaeviteGodLight.Orb, Size * (1.0f + 0.4f * (1.0f - Flash)), 0.05f, Seed, 1.0f, 0.55f, 0.4f, Flash * 0.9f);
            }
        }
        if (Kind == DaeviteGodSigil.KindLightning && Since >= 0.0f && Since < DaeviteGodSigil.BoltTicks) {
            float Fade = 1.0f - Since / DaeviteGodSigil.BoltTicks;
            long Seed = Sigil.seed() * 31L + (long) Since / 2;
            DaeviteGodLight.bolt(Buffers, Pose, Seed, Fade);
        }
        super.render(Sigil, Yaw, Partial, Pose, Buffers, Light);
    }

    private void spikes(DaeviteGodSigil Sigil, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Rise = Sigil.spikeRise(Partial);
        if (Rise <= 0.0f) {
            return;
        }
        RandomSource Random = RandomSource.create(Sigil.seed());
        float Radius = Sigil.radius();
        for (int Index = 0; Index < SpikeCount; Index++) {
            boolean Centre = Index == 0;
            float Angle = Random.nextFloat() * 360.0f;
            float Out = Centre ? 0.0f : Radius * (0.35f + Random.nextFloat() * 0.5f);
            float Scale = Centre ? 1.7f : 0.9f + Random.nextFloat() * 0.6f;
            float Tilt = Centre ? 0.0f : 10.0f + Random.nextFloat() * 18.0f;
            float Turn = Random.nextFloat() * 360.0f;
            float Delay = Centre ? 0.0f : Random.nextFloat() * 0.25f;
            float Grow = Mth.clamp((Rise - Delay) / (1.0f - Delay), 0.0f, 1.0f);
            if (Grow <= 0.0f) {
                continue;
            }
            BakedModel Model = DaeviteGodModels.get(Index % 2 == 0 ? DaeviteGodModels.Spike : DaeviteGodModels.SpikeThin);
            VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
            Pose.pushPose();
            Pose.mulPose(Axis.YP.rotationDegrees(Angle));
            Pose.translate(Out, 0.0f, 0.0f);
            Pose.mulPose(Axis.YP.rotationDegrees(Turn));
            Pose.mulPose(Axis.ZP.rotationDegrees(Tilt));
            Pose.translate(0.0f, -(1.0f - Grow) * Scale * 1.2f - SpikeBuried * Scale, 0.0f);
            Pose.scale(Scale, Scale, Scale);
            Pose.translate(-0.5f, 0.0f, -0.5f);
            DaeviteGodModels.render(Pose.last(), Consumer, Model, Math.max(Light, LightTexture.pack(9, 0)), OverlayTexture.NO_OVERLAY);
            Pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(DaeviteGodSigil Sigil) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
