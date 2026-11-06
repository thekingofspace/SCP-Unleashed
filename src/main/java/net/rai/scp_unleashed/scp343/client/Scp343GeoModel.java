package net.rai.scp_unleashed.scp343.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.HumanoidGait;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp343.Scp343God;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp343GeoModel extends DClassGeoModel<Scp343God> {
    public static final ResourceLocation GeoPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp343/scp343.geo.json");
    public static final ResourceLocation ClipPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/scp343/scp343.animation.json");
    public static final ResourceLocation SkinPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp343/scp343.png");
    public static final ResourceLocation GlowPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp343/scp343_glowmask.png");
    public static final float YawLimit = 25.0f;
    public static final float SeatedYawLimit = 15.0f;
    public static final float DownLimit = 20.0f;
    public static final float UpLimit = 15.0f;
    public static final float BeardFollow = 0.9f;
    public static final float BeardSway = 2.5f;
    public static final float MantleSway = 3.0f;
    public static final float SwaySpeed = 0.08f;

    @Override
    public ResourceLocation getModelResource(Scp343God Owner) {
        return GeoPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp343God Owner) {
        return SkinPath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp343God Owner) {
        return ClipPath;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp343God Owner, long InstanceId, AnimationState<Scp343God> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Head = part("head");
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        boolean Seated = Owner.isSittingPose();
        if (Head != null && Data != null) {
            float Yaw = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -75.0f, 75.0f);
            float Pitch = Mth.clamp(Data.headPitch(), -50.0f, 50.0f);
            float YawLimitNow = Seated ? SeatedYawLimit : YawLimit;
            float WantedYaw = Mth.clamp(Yaw, -YawLimitNow, YawLimitNow);
            float WantedPitch = Mth.clamp(Pitch, -DownLimit, UpLimit);
            Head.setRotY(Head.getRotY() + (WantedYaw - Yaw) * Mth.DEG_TO_RAD);
            Head.setRotX(Head.getRotX() + (WantedPitch - Pitch) * Mth.DEG_TO_RAD);
        }
        HumanoidGait Gait = Owner.gait();
        double Phase = Gait == null ? 0.0 : Gait.cycle() * Mth.TWO_PI;
        float Moving = Gait == null || !Gait.striding() ? 0.0f : Gait.running() ? 1.6f : 1.0f;
        GeoBone Beard = part("beard");
        if (Beard != null && Head != null) {
            float SwayX = (float) Math.sin(Phase * 2.0) * BeardSway * Moving * 0.5f;
            float SwayZ = (float) Math.sin(Phase) * BeardSway * Moving * 0.4f;
            Beard.setRotX(-BeardFollow * Head.getRotX() - SwayX * Mth.DEG_TO_RAD);
            Beard.setRotY(-BeardFollow * Head.getRotY());
            Beard.setRotZ(SwayZ * Mth.DEG_TO_RAD);
        }
        GeoBone Mantle = part("hair_mantle");
        if (Mantle != null) {
            if (Seated) {
                Mantle.setRotX(0.0f);
                Mantle.setRotZ(0.0f);
            } else {
                float Lift = (float) Math.abs(Math.sin(Phase)) * MantleSway * Moving + 1.5f * Moving;
                Mantle.setRotX(-Lift * Mth.DEG_TO_RAD);
                Mantle.setRotZ((float) Math.sin(Phase) * MantleSway * 0.5f * Moving * Mth.DEG_TO_RAD);
            }
        }
        GeoBone Front = part("robe_front");
        GeoBone Back = part("robe_back");
        GeoBone Right = part("right_leg");
        GeoBone Left = part("left_leg");
        if (Front == null || Back == null || Right == null || Left == null) {
            return;
        }
        Front.setHidden(Seated);
        Back.setHidden(Seated);
        if (Seated) {
            return;
        }
        Front.setRotX(Math.max(Right.getRotX(), Left.getRotX()));
        Back.setRotX(Math.min(Right.getRotX(), Left.getRotX()));
        Front.setRotY(0.0f);
        Back.setRotY(0.0f);
        Front.setRotZ(0.0f);
        Back.setRotZ(0.0f);
    }
}
