package net.rai.scp_unleashed.scp5001.xk.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;
import net.rai.scp_unleashed.scp5001.xk.Yaldabaoth;

public class YaldabaothEntityRenderer extends EntityRenderer<Yaldabaoth> {
    private static final ResourceLocation Texture = Scp5001XkRegistry.location("textures/entity/yaldabaoth/yaldabaoth_head.png");

    public YaldabaothEntityRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public boolean shouldRender(Yaldabaoth Owner, Frustum Cull, double X, double Y, double Z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(Yaldabaoth Owner) {
        return Texture;
    }
}
