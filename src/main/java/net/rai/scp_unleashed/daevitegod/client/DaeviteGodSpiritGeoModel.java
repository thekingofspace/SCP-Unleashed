package net.rai.scp_unleashed.daevitegod.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.daevitegod.DaeviteGodSpirit;
import software.bernie.geckolib.model.GeoModel;

public class DaeviteGodSpiritGeoModel extends GeoModel<DaeviteGodSpirit> {
    public static final String Name = "daevite_god_spirit";
    public static final ResourceLocation ModelPath = DaeviteGodGeoModel.location("geo/entity/daevite_god/" + Name + ".geo.json");
    public static final ResourceLocation TexturePath = DaeviteGodGeoModel.location("textures/entity/daevite_god/" + Name + ".png");
    public static final ResourceLocation AnimationPath = DaeviteGodGeoModel.location("animations/entity/daevite_god/" + Name + ".animation.json");

    @Override
    public ResourceLocation getModelResource(DaeviteGodSpirit Spirit) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(DaeviteGodSpirit Spirit) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(DaeviteGodSpirit Spirit) {
        return AnimationPath;
    }
}
