package net.rai.scp_unleashed.scp205.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp205.Scp205Devil;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp205DevilRenderer extends GeoEntityRenderer<Scp205Devil> {
    public static final float ShadowSize = 0.45f;
    public static final float BodyAlpha = 0.86f;
    public static final float AlphaSteps = 16.0f;

    public Scp205DevilRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp205DevilGeoModel());
        addRenderLayer(new Scp205DevilEyesLayer(this));
        shadowRadius = ShadowSize;
    }

    public static float bodyAlpha(Scp205Devil Devil, float PartialTick) {
        return Math.round(BodyAlpha * Devil.veil(PartialTick) * AlphaSteps) / AlphaSteps;
    }

    @Override
    public void render(Scp205Devil Devil, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        shadowRadius = ShadowSize * Devil.veil(PartialTick);
        if (bodyAlpha(Devil, PartialTick) <= 0.0f) {
            return;
        }
        super.render(Devil, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(Scp205Devil Devil, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        if (ShaderCompat.renderingShadowPass() && bodyAlpha(Devil, PartialTick) < 0.5f) {
            return null;
        }
        return RenderType.entityTranslucent(Texture);
    }

    @Override
    public Color getRenderColor(Scp205Devil Devil, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, bodyAlpha(Devil, PartialTick));
    }

    @Override
    protected float getDeathMaxRotation(Scp205Devil Devil) {
        return 0.0f;
    }
}
