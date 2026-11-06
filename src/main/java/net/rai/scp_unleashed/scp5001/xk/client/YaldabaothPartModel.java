package net.rai.scp_unleashed.scp5001.xk.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class YaldabaothPartModel extends GeoModel<YaldabaothPart> {
    @Override
    public ResourceLocation getModelResource(YaldabaothPart Part) {
        return Part.model();
    }

    @Override
    public ResourceLocation getTextureResource(YaldabaothPart Part) {
        return Part.texture();
    }

    @Override
    public ResourceLocation getAnimationResource(YaldabaothPart Part) {
        return Part.model();
    }
}
