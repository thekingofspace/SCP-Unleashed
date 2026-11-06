package net.rai.scp_unleashed.scp682.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp682.Scp682;

public class Scp682Renderer extends SharedGeoRenderer<Scp682> {
    public static final float Scale = 4.72f;
    public static final float ShadowRadius = 3.2f;
    public static final float Centre = 9.0f;
    public static final float SwimTilt = 0.6f;

    public Scp682Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp682GeoModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
        addRenderLayer(new Scp682OverlayLayer(this));
        addRenderLayer(new Scp682CommandLayer(this));
    }

    @Override
    public void render(Scp682 Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        float Size = Scale * Owner.shownScale(PartialTick);
        scaleWidth = Size;
        scaleHeight = Size;
        shadowRadius = ShadowRadius * Owner.shownScale(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected void applyRotations(Scp682 Owner, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        super.applyRotations(Owner, Pose, AgeInTicks, RotationYaw, PartialTick, NativeScale);
        if (Owner.isSwimmingNow()) {
            Pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(PartialTick, Owner.xRotO, Owner.getXRot()) * SwimTilt));
        }
        Pose.translate(0.0f, 0.0f, -Centre / 16.0f);
    }
}
