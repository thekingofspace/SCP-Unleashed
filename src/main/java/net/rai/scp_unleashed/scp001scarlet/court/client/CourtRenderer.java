package net.rai.scp_unleashed.scp001scarlet.court.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp001scarlet.court.AshWraith;
import net.rai.scp_unleashed.scp001scarlet.court.CourtDaughter;
import net.rai.scp_unleashed.scp001scarlet.court.CourtMinion;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterBride;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterMoon;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class CourtRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
    public static final float PhasedAlpha = 0.18f;

    public CourtRenderer(EntityRendererProvider.Context Context, String Name, float Scale, float Shadow) {
        super(Context, new CourtGeoModel<>(Name));
        withScale(Scale);
        shadowRadius = Shadow;
        addRenderLayer(new CourtGlowLayer<>(this, Name));
    }

    @Override
    public void render(T Subject, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (Subject instanceof CourtMinion Minion) {
            Minion.setRenderPartial(PartialTick);
        }
        super.render(Subject, Yaw, PartialTick, Pose, Buffers, Light);
    }

    public static float alpha(Entity Subject, float PartialTick) {
        if (Subject instanceof AshWraith Wraith && Wraith.phased()) {
            return PhasedAlpha;
        }
        if (Subject instanceof DaughterMoon Moon && Moon.translucent()) {
            return Moon.alpha();
        }
        if (Subject instanceof DaughterBride Bride && Bride.spectral()) {
            return DaughterBride.SpectralAlpha + 0.12f * (float) Math.sin((Bride.tickCount + PartialTick) * 0.6);
        }
        return 1.0f;
    }

    @Nullable
    @Override
    public RenderType getRenderType(T Subject, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        if (alpha(Subject, PartialTick) < 1.0f) {
            return RenderType.entityTranslucent(Texture);
        }
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(T Subject, float PartialTick, int PackedLight) {
        float Alpha = alpha(Subject, PartialTick);
        if (Alpha < 1.0f) {
            return Color.ofRGBA(1.0f, 1.0f, 1.0f, Alpha);
        }
        return super.getRenderColor(Subject, PartialTick, PackedLight);
    }

    @Override
    public int getPackedOverlay(T Subject, float U, float PartialTick) {
        if (Subject instanceof CourtDaughter || Subject instanceof CourtMinion Minion && Minion.deathTime > 0) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(Subject, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(T Subject) {
        return 0.0f;
    }
}
