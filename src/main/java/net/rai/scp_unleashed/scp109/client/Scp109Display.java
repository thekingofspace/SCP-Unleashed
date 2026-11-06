package net.rai.scp_unleashed.scp109.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp109.Scp109CanteenItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp109Display {
    public static final float SmallScale = 0.5f;
    public static final float TallScale = 0.95f;
    public static final float SmallYaw = 28.0f;
    public static final float TallYaw = 18.0f;

    private Scp109Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp109CanteenItem::is, StorageLayout.sized(
                Context -> onPedestal(Context, SmallScale, SmallYaw),
                Context -> onPedestal(Context, TallScale, TallYaw)));
    }

    private static void onPedestal(StorageLayoutContext Context, float Scale, float Yaw) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        Placement Standing = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Context.Tall()), StorageGeometry.CentreZ).withYaw(Yaw).withScale(Scale);
        Context.item(Context.Stack(), Standing, ItemDisplayContext.FIXED, ItemAnchor.Bottom);
    }
}
