package net.rai.scp_unleashed.scp3008.staff.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp3008StaffGeoModel extends SharedGeoModel<Scp3008Staff> {
    public static final String LookBone = "look";
    public static final float LookUp = 35.0f;
    public static final float LookDown = 35.0f;
    public static final float LookTurn = 55.0f;

    @Override
    public ResourceLocation getModelResource(Scp3008Staff Staff) {
        return Staff.variant().model();
    }

    @Override
    public ResourceLocation getTextureResource(Scp3008Staff Staff) {
        return Staff.variant().texture();
    }

    @Override
    public ResourceLocation getAnimationResource(Scp3008Staff Staff) {
        return Staff.variant().animations();
    }

    @Override
    public void setCustomAnimations(Scp3008Staff Staff, long InstanceId, AnimationState<Scp3008Staff> Animated) {
        super.setCustomAnimations(Staff, InstanceId, Animated);
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look == null) {
            return;
        }
        float Pitch = 0.0f;
        float Yaw = 0.0f;
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data != null && Staff.isAlive()) {
            Pitch = Mth.clamp(Data.headPitch(), -LookUp, LookDown);
            Yaw = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -LookTurn, LookTurn);
        }
        Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
        Look.setRotY(Look.getInitialSnapshot().getRotY() - Yaw * Mth.DEG_TO_RAD);
    }
}
