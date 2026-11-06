package net.rai.scp_unleashed.scp075.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp075.Scp075Snail;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp075Renderer extends GeoEntityRenderer<Scp075Snail> {
    public static final float Shadow = 0.5f;

    public Scp075Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp075GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(Scp075Snail Creature) {
        return Shadow * Creature.getAgeScale();
    }

    @Override
    public void scaleModelForRender(float WidthScale, float HeightScale, PoseStack Pose, Scp075Snail Creature, BakedGeoModel Model, boolean IsReRender, float PartialTick,
                                    int PackedLight, int PackedOverlay) {
        float Size = Creature.getAgeScale();
        super.scaleModelForRender(WidthScale * Size, HeightScale * Size, Pose, Creature, Model, IsReRender, PartialTick, PackedLight, PackedOverlay);
    }

    @Override
    public void render(Scp075Snail Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
