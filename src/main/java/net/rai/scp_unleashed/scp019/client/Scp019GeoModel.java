package net.rai.scp_unleashed.scp019.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp019.Scp019Creature;
import net.rai.scp_unleashed.scp019.Scp019GaitController;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp019GeoModel<T extends Scp019Creature> extends GeoModel<T> {
    public static final String LookBone = "look";
    public static final String RootBone = "root";
    public static final String BodyController = "body";
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;
    private final float PitchLimit;
    private final float YawLimit;

    public Scp019GeoModel(String Name, float PitchLimit, float YawLimit) {
        this.ModelPath = location("geo/entity/scp019/" + Name + ".geo.json");
        this.TexturePath = location("textures/entity/scp019/" + Name + ".png");
        this.AnimationPath = location("animations/entity/scp019/" + Name + ".animation.json");
        this.PitchLimit = PitchLimit;
        this.YawLimit = YawLimit;
    }

    public static boolean readyToDraw(Scp019Creature Creature, long InstanceId) {
        if (!Creature.locksYaw()) {
            return true;
        }
        String Clip = Creature.actionClip();
        AnimationController<Scp019Creature> Controller = Creature.getAnimatableInstanceCache().<Scp019Creature>getManagerForId(InstanceId).getAnimationControllers().get(BodyController);
        return Clip != null && Controller instanceof Scp019GaitController<Scp019Creature> Gait && Gait.playing(Clip);
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(T Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(T Creature, long InstanceId, AnimationState<T> Animated) {
        GeoBone Root = getAnimationProcessor().getBone(RootBone);
        if (Root != null) {
            Root.setHidden(!readyToDraw(Creature, InstanceId));
        }
        if (Creature.locksMovement()) {
            return;
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
    }
}
