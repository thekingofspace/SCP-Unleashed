package net.rai.scp_unleashed.scp372.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp372.Scp372Glimpse;
import software.bernie.geckolib.model.GeoModel;

public class Scp372GlimpseModel extends GeoModel<Scp372Glimpse> {
    @Override
    public ResourceLocation getModelResource(Scp372Glimpse Glimpse) {
        return Scp372GeoModel.ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp372Glimpse Glimpse) {
        return Scp372GeoModel.TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp372Glimpse Glimpse) {
        return Scp372GeoModel.AnimationPath;
    }
}
