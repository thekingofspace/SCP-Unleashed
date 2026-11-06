package net.rai.scp_unleashed.scp1762.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp1762.Scp1762BoxBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class Scp1762BoxRenderer extends GeoBlockRenderer<Scp1762BoxBlockEntity> {
    public static final float Scale = 1.0f / 3.0f;
    public static final int ViewDistance = 96;

    public Scp1762BoxRenderer(BlockEntityRendererProvider.Context Context) {
        super(new Scp1762BoxGeoModel());
        withScale(Scale);
    }

    @Override
    public int getViewDistance() {
        return ViewDistance;
    }

    @Override
    public AABB getRenderBoundingBox(Scp1762BoxBlockEntity Box) {
        return new AABB(Box.getBlockPos()).inflate(0.6, 0.0, 0.6).expandTowards(0.0, 0.8, 0.0);
    }
}
