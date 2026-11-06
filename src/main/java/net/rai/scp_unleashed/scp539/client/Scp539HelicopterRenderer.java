package net.rai.scp_unleashed.scp539.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp539.Scp539Helicopter;

public class Scp539HelicopterRenderer extends SharedGeoRenderer<Scp539Helicopter> {
    public static final float Pivot = 1.1f;
    public static final float Wobble = 2.5f;
    public static final float Shadow = 1.4f;

    public Scp539HelicopterRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp539HelicopterGeoModel());
        shadowRadius = Shadow;
        shadowStrength = 0.6f;
    }

    @Override
    protected void applyRotations(Scp539Helicopter Craft, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        float Yaw = Mth.rotLerp(PartialTick, Craft.yRotO, Craft.getYRot());
        float Pitch = Mth.lerp(PartialTick, Craft.xRotO, Craft.getXRot());
        float Age = Craft.tickCount + PartialTick;
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Yaw));
        Pose.translate(0.0f, Pivot, 0.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(-Pitch));
        Pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(Age * 0.9f) * Wobble));
        Pose.translate(0.0f, -Pivot, 0.0f);
    }
}
