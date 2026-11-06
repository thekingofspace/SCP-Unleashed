package net.rai.scp_unleashed.scp513.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp513.Scp513Registry;
import net.rai.scp_unleashed.scp513.Scp513Stalker;
import software.bernie.geckolib.model.GeoModel;

public class Scp513StalkerModel extends GeoModel<Scp513Stalker> {
    public static final ResourceLocation Model = Scp513Registry.location("geo/entity/scp513/scp513_stalker.geo.json");
    public static final ResourceLocation Texture = Scp513Registry.location("textures/entity/scp513/scp513_stalker.png");
    public static final ResourceLocation Animation = Scp513Registry.location("animations/entity/scp513/scp513_stalker.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp513Stalker Stalker) {
        return Model;
    }

    @Override
    public ResourceLocation getTextureResource(Scp513Stalker Stalker) {
        return Texture;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp513Stalker Stalker) {
        return Animation;
    }
}
