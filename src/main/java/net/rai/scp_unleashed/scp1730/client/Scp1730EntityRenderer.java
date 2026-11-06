package net.rai.scp_unleashed.scp1730.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp1730EntityRenderer<T extends Entity & GeoAnimatable> extends SharedGeoRenderer<T> {
    public Scp1730EntityRenderer(EntityRendererProvider.Context Context, String Name, String LookBone, float Scale, float Shadow, boolean Glowing) {
        super(Context, new Scp1730EntityModel<>(Name, LookBone));
        withScale(Scale);
        shadowRadius = Shadow;
        if (Glowing) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }

    @Override
    protected void applyRotations(T Owner, PoseStack Pose, float AgeInTicks, float RotationYaw, float PartialTick, float NativeScale) {
        if (Owner instanceof LivingEntity) {
            super.applyRotations(Owner, Pose, AgeInTicks, RotationYaw, PartialTick, NativeScale);
            return;
        }
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Mth.rotLerp(PartialTick, Owner.yRotO, Owner.getYRot())));
    }

    @Override
    protected float getDeathMaxRotation(T Owner) {
        return 0.0f;
    }

    @Override
    public boolean shouldRender(T Owner, net.minecraft.client.renderer.culling.Frustum View, double CamX, double CamY, double CamZ) {
        return Owner.shouldRender(CamX, CamY, CamZ) && (Owner.noCulling || View.isVisible(Owner.getBoundingBoxForCulling().inflate(10.0)));
    }
}
