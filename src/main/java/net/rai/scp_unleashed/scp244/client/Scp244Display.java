package net.rai.scp_unleashed.scp244.client;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp244.Scp244Style;
import net.rai.scp_unleashed.scp244.Scp244VaseBlock;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp244Display {
    public static final float[] SmallScale = {0.92f, 0.98f, 0.98f};
    public static final float[] TallScale = {2.0f, 1.34f, 1.78f};
    public static final float[] Yaw = {22.0f, 0.0f, 18.0f};

    private Scp244Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp244Display::is, StorageLayout.sized(Context -> onPedestal(Context, false), Context -> onPedestal(Context, true)));
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof BlockItem Held && Held.getBlock() instanceof Scp244VaseBlock;
    }

    private static void onPedestal(StorageLayoutContext Context, boolean Tall) {
        Context.platform();
        if (Context.isEmpty() || !(Context.Stack().getItem() instanceof BlockItem Held) || !(Held.getBlock() instanceof Scp244VaseBlock Vessel)) {
            return;
        }
        Scp244Style Style = Vessel.style();
        int Slot = Style.ordinal();
        float Scale = Tall ? TallScale[Slot] : SmallScale[Slot];
        Placement Standing = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Tall), StorageGeometry.CentreZ).withYaw(Yaw[Slot]).withScale(Scale);
        Context.item(Context.Stack(), Standing, ItemDisplayContext.FIXED, ItemAnchor.Bottom);
    }
}
