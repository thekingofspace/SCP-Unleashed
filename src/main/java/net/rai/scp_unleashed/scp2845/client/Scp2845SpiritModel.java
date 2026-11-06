package net.rai.scp_unleashed.scp2845.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;
import net.rai.scp_unleashed.scp2845.Scp2845Spirit;
import software.bernie.geckolib.animation.AnimationState;

public class Scp2845SpiritModel extends SharedGeoModel<Scp2845Spirit> {
    public static final ResourceLocation TexturePath = Scp2845Registry.location("textures/entity/scp2845/scp2845_spirit.png");

    @Override
    public ResourceLocation getModelResource(Scp2845Spirit Owner) {
        return Scp2845Model.ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp2845Spirit Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp2845Spirit Owner) {
        return Scp2845Model.AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp2845Spirit Owner, long InstanceId, AnimationState<Scp2845Spirit> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        Scp2845Orbit.apply(getAnimationProcessor(), Owner.tickCount / 20.0f + (Owner.getId() % 53) * 0.41f);
    }
}
