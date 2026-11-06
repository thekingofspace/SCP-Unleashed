package net.rai.scp_unleashed.scp215.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp215.Scp215Phantom;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp215PhantomRenderer extends GeoEntityRenderer<Scp215Phantom> {
    public static final int MinimumLight = 4;
    public static final float AlphaSteps = 8.0f;

    public Scp215PhantomRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp215PhantomModel());
        addRenderLayer(new Scp215GlowLayer(this));
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp215Phantom Phantom, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        if (Phantom.flickerOff()) {
            return;
        }
        super.render(Phantom, Yaw, PartialTick, Matrices, Buffers, Light);
    }

    @Override
    public RenderType getRenderType(Scp215Phantom Phantom, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return Phantom.fade(PartialTick) < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(Scp215Phantom Phantom, float PartialTick, int PackedLight) {
        float Stepped = Math.round(Phantom.fade(PartialTick) * AlphaSteps) / AlphaSteps;
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Math.max(0.0f, Stepped));
    }

    @Override
    protected int getBlockLightLevel(Scp215Phantom Phantom, BlockPos Pos) {
        return Math.max(MinimumLight, super.getBlockLightLevel(Phantom, Pos));
    }

    @Override
    protected void applyRotations(Scp215Phantom Phantom, PoseStack Matrices, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Matrices.mulPose(Axis.YP.rotationDegrees(180.0f - Phantom.bodyYaw(PartialTick)));
    }
}
