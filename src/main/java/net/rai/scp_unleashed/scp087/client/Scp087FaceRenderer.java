package net.rai.scp_unleashed.scp087.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp087.Scp087Face;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp087FaceRenderer extends GeoEntityRenderer<Scp087Face> {
    public static final float Scale = 1.45f;

    public Scp087FaceRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp087FaceModel());
        addRenderLayer(new Scp087FaceGlowLayer(this));
        withScale(Scale);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp087Face Face, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (Face.fade(PartialTick) <= 0.01f) {
            return;
        }
        super.render(Face, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public RenderType getRenderType(Scp087Face Face, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return Face.fade(PartialTick) < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(Scp087Face Face, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Math.max(0.0f, Face.fade(PartialTick)));
    }

    @Override
    protected void applyRotations(Scp087Face Face, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Scp087FaceModel.gazeYaw(Face, PartialTick)));
    }
}
