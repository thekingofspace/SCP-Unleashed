package net.rai.scp_unleashed.scp093.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp093.Scp093Registry;
import net.rai.scp_unleashed.scp093.Scp093Torso;
import software.bernie.geckolib.model.GeoModel;

public class Scp093TorsoGeoModel extends GeoModel<Scp093Torso> {
    public static final String Name = "scp093_torso";
    private static final ResourceLocation ModelPath = Scp093Registry.location("geo/entity/scp093/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = Scp093Registry.location("textures/entity/scp093/" + Name + ".png");
    private static final ResourceLocation AnimationPath = Scp093Registry.location("animations/entity/scp093/" + Name + ".animation.json");

    @Override
    public ResourceLocation getModelResource(Scp093Torso Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp093Torso Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp093Torso Creature) {
        return AnimationPath;
    }
}
