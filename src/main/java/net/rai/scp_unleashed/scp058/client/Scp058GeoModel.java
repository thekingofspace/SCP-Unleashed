package net.rai.scp_unleashed.scp058.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp058.Scp058GaitController;
import net.rai.scp_unleashed.scp058.Scp058Heart;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp058GeoModel extends GeoModel<Scp058Heart> {
    public static final String RootBone = "root";
    public static final String HeartBone = "heart";
    public static final String BodyController = "body";
    public static final String Name = "scp058_heart";
    private static final ResourceLocation ModelPath = location("geo/entity/scp058/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = location("textures/entity/scp058/" + Name + ".png");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp058/" + Name + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static boolean readyToDraw(Scp058Heart Creature, long InstanceId) {
        if (!Creature.isEmerging()) {
            return true;
        }
        AnimationController<Scp058Heart> Controller = Creature.getAnimatableInstanceCache().<Scp058Heart>getManagerForId(InstanceId).getAnimationControllers().get(BodyController);
        return Controller instanceof Scp058GaitController Gait && Gait.playing("emerge");
    }

    @Override
    public ResourceLocation getModelResource(Scp058Heart Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp058Heart Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp058Heart Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp058Heart Creature, long InstanceId, AnimationState<Scp058Heart> Animated) {
        GeoBone Root = getAnimationProcessor().getBone(RootBone);
        if (Root != null) {
            Root.setHidden(!readyToDraw(Creature, InstanceId));
        }
        GeoBone Heart = getAnimationProcessor().getBone(HeartBone);
        if (Heart == null) {
            return;
        }
        float Scale = Creature.pulse(Animated.getPartialTick());
        float Across = Scp058Heart.pulseAcross(Scale);
        Heart.setScaleX(Across);
        Heart.setScaleY(Across);
        Heart.setScaleZ(Scale);
    }
}
