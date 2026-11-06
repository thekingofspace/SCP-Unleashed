package net.rai.scp_unleashed.daevitegod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.daevitegod.DaeviteGodSpirit;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class DaeviteGodSpiritRenderer extends GeoEntityRenderer<DaeviteGodSpirit> {
    public static final float Opacity = 0.85f;

    public DaeviteGodSpiritRenderer(EntityRendererProvider.Context Context) {
        super(Context, new DaeviteGodSpiritGeoModel());
        addRenderLayer(new DaeviteGodGlowLayer<>(this));
        shadowRadius = 0.0f;
    }

    @Override
    public void render(DaeviteGodSpirit Spirit, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Spirit.setRenderPartial(Partial);
        super.render(Spirit, Yaw, Partial, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(DaeviteGodSpirit Spirit, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float Partial) {
        return RenderType.entityTranslucent(Texture);
    }

    @Override
    public Color getRenderColor(DaeviteGodSpirit Spirit, float Partial, int Light) {
        Color Base = super.getRenderColor(Spirit, Partial, Light);
        int Alpha = (int) (Base.getAlpha() * Opacity * Spirit.opacity(Partial));
        return Color.ofARGB(Math.max(0, Math.min(255, Alpha)), Base.getRed(), Base.getGreen(), Base.getBlue());
    }

    @Override
    protected float getDeathMaxRotation(DaeviteGodSpirit Spirit) {
        return 0.0f;
    }
}
