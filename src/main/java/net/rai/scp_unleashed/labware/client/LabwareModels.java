package net.rai.scp_unleashed.labware.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class LabwareModels {
    public static final ModelResourceLocation StorageLid = standalone("viral_storage_lid");
    public static final ModelResourceLocation StorageLidFrost = standalone("viral_storage_lid_frost");

    private LabwareModels() {
    }

    public static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/labware/" + Name));
    }

    public static ModelResourceLocation storageLid(boolean Frosted) {
        return Frosted ? StorageLidFrost : StorageLid;
    }

    public static List<ModelResourceLocation> all() {
        return List.of(StorageLid, StorageLidFrost);
    }
}
