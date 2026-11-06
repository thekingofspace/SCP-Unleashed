package net.rai.scp_unleashed.scp035.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp035.Scp035MaskItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp035Display {
    public static final ModelResourceLocation Stand = standalone("stand");
    public static final ModelResourceLocation TallStand = standalone("stand_tall");
    public static final float StandZ = 9.0f;
    public static final float SmallScale = 0.5f;
    public static final float SmallCentreY = 9.0f;
    public static final float SmallCentreZ = 8.95f;
    public static final float TallScale = 0.85f;
    public static final float TallCentreY = 16.6f;
    public static final float TallCentreZ = 8.6f;

    private Scp035Display() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp035/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Stand, TallStand);
    }

    public static void register() {
        StorageLayouts.register(Scp035MaskItem::is, StorageLayout.sized(
                Context -> onStand(Context, Stand, SmallScale, SmallCentreY, SmallCentreZ),
                Context -> onStand(Context, TallStand, TallScale, TallCentreY, TallCentreZ)));
    }

    private static void onStand(StorageLayoutContext Context, ModelResourceLocation Model, float Scale, float CentreY, float CentreZ) {
        Context.model(Model, Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, StandZ));
        if (!Context.isEmpty()) {
            Context.item(Context.Stack(), Placement.at(StorageGeometry.CentreX, CentreY, CentreZ).withScale(Scale), ItemDisplayContext.FIXED, ItemAnchor.Centre);
        }
    }
}
