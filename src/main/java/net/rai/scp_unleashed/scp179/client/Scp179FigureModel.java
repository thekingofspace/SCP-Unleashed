package net.rai.scp_unleashed.scp179.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import software.bernie.geckolib.model.GeoModel;

public class Scp179FigureModel extends GeoModel<Scp179Figure> {
    public static final String Name = "scp179";
    public static final ResourceLocation ModelPath = location("geo/entity/scp179/" + Name + ".geo.json");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp179/" + Name + ".animation.json");
    public static final ResourceLocation TexturePath = location("textures/entity/scp179/" + Name + ".png");
    public static final ResourceLocation GlowPath = location("textures/entity/scp179/" + Name + "_glowmask.png");

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp179Figure Figure) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp179Figure Figure) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp179Figure Figure) {
        return AnimationPath;
    }
}
