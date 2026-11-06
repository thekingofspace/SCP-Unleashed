package net.rai.scp_unleashed.scp294.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp294.Scp294CupItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp294Display {
    public static final float SmallScale = 0.62f;
    public static final float TallScale = 1.1f;
    public static final float Yaw = 25.0f;

    private Scp294Display() {
    }

    public static void register() {
        StorageLayouts.register(Stack -> Stack.getItem() instanceof Scp294CupItem, StorageLayout.sized(Context -> onPedestal(Context, SmallScale), Context -> onPedestal(Context, TallScale)));
    }

    private static void onPedestal(StorageLayoutContext Context, float Scale) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        Placement Standing = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Context.Tall()), StorageGeometry.CentreZ).withYaw(Yaw).withScale(Scale);
        Context.item(Context.Stack(), Standing, ItemDisplayContext.NONE, ItemAnchor.Bottom);
    }
}
