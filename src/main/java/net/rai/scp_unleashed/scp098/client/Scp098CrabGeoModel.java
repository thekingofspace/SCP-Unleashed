package net.rai.scp_unleashed.scp098.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp098.Scp098Crab;
import net.rai.scp_unleashed.scp098.Scp098GaitController;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp098CrabGeoModel extends GeoModel<Scp098Crab> {
    public static final String RootBone = "root";
    public static final String BodyController = "body";
    public static final String Name = "scp098_crab";
    private static final ResourceLocation ModelPath = location("geo/entity/scp098/" + Name + ".geo.json");
    private static final ResourceLocation RipeTexture = location("textures/entity/scp098/" + Name + ".png");
    private static final ResourceLocation FadedTexture = location("textures/entity/scp098/" + Name + "_faded.png");
    private static final ResourceLocation PaleTexture = location("textures/entity/scp098/" + Name + "_pale.png");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp098/" + Name + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static boolean readyToDraw(Scp098Crab Creature, long InstanceId) {
        byte Action = Creature.action();
        String Clip = Action == Scp098Crab.ActionEmerge ? "emerge" : Action == Scp098Crab.ActionSurface ? "surface" : null;
        if (Clip == null) {
            return true;
        }
        AnimationController<Scp098Crab> Controller = Creature.getAnimatableInstanceCache().<Scp098Crab>getManagerForId(InstanceId).getAnimationControllers().get(BodyController);
        return Controller instanceof Scp098GaitController Gait && Gait.playing(Clip);
    }

    @Override
    public ResourceLocation getModelResource(Scp098Crab Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp098Crab Creature) {
        float Ripeness = Creature.ripeness();
        if (Ripeness < Scp098Crab.PaleBelow) {
            return PaleTexture;
        }
        return Ripeness < Scp098Crab.FadedBelow ? FadedTexture : RipeTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp098Crab Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp098Crab Creature, long InstanceId, AnimationState<Scp098Crab> Animated) {
        GeoBone Root = getAnimationProcessor().getBone(RootBone);
        if (Root != null) {
            Root.setHidden(!readyToDraw(Creature, InstanceId));
        }
    }
}
