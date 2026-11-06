package net.rai.scp_unleashed.scp149.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp149.Scp149Larva;
import software.bernie.geckolib.model.GeoModel;

public class Scp149LarvaGeoModel extends GeoModel<Scp149Larva> {
    public static final String Name = "scp149_larva";
    private static final ResourceLocation ModelPath = Scp149FlyGeoModel.location("geo/entity/scp149/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = Scp149FlyGeoModel.location("textures/entity/scp149/" + Name + ".png");
    private static final ResourceLocation AnimationPath = Scp149FlyGeoModel.location("animations/entity/scp149/" + Name + ".animation.json");

    @Override
    public ResourceLocation getModelResource(Scp149Larva Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp149Larva Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp149Larva Creature) {
        return AnimationPath;
    }
}
