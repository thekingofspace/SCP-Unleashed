package net.rai.scp_unleashed.z0004.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.z0004.Z0004Cannonball;
import net.rai.scp_unleashed.z0004.Z0004Registry;

public class Z0004CannonballGeoModel extends SharedGeoModel<Z0004Cannonball> {
    public static final ResourceLocation ModelPath = Z0004Registry.location("geo/entity/z0004/z0004_cannonball.geo.json");
    public static final ResourceLocation TexturePath = Z0004Registry.location("textures/entity/z0004/z0004_cannonball.png");
    public static final ResourceLocation AnimationPath = Z0004Registry.location("animations/entity/z0004/z0004_cannonball.animation.json");

    @Override
    public ResourceLocation getModelResource(Z0004Cannonball Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Z0004Cannonball Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Z0004Cannonball Owner) {
        return AnimationPath;
    }
}
