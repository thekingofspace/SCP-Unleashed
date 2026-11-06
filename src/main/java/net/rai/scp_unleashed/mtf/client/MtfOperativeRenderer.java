package net.rai.scp_unleashed.mtf.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.mtf.MtfOperative;

public class MtfOperativeRenderer extends DClassRenderer<MtfOperative> {
    public MtfOperativeRenderer(EntityRendererProvider.Context Context) {
        super(Context, new MtfOperativeGeoModel());
    }
}
