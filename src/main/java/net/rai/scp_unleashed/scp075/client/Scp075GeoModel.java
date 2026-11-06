package net.rai.scp_unleashed.scp075.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp075.Scp075Snail;
import software.bernie.geckolib.model.GeoModel;

public class Scp075GeoModel extends GeoModel<Scp075Snail> {
    public static final String Name = "scp075_snail";
    private static final ResourceLocation ModelPath = location("geo/entity/scp075/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = location("textures/entity/scp075/" + Name + ".png");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp075/" + Name + ".animation.json");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp075Snail Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp075Snail Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp075Snail Creature) {
        return AnimationPath;
    }
}
