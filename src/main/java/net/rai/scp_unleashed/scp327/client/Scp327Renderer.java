package net.rai.scp_unleashed.scp327.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp327.Scp327Siren;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp327Renderer extends GeoEntityRenderer<Scp327Siren> {
    public static final float ShadowSize = 0.6f;

    public Scp327Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp327GeoModel());
        shadowRadius = ShadowSize;
    }

    @Override
    public RenderType getRenderType(Scp327Siren Creature, ResourceLocation Texture, MultiBufferSource Buffers, float PartialTick) {
        return RenderType.entityTranslucent(Texture);
    }

    @Override
    protected float getShadowRadius(Scp327Siren Creature) {
        return shadowRadius * Creature.getScale();
    }

    @Override
    protected float getDeathMaxRotation(Scp327Siren Creature) {
        return 0.0f;
    }
}
