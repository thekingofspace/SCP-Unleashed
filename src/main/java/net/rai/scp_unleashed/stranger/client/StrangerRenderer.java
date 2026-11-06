package net.rai.scp_unleashed.stranger.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

public class StrangerRenderer extends GeoEntityRenderer<StrangerEntity> {
    public StrangerRenderer(EntityRendererProvider.Context Context) {
        super(Context, new StrangerModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.4f;
    }

    @Override
    public RenderType getRenderType(StrangerEntity Figure, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return Figure.fade(PartialTick) < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(StrangerEntity Figure, float PartialTick, int PackedLight) {
        float Alpha = Figure.fade(PartialTick);
        float Stepped = Math.round(Alpha * 8.0f) / 8.0f;
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Math.max(0.0f, Stepped));
    }
}
