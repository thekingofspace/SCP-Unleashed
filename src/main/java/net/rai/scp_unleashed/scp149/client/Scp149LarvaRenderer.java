package net.rai.scp_unleashed.scp149.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp149.Scp149Larva;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp149LarvaRenderer extends GeoEntityRenderer<Scp149Larva> {
    public static final float Shadow = 0.15f;

    public Scp149LarvaRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp149LarvaGeoModel());
        withScale(Scp149Larva.ModelScale);
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp149Larva Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
