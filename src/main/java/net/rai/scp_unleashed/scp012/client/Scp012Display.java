package net.rai.scp_unleashed.scp012.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp012.Scp012CompositionItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp012Display {
    public static final ModelResourceLocation Stand = standalone("stand");
    public static final ModelResourceLocation TallStand = standalone("stand_tall");
    public static final float StandZ = 9.0f;
    public static final float Tilt = 22.5f;
    public static final float SmallScale = 0.48f;
    public static final float SmallRestY = 5.572f;
    public static final float SmallRestZ = 8.863f;
    public static final float TallScale = 0.8f;
    public static final float TallRestY = 11.664f;
    public static final float TallRestZ = 8.641f;

    private Scp012Display() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp012/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Stand, TallStand);
    }

    public static void register() {
        StorageLayouts.register(Scp012CompositionItem::is, StorageLayout.sized(
                Context -> onStand(Context, Stand, SmallScale, SmallRestY, SmallRestZ),
                Context -> onStand(Context, TallStand, TallScale, TallRestY, TallRestZ)));
    }

    private static void onStand(StorageLayoutContext Context, ModelResourceLocation Model, float Scale, float RestY, float RestZ) {
        Context.model(Model, Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, StandZ));
        if (!Context.isEmpty()) {
            Placement Rest = Placement.at(StorageGeometry.CentreX, RestY, RestZ).withRotation(0.0f, Tilt, 0.0f).withScale(Scale);
            Context.item(Context.Stack(), Rest, ItemDisplayContext.FIXED, ItemAnchor.Bottom);
        }
    }
}
