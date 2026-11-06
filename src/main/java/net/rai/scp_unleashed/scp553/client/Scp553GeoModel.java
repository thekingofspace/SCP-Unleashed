package net.rai.scp_unleashed.scp553.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp553.Scp553Butterfly;
import net.rai.scp_unleashed.scp553.Scp553Registry;

public class Scp553GeoModel extends SharedGeoModel<Scp553Butterfly> {
    public static final ResourceLocation ModelPath = Scp553Registry.location("geo/entity/scp553/scp553_butterfly.geo.json");
    public static final ResourceLocation TexturePath = Scp553Registry.location("textures/entity/scp553/scp553_butterfly.png");
    public static final ResourceLocation AnimationPath = Scp553Registry.location("animations/entity/scp553/scp553_butterfly.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp553Butterfly Butterfly) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp553Butterfly Butterfly) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp553Butterfly Butterfly) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp553Butterfly Butterfly, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }
}
