package net.rai.scp_unleashed.scp323.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp323.Scp323Beast;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp323BeastRenderer extends GeoEntityRenderer<Scp323Beast> {
    public static final float ShadowSize = 0.7f;

    public Scp323BeastRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp323BeastModel());
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp323Beast Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp323Beast Creature) {
        return 0.0f;
    }
}
