package net.rai.scp_unleashed.scp354.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import net.rai.scp_unleashed.scp354.Scp354StageController;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Predicate;

public class Scp354GeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    public static final String RootBone = "root";
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;
    private final String Controller;
    private final String Opening;
    private final Predicate<T> Opens;

    public Scp354GeoModel(String Name, String Controller, String Opening, Predicate<T> Opens) {
        this.ModelPath = Scp354Registry.location("geo/entity/scp354/" + Name + ".geo.json");
        this.TexturePath = Scp354Registry.location("textures/entity/scp354/" + Name + ".png");
        this.AnimationPath = Scp354Registry.location("animations/entity/scp354/" + Name + ".animation.json");
        this.Controller = Controller;
        this.Opening = Opening;
        this.Opens = Opens;
    }

    @Override
    public ResourceLocation getModelResource(T Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Owner) {
        return AnimationPath;
    }

    public boolean readyToDraw(T Owner, long InstanceId) {
        if (!Opens.test(Owner)) {
            return true;
        }
        AnimationController<T> Found = Owner.getAnimatableInstanceCache().<T>getManagerForId(InstanceId).getAnimationControllers().get(Controller);
        return Found instanceof Scp354StageController<T> Stage && Stage.playing(Opening);
    }

    @Override
    public void setCustomAnimations(T Owner, long InstanceId, AnimationState<T> Animated) {
        GeoBone Root = getAnimationProcessor().getBone(RootBone);
        if (Root != null) {
            Root.setHidden(!readyToDraw(Owner, InstanceId));
        }
    }
}
