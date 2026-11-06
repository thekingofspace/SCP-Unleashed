package net.rai.scp_unleashed.storage.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class StorageModels {
    public static final ModelResourceLocation Door = standalone("container/door");
    public static final ModelResourceLocation GlazedDoor = standalone("container/door_glazed");
    public static final ModelResourceLocation Glass = standalone("container/door_glass");
    public static final ModelResourceLocation FrostedGlass = standalone("container/door_glass_frost");
    public static final ModelResourceLocation TallDoor = standalone("container_tall/door");
    public static final ModelResourceLocation TallGlazedDoor = standalone("container_tall/door_glazed");
    public static final ModelResourceLocation TallGlass = standalone("container_tall/door_glass");
    public static final ModelResourceLocation TallFrostedGlass = standalone("container_tall/door_glass_frost");
    public static final ModelResourceLocation Pedestal = standalone("pedestal");
    public static final ModelResourceLocation TallPedestal = standalone("pedestal_tall");

    private StorageModels() {
    }

    public static ModelResourceLocation standalone(String Path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/storage/" + Path));
    }

    public static ModelResourceLocation door(boolean Tall, boolean Glazed) {
        if (Tall) {
            return Glazed ? TallGlazedDoor : TallDoor;
        }
        return Glazed ? GlazedDoor : Door;
    }

    public static ModelResourceLocation glass(boolean Tall, boolean Frosted) {
        if (Tall) {
            return Frosted ? TallFrostedGlass : TallGlass;
        }
        return Frosted ? FrostedGlass : Glass;
    }

    public static List<ModelResourceLocation> all() {
        return List.of(Door, GlazedDoor, Glass, FrostedGlass, TallDoor, TallGlazedDoor, TallGlass, TallFrostedGlass, Pedestal, TallPedestal);
    }
}
