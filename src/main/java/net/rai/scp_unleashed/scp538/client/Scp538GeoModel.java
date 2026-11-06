package net.rai.scp_unleashed.scp538.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp538.Scp538Registry;
import net.rai.scp_unleashed.scp538.Scp538Spider;

public class Scp538GeoModel extends SharedGeoModel<Scp538Spider> {
    public static final ResourceLocation ModelPath = Scp538Registry.location("geo/entity/scp538/scp538_shadow_spider.geo.json");
    public static final ResourceLocation TexturePath = Scp538Registry.location("textures/entity/scp538/scp538_shadow_spider.png");
    public static final ResourceLocation AnimationPath = Scp538Registry.location("animations/entity/scp538/scp538_shadow_spider.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp538Spider Spider) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp538Spider Spider) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp538Spider Spider) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp538Spider Spider, ResourceLocation Texture) {
        return RenderType.entityTranslucent(Texture);
    }
}
