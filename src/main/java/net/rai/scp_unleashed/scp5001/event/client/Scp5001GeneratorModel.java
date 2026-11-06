package net.rai.scp_unleashed.scp5001.event.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001GeneratorBlockEntity;

public class Scp5001GeneratorModel extends SharedGeoModel<Scp5001GeneratorBlockEntity> {
    public static final ResourceLocation ModelPath = Scp5001EventRegistry.location("geo/block/scp5001_event/scp5001_generator.geo.json");
    public static final ResourceLocation TexturePath = Scp5001EventRegistry.location("textures/block/scp5001_event/scp5001_generator.png");
    public static final ResourceLocation HotPath = Scp5001EventRegistry.location("textures/block/scp5001_event/scp5001_generator_overdrive.png");
    public static final ResourceLocation GlowPath = Scp5001EventRegistry.location("textures/block/scp5001_event/scp5001_generator_glowmask.png");
    public static final ResourceLocation HotGlowPath = Scp5001EventRegistry.location("textures/block/scp5001_event/scp5001_generator_overdrive_glowmask.png");
    public static final ResourceLocation AnimationPath = Scp5001EventRegistry.location("animations/block/scp5001_event/scp5001_generator.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp5001GeneratorBlockEntity Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp5001GeneratorBlockEntity Owner) {
        return Owner.overdriven() ? HotPath : TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp5001GeneratorBlockEntity Owner) {
        return AnimationPath;
    }
}
