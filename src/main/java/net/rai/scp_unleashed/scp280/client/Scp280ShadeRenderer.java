package net.rai.scp_unleashed.scp280.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp280.Scp280Shade;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp280ShadeRenderer extends GeoEntityRenderer<Scp280Shade> {
    public static final float ShadowSize = 0.4f;

    public Scp280ShadeRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp280ShadeGeoModel());
        addRenderLayer(new Scp280ShadeEyesLayer(this));
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp280Shade Shade, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Shade.setGaitPartial(PartialTick);
        super.render(Shade, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(Scp280Shade Shade, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    protected float getDeathMaxRotation(Scp280Shade Shade) {
        return 0.0f;
    }
}
