package net.rai.scp_unleashed.scp003.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp003.Scp003Creature;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp003CreatureRenderer<T extends Scp003Creature> extends GeoEntityRenderer<T> {
    public Scp003CreatureRenderer(EntityRendererProvider.Context Context, Scp003GeoModel<T> Geometry, float Shadow) {
        super(Context, Geometry);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = Shadow;
    }

    @Override
    public void render(T Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setGaitPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
