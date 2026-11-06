package net.rai.scp_unleashed.scp939.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939Breath;
import software.bernie.geckolib.util.Color;

public class Scp939Renderer extends SharedGeoRenderer<Scp939> {
    public static final float Scale = 1.5f;
    public static final float ShadowRadius = 0.85f;
    public static final float FadeDepth = 0.55f;
    public static final float Shimmer = 0.16f;

    public Scp939Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp939GeoModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
        addRenderLayer(new Scp939HazeLayer(this));
    }

    public static float hazeShown(Scp939 Owner) {
        if (!Owner.isHazed() || Minecraft.getInstance().player == null || Scp939Breath.masked(Minecraft.getInstance().player)) {
            return 0.0f;
        }
        return Mth.clamp((Owner.haze() - Scp939.HazeVisible) / (1.0f - Scp939.HazeVisible) * 0.7f + 0.3f, 0.0f, 1.0f);
    }

    public static float shimmer(Scp939 Owner, float Partial) {
        float Time = Owner.tickCount + Partial + Owner.getId() * 13.0f;
        return 0.5f + 0.5f * Mth.sin(Time * 0.31f) * Mth.sin(Time * 0.137f + 1.3f);
    }

    @Override
    public void render(Scp939 Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        float Size = Owner.isBaby() ? Scale * 0.5f : Scale;
        scaleWidth = Size;
        scaleHeight = Size;
        shadowRadius = Owner.isBaby() ? ShadowRadius * 0.5f : ShadowRadius;
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public RenderType getRenderType(Scp939 Owner, ResourceLocation Texture, MultiBufferSource Buffers, float PartialTick) {
        if (hazeShown(Owner) > 0.0f) {
            return RenderType.entityTranslucent(Texture);
        }
        return super.getRenderType(Owner, Texture, Buffers, PartialTick);
    }

    @Override
    public Color getRenderColor(Scp939 Owner, float PartialTick, int Light) {
        float Haze = hazeShown(Owner);
        if (Haze <= 0.0f) {
            return super.getRenderColor(Owner, PartialTick, Light);
        }
        float Alpha = Mth.clamp(1.0f - FadeDepth * Haze - Shimmer * Haze * shimmer(Owner, PartialTick), 0.18f, 1.0f);
        return Color.ofRGBA(0.92f, 0.86f, 0.86f, Alpha);
    }
}
