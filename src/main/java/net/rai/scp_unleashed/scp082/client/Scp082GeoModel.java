package net.rai.scp_unleashed.scp082.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp082.Scp082Fernand;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp082GeoModel extends GeoModel<Scp082Fernand> {
    public static final ResourceLocation ModelPath = location("geo/entity/scp082/fernand.geo.json");
    public static final ResourceLocation TexturePath = location("textures/entity/scp082/fernand.png");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp082/fernand.animation.json");
    public static final String LookBone = "look";

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp082Fernand Fernand) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp082Fernand Fernand) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp082Fernand Fernand) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp082Fernand Fernand, long InstanceId, AnimationState<Scp082Fernand> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null || Fernand.isDeadOrDying() || Fernand.action() != Scp082Fernand.ActionNone) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -Scp082Fernand.PitchLimit, Scp082Fernand.PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -Scp082Fernand.YawLimit, Scp082Fernand.YawLimit) * Mth.DEG_TO_RAD);
    }
}
