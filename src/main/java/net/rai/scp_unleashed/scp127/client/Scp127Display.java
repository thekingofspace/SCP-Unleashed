package net.rai.scp_unleashed.scp127.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp127.Scp127Item;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageFit;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp127Display {
    public static final ModelResourceLocation Rack = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp127/rack"));
    public static final float GunScale = 0.6f;
    public static final float GunX = 8.0f;
    public static final float GunY = 20.8f;
    public static final float GunZ = 9.0f;
    public static final float GunYaw = -90.0f;
    public static final float GunPitch = 90.0f;
    public static final float ShelfTop = 7.0f;
    private static final Placement PieceOrigin = Placement.at(8.0f, 0.0f, 8.0f);
    private static final Placement Standing = Placement.at(GunX, GunY, GunZ).withRotation(GunYaw, GunPitch, 0.0f).withScale(GunScale);

    private Scp127Display() {
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Rack);
    }

    public static void register() {
        StorageLayouts.register(Scp127Item::is, StorageLayout.only(StorageFit.TallOnly, Scp127Display::onRack));
    }

    public static void onRack(StorageLayoutContext Context) {
        Context.model(Rack, PieceOrigin);
        if (!Context.isEmpty()) {
            Context.item(Context.Stack(), Standing, ItemDisplayContext.NONE, ItemAnchor.Centre);
        }
    }
}
