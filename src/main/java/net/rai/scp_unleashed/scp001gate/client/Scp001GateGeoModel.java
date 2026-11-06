package net.rai.scp_unleashed.scp001gate.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp001gate.Scp001GateGuardian;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp001GateGeoModel extends GeoModel<Scp001GateGuardian> {
    public static final ResourceLocation ModelPath = Scp001GateRegistry.location("geo/entity/scp001_gate/scp001_gate_guardian.geo.json");
    public static final ResourceLocation TexturePath = Scp001GateRegistry.location("textures/entity/scp001_gate/scp001_gate_guardian.png");
    public static final ResourceLocation AnimationPath = Scp001GateRegistry.location("animations/entity/scp001_gate/scp001_gate_guardian.animation.json");
    public static final String[] Tracked = {"sword", "head", "hand_r"};

    @Override
    public ResourceLocation getModelResource(Scp001GateGuardian Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp001GateGuardian Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp001GateGuardian Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp001GateGuardian Owner, long InstanceId, AnimationState<Scp001GateGuardian> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        for (String Name : Tracked) {
            GeoBone Found = getAnimationProcessor().getBone(Name);
            if (Found != null) {
                Found.setTrackingMatrices(true);
            }
        }
        GeoBone Head = getAnimationProcessor().getBone("head");
        if (Head == null) {
            return;
        }
        float Partial = Animated.getPartialTick();
        Head.setRotY(Head.getInitialSnapshot().getRotY() + Owner.gazeYaw(Partial) * Mth.DEG_TO_RAD);
        Head.setRotX(Head.getInitialSnapshot().getRotX() - Owner.gazePitch(Partial) * Mth.DEG_TO_RAD);
    }
}
