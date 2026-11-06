package net.rai.scp_unleashed.scp085.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp085.Scp085SheetItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp085Display {
    public static final float WallZ = 14.6f;
    public static final float SmallScale = 0.75f;
    public static final float SmallCentreY = 9.0f;
    public static final float TallScale = 1.0f;
    public static final float TallCentreY = 19.0f;

    private Scp085Display() {
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp085SheetItem;
    }

    public static void register() {
        StorageLayouts.register(Scp085Display::is, StorageLayout.sized(
                Context -> onWall(Context, SmallScale, SmallCentreY),
                Context -> onWall(Context, TallScale, TallCentreY)));
    }

    private static void onWall(StorageLayoutContext Context, float Scale, float CentreY) {
        if (Context.isEmpty()) {
            return;
        }
        Placement Hung = Placement.at(StorageGeometry.CentreX, CentreY, WallZ).withScale(Scale);
        Context.item(Context.Stack(), Hung, ItemDisplayContext.FIXED, ItemAnchor.Centre);
    }
}
