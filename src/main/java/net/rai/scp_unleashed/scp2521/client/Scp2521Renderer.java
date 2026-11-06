package net.rai.scp_unleashed.scp2521.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp2521.Scp2521;
import net.rai.scp_unleashed.scp2521.Scp2521Motion;
import net.rai.scp_unleashed.scp2521.Scp2521Void;

public class Scp2521Renderer extends SharedGeoRenderer<Scp2521> {
    public static final float Scale = 1.7f;
    public static final float Lift = 15.0f / 16.0f;
    public static final float ShadowRadius = 0.35f;
    public static final float SandSeconds = 4.2f;
    public static final float SandRise = 1.7f;

    public Scp2521Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp2521GeoModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
        addRenderLayer(new Scp2521GlowLayer(this));
    }

    @Override
    public void render(Scp2521 Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        float Squash = Owner.squash();
        scaleWidth = Scale * Squash;
        scaleHeight = Scale * Squash;
        int Used = Scp2521Void.inside(Owner.level()) ? LightTexture.FULL_BRIGHT : Light;
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Used);
        sand(Owner, PartialTick, Pose, Buffers, Light);
    }

    private void sand(Scp2521 Owner, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        BlockState Sand = Owner.sandState();
        if (Sand == null || Owner.mode() != Scp2521.ModeHolding) {
            return;
        }
        float Seconds = Owner.modeSeconds(PartialTick);
        float Left = 1.0f - Scp2521Motion.ease(Seconds / SandSeconds);
        if (Left <= 0.02f) {
            return;
        }
        Pose.pushPose();
        Pose.translate(-0.5f * Left, (1.0f - Left) * SandRise, -0.5f * Left);
        Pose.scale(Left, Left, Left);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Sand, Pose, Buffers, Light, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
    }

    @Override
    protected void applyRotations(Scp2521 Owner, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        super.applyRotations(Owner, Pose, AgeInTicks, RotationYaw, PartialTick, NativeScale);
        Pose.translate(0.0f, Lift, 0.0f);
    }

    @Override
    protected float getDeathMaxRotation(Scp2521 Owner) {
        return 0.0f;
    }
}
