package net.rai.scp_unleashed.z0002.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.z0002.Z0002Registry;
import net.rai.scp_unleashed.z0002.Z0002Shrine;
import software.bernie.geckolib.model.GeoModel;

public class Z0002ShrineModel extends GeoModel<Z0002Shrine> {
    public static final ResourceLocation ModelPath = Z0002Registry.location("geo/entity/z0002/shrine.geo.json");
    public static final ResourceLocation TexturePath = Z0002Registry.location("textures/entity/z0002/shrine.png");
    public static final ResourceLocation AnimationPath = Z0002Registry.location("animations/entity/z0002/shrine.animation.json");

    @Override
    public ResourceLocation getModelResource(Z0002Shrine Shrine) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Z0002Shrine Shrine) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Z0002Shrine Shrine) {
        return AnimationPath;
    }
}
