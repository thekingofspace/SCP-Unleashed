package net.rai.scp_unleashed.storage.gel.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class GelModels {
    public static final ModelResourceLocation Door = standalone("door");
    public static final ModelResourceLocation Glass = standalone("door_glass");
    public static final ModelResourceLocation Fill = standalone("gel_fill");

    private GelModels() {
    }

    public static ModelResourceLocation standalone(String Path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/storage/gel/" + Path));
    }

    public static List<ModelResourceLocation> all() {
        return List.of(Door, Glass, Fill);
    }
}
