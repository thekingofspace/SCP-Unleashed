package net.rai.scp_unleashed.scp173.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp173Renderer extends GeoEntityRenderer<Scp173Statue> {
    public static final float Shadow = 0.42f;

    public Scp173Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp173GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp173Statue Statue, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Statue.yBodyRotO = Statue.getYRot();
        Statue.yBodyRot = Statue.getYRot();
        super.render(Statue, Statue.getYRot(), PartialTick, Pose, Buffers, Light);
    }
}
