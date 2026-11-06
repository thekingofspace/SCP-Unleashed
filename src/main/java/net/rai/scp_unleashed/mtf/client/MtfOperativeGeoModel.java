package net.rai.scp_unleashed.mtf.client;

import net.minecraft.util.Mth;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.mtf.MtfOperative;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class MtfOperativeGeoModel extends DClassGeoModel<MtfOperative> {
    public static final float[] RopeRight = {2.95f, 0.05f, -0.16f};
    public static final float[] RopeLeft = {2.62f, -0.05f, 0.16f};
    public static final float[] RopeRightLeg = {0.42f, 0.0f, 0.06f};
    public static final float[] RopeLeftLeg = {0.12f, 0.0f, -0.08f};
    public static final float RopeSway = 0.05f;

    @Nullable
    private GeoBone find(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    private void put(String Name, float X, float Y, float Z) {
        GeoBone Found = find(Name);
        if (Found != null) {
            Found.setRotX(X);
            Found.setRotY(Y);
            Found.setRotZ(Z);
        }
    }

    private void put(String Name, float[] Rotation) {
        put(Name, Rotation[0], Rotation[1], Rotation[2]);
    }

    @Override
    public void setCustomAnimations(MtfOperative Owner, long InstanceId, AnimationState<MtfOperative> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        if (Owner.isDeadOrDying()) {
            return;
        }
        float Partial = Animated.getPartialTick();
        if (Owner.isRoping()) {
            float Sway = Mth.sin((Owner.tickCount + Partial) * 0.35f) * RopeSway;
            put("right_arm", RopeRight[0] + Sway, RopeRight[1], RopeRight[2]);
            put("left_arm", RopeLeft[0] - Sway, RopeLeft[1], RopeLeft[2]);
            put("right_leg", RopeRightLeg);
            put("left_leg", RopeLeftLeg);
            put("body", 0.0f, 0.0f, 0.0f);
            return;
        }
    }

    @Override
    protected boolean gunAiming(DClassLike Owner) {
        return Owner instanceof MtfOperative Operative && Operative.isAiming() || super.gunAiming(Owner);
    }
}
