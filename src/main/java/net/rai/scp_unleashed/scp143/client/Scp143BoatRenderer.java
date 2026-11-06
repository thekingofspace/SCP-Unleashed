package net.rai.scp_unleashed.scp143.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import net.rai.scp_unleashed.Scp_unleashed;

public class Scp143BoatRenderer extends BoatRenderer {
    private final Pair<ResourceLocation, ListModel<Boat>> Look;

    public Scp143BoatRenderer(EntityRendererProvider.Context Context, boolean Chest) {
        super(Context, Chest);
        ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Chest ? "textures/entity/chest_boat/scp143_bladewood.png"
                : "textures/entity/boat/scp143_bladewood.png");
        ListModel<Boat> Hull = Chest ? new ChestBoatModel(Context.bakeLayer(ModelLayers.createChestBoatModelName(Boat.Type.OAK)))
                : new BoatModel(Context.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK)));
        this.Look = Pair.of(Texture, Hull);
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat Ride) {
        return Look;
    }
}
