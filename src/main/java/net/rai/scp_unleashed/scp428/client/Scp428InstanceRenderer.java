package net.rai.scp_unleashed.scp428.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.scp428.Scp428Instance;

public class Scp428InstanceRenderer extends DClassRenderer<Scp428Instance> {
    public Scp428InstanceRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp428InstanceGeoModel());
    }

    @Override
    public void render(Scp428Instance Body, float Yaw, float PartialTick, PoseStack Transforms, MultiBufferSource Buffers, int Light) {
        super.render(Body, Yaw, PartialTick, Transforms, Buffers, Light);
        Scp428Tendrils.tether(Body, getGeoModel().getAnimationProcessor().getBone(Scp428Tendrils.HeadBone), PartialTick, Transforms, Buffers, Light);
    }

    @Override
    public boolean shouldShowName(Scp428Instance Body) {
        return Body.shouldShowName();
    }
}
