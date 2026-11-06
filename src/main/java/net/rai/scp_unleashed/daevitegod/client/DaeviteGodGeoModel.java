package net.rai.scp_unleashed.daevitegod.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevitegod.DaeviteGod;
import software.bernie.geckolib.model.GeoModel;

public class DaeviteGodGeoModel extends GeoModel<DaeviteGod> {
    public static final String GodName = "daevite_god";
    public static final String KingName = "daevite_god_king";
    public static final ResourceLocation GodModel = location("geo/entity/daevite_god/" + GodName + ".geo.json");
    public static final ResourceLocation GodTexture = location("textures/entity/daevite_god/" + GodName + ".png");
    public static final ResourceLocation GodAnimations = location("animations/entity/daevite_god/" + GodName + ".animation.json");
    public static final ResourceLocation KingModel = location("geo/entity/daevite_god/" + KingName + ".geo.json");
    public static final ResourceLocation KingTexture = location("textures/entity/daevite_god/" + KingName + ".png");
    public static final ResourceLocation KingAnimations = location("animations/entity/daevite_god/" + KingName + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(DaeviteGod God) {
        return God.king() ? KingModel : GodModel;
    }

    @Override
    public ResourceLocation getTextureResource(DaeviteGod God) {
        return God.king() ? KingTexture : GodTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(DaeviteGod God) {
        return God.king() ? KingAnimations : GodAnimations;
    }
}
