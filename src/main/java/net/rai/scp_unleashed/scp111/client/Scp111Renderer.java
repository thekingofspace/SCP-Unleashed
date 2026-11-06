package net.rai.scp_unleashed.scp111.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp111.Scp111Snail;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp111Renderer extends GeoEntityRenderer<Scp111Snail> {
    public static final float ModelScale = 0.45f;
    public static final float Shadow = 0.13f;

    public Scp111Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp111GeoModel());
        addRenderLayer(new Scp111GlowLayer(this));
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(Scp111Snail Creature) {
        return Shadow * Creature.getAgeScale();
    }

    @Override
    public void scaleModelForRender(float WidthScale, float HeightScale, PoseStack Pose, Scp111Snail Creature, BakedGeoModel Model, boolean IsReRender, float PartialTick,
                                    int PackedLight, int PackedOverlay) {
        float Size = Creature.getAgeScale() * ModelScale;
        super.scaleModelForRender(WidthScale * Size, HeightScale * Size, Pose, Creature, Model, IsReRender, PartialTick, PackedLight, PackedOverlay);
    }

    @Override
    public void render(Scp111Snail Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
