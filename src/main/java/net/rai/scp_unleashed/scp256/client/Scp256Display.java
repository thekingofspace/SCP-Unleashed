package net.rai.scp_unleashed.scp256.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp256.Scp256Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp256Display {
    public static final float SmallScale = 0.44f;
    public static final float TallScale = 0.66f;
    public static final float SmallYaw = 14.0f;
    public static final float TallYaw = 8.0f;

    private Scp256Display() {
    }

    public static boolean is(ItemStack Stack) {
        return Stack.is(Scp256Registry.TypewriterItem.get());
    }

    public static void register() {
        StorageLayouts.register(Scp256Display::is, StorageLayout.sized(
                Context -> onPlatform(Context, SmallScale, SmallYaw),
                Context -> onPlatform(Context, TallScale, TallYaw)));
    }

    private static void onPlatform(StorageLayoutContext Context, float Scale, float Yaw) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        Placement Resting = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Context.Tall()), StorageGeometry.CentreZ).withYaw(Yaw).withScale(Scale);
        Context.item(Context.Stack(), Resting, ItemDisplayContext.NONE, ItemAnchor.Bottom);
    }
}
