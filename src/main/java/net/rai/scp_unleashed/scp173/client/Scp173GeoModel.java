package net.rai.scp_unleashed.scp173.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import software.bernie.geckolib.model.GeoModel;

public class Scp173GeoModel extends GeoModel<Scp173Statue> {
    public static final String Name = "scp173";
    private static final ResourceLocation ModelPath = location("geo/entity/scp173/" + Name + ".geo.json");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp173/" + Name + ".animation.json");
    private static final ResourceLocation OriginalTexture = location("textures/entity/scp173/" + Name + ".png");
    private static final ResourceLocation[] CloneTextures = {
            location("textures/entity/scp173/scp173_clone.png"),
            location("textures/entity/scp173/scp173_clone_cracked_1.png"),
            location("textures/entity/scp173/scp173_clone_cracked_2.png"),
            location("textures/entity/scp173/scp173_clone_cracked_3.png"),
    };

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp173Statue Statue) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp173Statue Statue) {
        if (!Statue.isClone()) {
            return OriginalTexture;
        }
        return CloneTextures[Math.max(0, Math.min(CloneTextures.length - 1, Statue.getCrackStage()))];
    }

    @Override
    public ResourceLocation getAnimationResource(Scp173Statue Statue) {
        return AnimationPath;
    }
}
