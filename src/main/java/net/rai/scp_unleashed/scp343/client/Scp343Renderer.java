package net.rai.scp_unleashed.scp343.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp343.Scp343God;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp343Renderer extends GeoEntityRenderer<Scp343God> {
    public static final float ShadowSize = 0.45f;
    public static final float AlphaSteps = 12.0f;
    public static final int LeastLight = 7;

    public Scp343Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp343GeoModel());
        addRenderLayer(new Scp343GlowLayer(this));
        shadowRadius = ShadowSize;
    }

    public static float alpha(Scp343God God, float PartialTick) {
        return Math.round(God.fade(PartialTick) * AlphaSteps) / AlphaSteps;
    }

    @Override
    public void render(Scp343God God, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        God.setGaitPartial(PartialTick);
        shadowRadius = ShadowSize * God.fade(PartialTick);
        if (alpha(God, PartialTick) <= 0.0f) {
            return;
        }
        super.render(God, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(Scp343God God, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        float Alpha = alpha(God, PartialTick);
        if (Alpha >= 1.0f) {
            return RenderType.entityCutoutNoCull(Texture);
        }
        if (ShaderCompat.renderingShadowPass() && Alpha < 0.5f) {
            return null;
        }
        return RenderType.entityTranslucent(Texture);
    }

    @Override
    public Color getRenderColor(Scp343God God, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha(God, PartialTick));
    }

    @Override
    protected int getBlockLightLevel(Scp343God God, BlockPos Pos) {
        return Math.max(God.isPhasing() ? 15 : LeastLight, super.getBlockLightLevel(God, Pos));
    }
}
