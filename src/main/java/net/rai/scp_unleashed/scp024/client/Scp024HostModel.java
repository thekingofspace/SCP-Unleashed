package net.rai.scp_unleashed.scp024.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp024.Scp024Host;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp024HostModel extends GeoModel<Scp024Host> {
    public static final String LookBone = "look";
    public static final ResourceLocation Model = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp024/host.geo.json");
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp024/host.png");
    public static final ResourceLocation Animations = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/scp024/host.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp024Host Figure) {
        return Model;
    }

    @Override
    public ResourceLocation getTextureResource(Scp024Host Figure) {
        return Texture;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp024Host Figure) {
        return Animations;
    }

    @Override
    public void setCustomAnimations(Scp024Host Figure, long InstanceId, AnimationState<Scp024Host> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -25.0f, 25.0f) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -50.0f, 50.0f) * Mth.DEG_TO_RAD);
    }
}
