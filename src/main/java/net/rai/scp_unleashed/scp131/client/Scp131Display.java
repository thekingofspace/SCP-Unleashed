package net.rai.scp_unleashed.scp131.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp131.Scp131PodItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp131Display {
    public static final float SmallScale = 0.8f;
    public static final float TallScale = 1.15f;
    public static final float PodZ = 8.5f;
    public static final float SmallYaw = 18.0f;
    public static final float TallYaw = 10.0f;

    private Scp131Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp131PodItem::is, StorageLayout.sized(Context -> pod(Context, SmallScale, SmallYaw), Context -> pod(Context, TallScale, TallYaw)));
    }

    private static void pod(StorageLayoutContext Context, float Scale, float Yaw) {
        if (Context.isEmpty()) {
            return;
        }
        Placement Spot = Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, PodZ).withYaw(Yaw).withScale(Scale);
        Context.item(Context.Stack(), Spot, ItemDisplayContext.NONE, ItemAnchor.Centre);
    }
}
