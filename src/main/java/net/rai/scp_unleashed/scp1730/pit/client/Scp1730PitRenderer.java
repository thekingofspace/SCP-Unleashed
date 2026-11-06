package net.rai.scp_unleashed.scp1730.pit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp1730.pit.Scp1730PitTongue;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp1730PitRenderer<T extends Entity & GeoAnimatable> extends SharedGeoRenderer<T> {
    private final float Scale;
    private final float Lift;
    private final float Back;
    private final double Cull;

    public Scp1730PitRenderer(EntityRendererProvider.Context Context, String Name, int Stages, float Scale, float Shadow, boolean Glowing, float Lift, float Back, double Cull) {
        super(Context, new Scp1730PitModel<>(Name, Stages));
        this.Scale = Scale;
        this.Lift = Lift;
        this.Back = Back;
        this.Cull = Cull;
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
        } else {
            Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Mth.rotLerp(PartialTick, Owner.yRotO, Owner.getYRot())));
            if (Owner instanceof Scp1730PitTongue) {
                Pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(PartialTick, Owner.xRotO, Owner.getXRot())));
            }
        }
        if (Lift != 0.0f || Back != 0.0f) {
            Pose.translate(0.0f, Lift / Scale, Back / Scale);
        }
    }

    @Override
    protected float getDeathMaxRotation(T Owner) {
        return 0.0f;
    }

    @Override
    public int getPackedOverlay(T Owner, float U, float PartialTick) {
        if (Owner instanceof LivingEntity Living && Living.deathTime > 0) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(Owner, U, PartialTick);
    }

    @Override
    public boolean shouldRender(T Owner, Frustum View, double CamX, double CamY, double CamZ) {
        return Owner.shouldRender(CamX, CamY, CamZ) && (Owner.noCulling || View.isVisible(Owner.getBoundingBoxForCulling().inflate(Cull)));
    }
}
