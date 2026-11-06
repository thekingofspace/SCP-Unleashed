package net.rai.scp_unleashed.scp428.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp428.Scp428Core;
import net.rai.scp_unleashed.scp428.Scp428Registry;
import software.bernie.geckolib.model.GeoModel;

public class Scp428CoreGeoModel extends GeoModel<Scp428Core> {
    public static final ResourceLocation ModelPath = Scp428Registry.location("geo/entity/scp428/scp428.geo.json");
    public static final ResourceLocation TexturePath = Scp428Registry.location("textures/entity/scp428/scp428.png");
    public static final ResourceLocation AnimationPath = Scp428Registry.location("animations/entity/scp428/scp428.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp428Core Core) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp428Core Core) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp428Core Core) {
        return AnimationPath;
    }
}
