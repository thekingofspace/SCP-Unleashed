package net.rai.scp_unleashed.scp163.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp163.Scp163Castaway;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp163Renderer extends GeoEntityRenderer<Scp163Castaway> {
    public static final float Shadow = 0.75f;

    public Scp163Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp163GeoModel());
        shadowRadius = Shadow;
        addRenderLayer(new Scp163GlassLayer(this));
    }

    @Override
    public void render(Scp163Castaway Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
