package net.rai.scp_unleashed.scp439.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp439CreatureModel<T extends Scp439Creature> extends GeoModel<T> {
    public static final String LookBone = "look";
    public static final float PitchLimit = 25.0f;
    public static final float YawLimit = 40.0f;
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;

    public Scp439CreatureModel(String Name) {
        ModelPath = Scp439Registry.location("geo/entity/scp439/" + Name + ".geo.json");
        TexturePath = Scp439Registry.location("textures/entity/scp439/" + Name + ".png");
        AnimationPath = Scp439Registry.location("animations/entity/scp439/" + Name + ".animation.json");
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
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look != null && Data != null && !Creature.isActing() && Creature.deathTime <= 0) {
            Look.setRotX(-Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
            Look.setRotY(-Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
        }
    }
}
