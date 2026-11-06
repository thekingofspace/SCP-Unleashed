package net.rai.scp_unleashed.scp5001.engine.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp5001.engine.Scp5001EngineBlockEntity;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp5001EngineRenderer extends GeoBlockRenderer<Scp5001EngineBlockEntity> {
    public static final float Scale = 1.5f;
    public static final double Reach = 5.0;
    public static final double Tall = 11.0;

    public Scp5001EngineRenderer(BlockEntityRendererProvider.Context Context) {
        super(new Model());
        withScale(Scale);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(Scp5001EngineBlockEntity Owner) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(Scp5001EngineBlockEntity Owner) {
        return new AABB(Owner.getBlockPos()).inflate(Reach, 0.5, Reach).expandTowards(0.0, Tall, 0.0);
    }

    public static class Model extends GeoModel<Scp5001EngineBlockEntity> {
        private static final ResourceLocation ModelPath = Scp5001WorldRegistry.location("geo/block/scp5001_engine.geo.json");
        private static final ResourceLocation TexturePath = Scp5001WorldRegistry.location("textures/block/scp5001_world/engine.png");
        private static final ResourceLocation AnimationPath = Scp5001WorldRegistry.location("animations/block/scp5001_engine.animation.json");

        @Override
        public ResourceLocation getModelResource(Scp5001EngineBlockEntity Owner) {
            return ModelPath;
        }

        @Override
        public ResourceLocation getTextureResource(Scp5001EngineBlockEntity Owner) {
            return TexturePath;
        }

        @Override
        public ResourceLocation getAnimationResource(Scp5001EngineBlockEntity Owner) {
            return AnimationPath;
        }
    }
}
