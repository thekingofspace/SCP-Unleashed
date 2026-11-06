package net.rai.scp_unleashed.scp323.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp323.Scp323Beast;
import net.rai.scp_unleashed.scp323.Scp323Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp323BeastModel extends GeoModel<Scp323Beast> {
    public static final ResourceLocation ModelPath = Scp323Registry.location("geo/entity/scp323/beast.geo.json");
    public static final ResourceLocation TexturePath = Scp323Registry.location("textures/entity/scp323/beast.png");
    public static final ResourceLocation AnimationPath = Scp323Registry.location("animations/entity/scp323/beast.animation.json");
    public static final String LookBone = "look";
    public static final float PitchLimit = 35.0f;
    public static final float YawLimit = 55.0f;

    @Override
    public ResourceLocation getModelResource(Scp323Beast Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp323Beast Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp323Beast Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp323Beast Creature, long InstanceId, AnimationState<Scp323Beast> Animated) {
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look != null && Data != null && !Creature.busy() && !Creature.isDeadOrDying()) {
            Look.setRotX(-Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
            Look.setRotY(-Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
        }
    }
}
