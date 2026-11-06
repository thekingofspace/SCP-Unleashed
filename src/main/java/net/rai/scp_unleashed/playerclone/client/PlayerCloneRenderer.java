package net.rai.scp_unleashed.playerclone.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.playerclone.PlayerClone;

public class PlayerCloneRenderer extends DClassRenderer<PlayerClone> {
    public PlayerCloneRenderer(EntityRendererProvider.Context Context) {
        super(Context, new PlayerCloneGeoModel());
    }
}
