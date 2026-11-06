package net.rai.scp_unleashed.scp303.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp303.Scp303Doorman;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp303Renderer extends GeoEntityRenderer<Scp303Doorman> {
    public static final float Shadow = 0.6f;
    public static final float FoldedShadow = 0.75f;

    public Scp303Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp303GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(Scp303Doorman Doorman) {
        return Doorman.folded() ? FoldedShadow : Shadow;
    }

    @Override
    public void render(Scp303Doorman Doorman, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Doorman.setRenderPartial(PartialTick);
        super.render(Doorman, Doorman.locksMovement() ? Doorman.getYRot() : Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp303Doorman Doorman) {
        return 0.0f;
    }
}
