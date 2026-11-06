package net.rai.scp_unleashed.scp106.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp106.Scp106OldMan;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp106GeoModel extends GeoModel<Scp106OldMan> {
    public static final String Name = "scp106_old_man";
    public static final float HeadYawLimit = 55.0f;
    public static final float HeadPitchLimit = 15.0f;
    public static final float TiltAngle = 5.0f;
    private static final ResourceLocation ModelPath = location("geo/entity/scp106/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = location("textures/entity/scp106/" + Name + ".png");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp106/" + Name + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp106OldMan Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp106OldMan Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp106OldMan Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp106OldMan Creature, long InstanceId, AnimationState<Scp106OldMan> Animated) {
        GeoBone Head = getAnimationProcessor().getBone("head");
        if (Head == null || Creature.isDeadOrDying()) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        byte Action = Creature.action();
        if (Data != null && (Action == Scp106OldMan.ActionNone || Action == Scp106OldMan.ActionRise)) {
            float Pitch = Mth.clamp(Data.headPitch(), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD;
            float Yaw = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD;
            Head.setRotX(Head.getRotX() + Pitch);
            Head.setRotY(Head.getRotY() + Yaw);
        }
        float Time = Creature.tickCount + Animated.getPartialTick();
        Head.setRotZ(Head.getRotZ() + Mth.sin(Time * 0.035f + Creature.getId()) * TiltAngle * Mth.DEG_TO_RAD);
    }
}
