package net.rai.scp_unleashed.playerclone.client;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.playerclone.PlayerClone;

public class PlayerCloneGeoModel extends DClassGeoModel<PlayerClone> {
    public static final ResourceLocation WidePath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/player_clone/player_clone.geo.json");
    public static final ResourceLocation SlimPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/player_clone/player_clone_slim.geo.json");

    @Override
    public ResourceLocation getModelResource(PlayerClone Owner) {
        return PlayerCloneSkins.skinOf(Owner).model() == PlayerSkin.Model.SLIM ? SlimPath : WidePath;
    }

    @Override
    public ResourceLocation getTextureResource(PlayerClone Owner) {
        return PlayerCloneSkins.skinOf(Owner).texture();
    }
}
