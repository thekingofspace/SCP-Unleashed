package net.rai.scp_unleashed.scp352.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp352.Scp352Crone;
import net.rai.scp_unleashed.scp352.Scp352Phase;
import net.rai.scp_unleashed.scp352.Scp352Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp352GeoModel extends GeoModel<Scp352Crone> {
    public static final ResourceLocation ModelPath = Scp352Registry.location("geo/entity/scp352/baba_yaga.geo.json");
    public static final ResourceLocation TexturePath = Scp352Registry.location("textures/entity/scp352/baba_yaga.png");
    public static final ResourceLocation AnimationPath = Scp352Registry.location("animations/entity/scp352/baba_yaga.animation.json");
    public static final String LookBone = "look";
    public static final float PitchLimit = 22.0f;
    public static final float YawLimit = 40.0f;

    @Override
    public ResourceLocation getModelResource(Scp352Crone Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp352Crone Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp352Crone Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp352Crone Creature, long InstanceId, AnimationState<Scp352Crone> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null || Creature.isDeadOrDying()) {
            return;
        }
        Scp352Phase Phase = Creature.phase();
        boolean Free = (Phase == Scp352Phase.Wander || Phase.tall()) && Creature.action() == Scp352Crone.ActionNone;
        if (Free) {
            Look.setRotX(-Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
            Look.setRotY(-Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
        }
    }
}
