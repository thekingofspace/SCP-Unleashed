package net.rai.scp_unleashed.scp027.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp027.Scp027Entity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp027Renderer extends GeoEntityRenderer<Scp027Entity> {
    public static final float Shadow = 0.5f;

    public Scp027Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp027GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp027Entity Owner, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Matrices, Buffers, Light);
    }
}
