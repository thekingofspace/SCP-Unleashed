package net.rai.scp_unleashed.raid.hostile.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;

public class HostileRaiderRenderer extends DClassRenderer<HostileRaider> {
    public HostileRaiderRenderer(EntityRendererProvider.Context Context) {
        super(Context, new HostileRaiderGeoModel());
    }
}
