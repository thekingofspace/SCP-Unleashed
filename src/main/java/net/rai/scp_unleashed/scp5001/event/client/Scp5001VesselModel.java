package net.rai.scp_unleashed.scp5001.event.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;

public class Scp5001VesselModel extends SharedGeoModel<Scp5001Vessel> {
    public static final ResourceLocation ModelPath = Scp5001EventRegistry.location("geo/entity/scp5001_event/scp5001_vessel.geo.json");
    public static final ResourceLocation TexturePath = Scp5001EventRegistry.location("textures/entity/scp5001_event/scp5001_vessel.png");
    public static final ResourceLocation GlowPath = Scp5001EventRegistry.location("textures/entity/scp5001_event/scp5001_vessel_glowmask.png");
    public static final ResourceLocation AnimationPath = Scp5001EventRegistry.location("animations/entity/scp5001_event/scp5001_vessel.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp5001Vessel Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp5001Vessel Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp5001Vessel Owner) {
        return AnimationPath;
    }
}
