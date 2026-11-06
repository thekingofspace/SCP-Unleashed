package net.rai.scp_unleashed.scp548.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp548.Scp548Registry;
import net.rai.scp_unleashed.scp548.Scp548Spider;

public class Scp548GeoModel extends SharedGeoModel<Scp548Spider> {
    public static final ResourceLocation ModelPath = Scp548Registry.location("geo/entity/scp548/scp548_ice_spider.geo.json");
    public static final ResourceLocation TexturePath = Scp548Registry.location("textures/entity/scp548/scp548_ice_spider.png");
    public static final ResourceLocation AnimationPath = Scp548Registry.location("animations/entity/scp548/scp548_ice_spider.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp548Spider Spider) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp548Spider Spider) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp548Spider Spider) {
        return AnimationPath;
    }
}
