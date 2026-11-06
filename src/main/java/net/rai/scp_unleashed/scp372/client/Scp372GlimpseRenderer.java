package net.rai.scp_unleashed.scp372.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp372.Scp372Glimpse;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp372GlimpseRenderer extends GeoEntityRenderer<Scp372Glimpse> {
    public static final int MinimumLight = 3;
    public static final float AlphaSteps = 8.0f;
    public static final float LeapAlpha = 0.85f;
    public static final float LeapFade = 0.55f;
    public static final float[] GhostAlphas = {0.45f, 0.28f, 0.14f};
    private float Alpha = 1.0f;

    public Scp372GlimpseRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp372GlimpseModel());
        withScale(Scp372Renderer.RenderScale);
        shadowRadius = 0.0f;
        addRenderLayer(new Scp372GlowLayer<>(this, Glimpse -> Alpha >= 0.999f));
    }

    @Override
    public void render(Scp372Glimpse Glimpse, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (Glimpse.isLeaping()) {
            float Progress = Glimpse.leapProgress(PartialTick);
            Vec3 Here = Glimpse.getPosition(PartialTick);
            for (int Index = Scp372Glimpse.Trail - 1; Index >= 0; Index--) {
                Vec3 Offset = Glimpse.trail(Index).subtract(Here);
                Pose.pushPose();
                Pose.translate(Offset.x, Offset.y, Offset.z);
                Alpha = GhostAlphas[Index] * (1.0f - Progress * LeapFade);
                super.render(Glimpse, Yaw, PartialTick, Pose, Buffers, Light);
                Pose.popPose();
            }
            Alpha = LeapAlpha * (1.0f - Progress * LeapFade);
        } else {
            Alpha = 1.0f;
        }
        super.render(Glimpse, Yaw, PartialTick, Pose, Buffers, Light);
        Alpha = 1.0f;
    }

    @Override
    public RenderType getRenderType(Scp372Glimpse Glimpse, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return Alpha < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(Scp372Glimpse Glimpse, float PartialTick, int PackedLight) {
        float Stepped = Math.round(Alpha * AlphaSteps) / AlphaSteps;
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Math.max(0.0f, Stepped));
    }

    @Override
    protected int getBlockLightLevel(Scp372Glimpse Glimpse, BlockPos Pos) {
        return Math.max(MinimumLight, super.getBlockLightLevel(Glimpse, Pos));
    }

    @Override
    protected void applyRotations(Scp372Glimpse Glimpse, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Vec3 Up = Glimpse.normal();
        Vec3 Ahead = Glimpse.heading();
        Vec3 Across = Ahead.cross(Up);
        Matrix3f Basis = new Matrix3f((float) Across.x, (float) Across.y, (float) Across.z, (float) Up.x, (float) Up.y, (float) Up.z,
                (float) -Ahead.x, (float) -Ahead.y, (float) -Ahead.z);
        Pose.mulPose(new Quaternionf().setFromNormalized(Basis));
    }
}
