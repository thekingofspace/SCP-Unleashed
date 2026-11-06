package net.rai.scp_unleashed.scp105.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp105.Scp105Iris;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp105Renderer extends GeoEntityRenderer<Scp105Iris> {
    public static final float Shadow = 0.45f;

    public Scp105Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp105GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp105Iris Owner, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Matrices, Buffers, Light);
    }
}
