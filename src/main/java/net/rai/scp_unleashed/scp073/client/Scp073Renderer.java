package net.rai.scp_unleashed.scp073.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp073.Scp073Cain;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp073Renderer extends GeoEntityRenderer<Scp073Cain> {
    public static final float Shadow = 0.45f;

    public Scp073Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp073GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp073Cain Owner, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Matrices, Buffers, Light);
    }
}
