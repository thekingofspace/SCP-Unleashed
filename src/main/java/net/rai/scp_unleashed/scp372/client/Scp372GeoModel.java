package net.rai.scp_unleashed.scp372.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp372.Scp372Insect;
import net.rai.scp_unleashed.scp372.Scp372Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp372GeoModel extends GeoModel<Scp372Insect> {
    public static final ResourceLocation ModelPath = Scp372Registry.location("geo/entity/scp372/scp372.geo.json");
    public static final ResourceLocation TexturePath = Scp372Registry.location("textures/entity/scp372/scp372.png");
    public static final ResourceLocation AnimationPath = Scp372Registry.location("animations/entity/scp372/scp372.animation.json");
    public static final String HeadBone = "head";
    public static final float YawLimit = 28.0f;
    public static final float PitchLimit = 18.0f;
    public static final float LookShare = 0.6f;

    @Override
    public ResourceLocation getModelResource(Scp372Insect Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp372Insect Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp372Insect Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp372Insect Creature, long InstanceId, AnimationState<Scp372Insect> Animated) {
        GeoBone Head = getAnimationProcessor().getBone(HeadBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Head == null || Data == null || Creature.isActing()) {
            return;
        }
        Head.setRotY(Head.getRotY() + Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * LookShare * Mth.DEG_TO_RAD);
        Head.setRotX(Head.getRotX() + Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * LookShare * Mth.DEG_TO_RAD);
    }
}
