package net.rai.scp_unleashed.scp513.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp513.Scp513Stalker;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp513StalkerRenderer extends GeoEntityRenderer<Scp513Stalker> {
    public static final int MinimumLight = 3;

    public Scp513StalkerRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp513StalkerModel());
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp513Stalker Stalker, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        if (Stalker.fade(PartialTick) <= 0.0f) {
            return;
        }
        super.render(Stalker, Yaw, PartialTick, Matrices, Buffers, Light);
    }

    @Override
    public RenderType getRenderType(Scp513Stalker Stalker, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return Stalker.fade(PartialTick) < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(Scp513Stalker Stalker, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Math.max(0.0f, Stalker.fade(PartialTick)));
    }

    @Override
    protected int getBlockLightLevel(Scp513Stalker Stalker, BlockPos Pos) {
        return Math.max(MinimumLight, super.getBlockLightLevel(Stalker, Pos));
    }

    @Override
    protected void applyRotations(Scp513Stalker Stalker, PoseStack Matrices, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        Matrices.mulPose(Axis.YP.rotationDegrees(180.0f - Stalker.bodyYaw(PartialTick)));
    }
}
