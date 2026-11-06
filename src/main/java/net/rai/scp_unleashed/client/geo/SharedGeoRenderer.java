package net.rai.scp_unleashed.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SharedGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
    public SharedGeoRenderer(EntityRendererProvider.Context Context, GeoModel<T> Model) {
        super(Context, Model);
    }

    @Override
    public void render(T Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        try {
            super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
        } finally {
            SharedBones.release(getGeoModel().getAnimationProcessor().getRegisteredBones());
        }
    }
}
