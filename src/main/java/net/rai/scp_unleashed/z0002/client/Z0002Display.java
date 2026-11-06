package net.rai.scp_unleashed.z0002.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;
import net.rai.scp_unleashed.z0002.Z0002FingerItem;
import net.rai.scp_unleashed.z0002.Z0002Registry;

import java.util.List;

public final class Z0002Display {
    public static final ModelResourceLocation Cushion = ModelResourceLocation.standalone(Z0002Registry.location("block/z0002/display_cushion"));
    public static final float CushionTop = 7.75f;
    public static final float SmallScale = 0.5f;
    public static final float TallScale = 0.62f;
    public static final float RestYaw = 28.0f;
    public static final float FingerLow = 6.0f;
    public static final float Gap = 0.05f;
    private static final Placement Origin = Placement.at(8.0f, 0.0f, 8.0f);

    private Z0002Display() {
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Cushion);
    }

    public static void register() {
        StorageLayouts.register(Z0002FingerItem::is, StorageLayout.sized(Context -> onCushion(Context, false), Context -> onCushion(Context, true)));
    }

    public static float cushionLift(boolean Tall) {
        return InteriorLayout.platformTop(Tall) - InteriorLayout.platformTop(false);
    }

    private static void onCushion(StorageLayoutContext Context, boolean Tall) {
        Context.platform();
        float Raise = cushionLift(Tall);
        Context.model(Cushion, Origin.moved(0.0f, Raise, 0.0f));
        if (Context.isEmpty()) {
            return;
        }
        float Scale = Tall ? TallScale : SmallScale;
        Placement Resting = Placement.at(StorageGeometry.CentreX, CushionTop + Raise + (8.0f - FingerLow) * Scale + Gap, StorageGeometry.CentreZ)
                .withRotation(RestYaw, 0.0f, 0.0f).withScale(Scale);
        Context.item(Context.Stack(), Resting, ItemDisplayContext.NONE, ItemAnchor.Centre);
    }
}
