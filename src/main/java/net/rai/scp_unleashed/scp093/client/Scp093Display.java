package net.rai.scp_unleashed.scp093.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp093.Scp093DiscItem;
import net.rai.scp_unleashed.scp093.Scp093Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp093Display {
    public static final ModelResourceLocation Stand = ModelResourceLocation.standalone(Scp093Registry.location("block/scp093/display_stand"));
    public static final float StandZ = 9.0f;
    public static final float SmallScale = 0.55f;
    public static final float TallScale = 0.95f;
    public static final float TallStandScale = 1.6f;
    public static final float BaseTop = 1.0f;
    public static final float Lean = 12.0f;
    public static final float TearsSmallScale = 0.58f;
    public static final float TearsTallScale = 0.85f;
    public static final float TearsYaw = 20.0f;
    public static final float TearsRise = 7.0f;

    private Scp093Display() {
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Stand);
    }

    public static void register() {
        StorageLayouts.register(Scp093DiscItem::is, StorageLayout.sized(
                Context -> onStand(Context, 1.0f, SmallScale),
                Context -> onStand(Context, TallStandScale, TallScale)));
        StorageLayouts.register(Scp093Registry.Tears.get(), StorageLayout.sized(tears(false), tears(true)));
    }

    public static InteriorLayout tears(boolean Tall) {
        float Scale = Tall ? TearsTallScale : TearsSmallScale;
        Placement Rest = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Tall) + TearsRise * Scale, StorageGeometry.CentreZ)
                .withYaw(TearsYaw).withScale(Scale);
        return InteriorLayout.pedestal(Tall).withStance(ItemPose.resting(Rest).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }

    private static void onStand(StorageLayoutContext Context, float StandScale, float Scale) {
        Context.model(Stand, Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, StandZ).withScale(StandScale));
        if (!Context.isEmpty()) {
            Placement Rest = Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor + BaseTop * StandScale, StandZ - 0.5f * StandScale)
                    .withRotation(0.0f, Lean, 0.0f).withScale(Scale);
            Context.item(Context.Stack(), Rest, ItemDisplayContext.FIXED, ItemAnchor.Bottom);
        }
    }
}
