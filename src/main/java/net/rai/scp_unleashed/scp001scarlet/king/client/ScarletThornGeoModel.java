package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletThorn;

public class ScarletThornGeoModel extends SharedGeoModel<ScarletThorn> {
    public static final ResourceLocation ModelPath = ScarletKingRegistry.location("geo/entity/scarlet_king/scarlet_king_thorn.geo.json");
    public static final ResourceLocation TexturePath = ScarletKingRegistry.location("textures/entity/scarlet_king/scarlet_king_thorn.png");
    public static final ResourceLocation AnimationPath = ScarletKingRegistry.location("animations/entity/scarlet_king/scarlet_king_thorn.animation.json");

    @Override
    public ResourceLocation getModelResource(ScarletThorn Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(ScarletThorn Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(ScarletThorn Owner) {
        return AnimationPath;
    }
}
