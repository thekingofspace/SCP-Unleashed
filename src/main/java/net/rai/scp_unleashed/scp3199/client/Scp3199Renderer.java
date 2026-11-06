package net.rai.scp_unleashed.scp3199.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp3199.Scp3199;

public class Scp3199Renderer extends SharedGeoRenderer<Scp3199> {
    public static final float Scale = 0.8f;
    public static final float ShadowRadius = 0.45f;
    public static final float SwimTilt = 0.5f;

    public Scp3199Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp3199GeoModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
        addRenderLayer(new Scp3199GlowLayer(this));
    }

    @Override
    public void render(Scp3199 Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        float Grown = Owner.growthScale();
        scaleWidth = Scale * Grown;
        scaleHeight = Scale * Grown;
        shadowRadius = ShadowRadius * Grown;
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected void applyRotations(Scp3199 Owner, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        super.applyRotations(Owner, Pose, AgeInTicks, RotationYaw, PartialTick, NativeScale);
        if (Owner.isSwimmingNow()) {
            Pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(PartialTick, Owner.xRotO, Owner.getXRot()) * SwimTilt));
        }
    }

    @Override
    protected float getDeathMaxRotation(Scp3199 Owner) {
        return 0.0f;
    }
}
