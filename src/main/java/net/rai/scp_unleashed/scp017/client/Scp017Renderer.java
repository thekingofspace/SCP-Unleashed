package net.rai.scp_unleashed.scp017.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp017.Scp017Shadow;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp017Renderer extends GeoEntityRenderer<Scp017Shadow> {
    public static final float ShadowSize = 0.42f;
    public static final float AlphaSteps = 16.0f;

    public Scp017Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp017GeoModel());
        addRenderLayer(new Scp017EyesLayer(this));
        shadowRadius = ShadowSize;
    }

    public static float bodyAlpha(Scp017Shadow Figure, float PartialTick) {
        float Veil = Figure.veil(PartialTick);
        float Alpha = 1.0f - Veil * (1.0f - Scp017Shadow.HiddenAlpha);
        return Math.round(Alpha * AlphaSteps) / AlphaSteps;
    }

    @Override
    public void render(Scp017Shadow Figure, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Figure.setGaitPartial(PartialTick);
        shadowRadius = ShadowSize * (1.0f - Figure.veil(PartialTick));
        super.render(Figure, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(Scp017Shadow Figure, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        float Alpha = bodyAlpha(Figure, PartialTick);
        if (Alpha < 0.5f && ShaderCompat.renderingShadowPass()) {
            return null;
        }
        return Alpha < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(Scp017Shadow Figure, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, bodyAlpha(Figure, PartialTick));
    }

    @Override
    protected float getDeathMaxRotation(Scp017Shadow Figure) {
        return 0.0f;
    }
}
