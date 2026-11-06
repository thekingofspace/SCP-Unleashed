package net.rai.scp_unleashed.scp444.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp444.Scp444BookItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp444Display {
    public static final float FlatScale = 0.55f;
    public static final float FlatYaw = -14.0f;
    public static final float LeanScale = 0.8f;
    public static final float LeanPitch = -75.0f;
    public static final float LeanYaw = 6.0f;
    public static final float LeanRise = 6.29f;
    public static final float LeanZ = 6.9f;

    private Scp444Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp444BookItem::is, StorageLayout.sized(Scp444Display::flat, Scp444Display::lean));
    }

    public static void flat(StorageLayoutContext Context) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        Placement Rest = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(false), StorageGeometry.CentreZ).withYaw(FlatYaw).withScale(FlatScale);
        Context.item(Context.Stack(), Rest, ItemDisplayContext.NONE, ItemAnchor.Bottom);
    }

    public static void lean(StorageLayoutContext Context) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        Placement Rest = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(true) + LeanRise, LeanZ).withRotation(LeanYaw, LeanPitch, 0.0f).withScale(LeanScale);
        Context.item(Context.Stack(), Rest, ItemDisplayContext.NONE, ItemAnchor.Centre);
    }
}
