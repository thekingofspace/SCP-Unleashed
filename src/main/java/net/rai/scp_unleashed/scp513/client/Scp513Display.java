package net.rai.scp_unleashed.scp513.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp513.Scp513BellItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp513Display {
    public static final float SmallScale = 0.62f;
    public static final float TallScale = 0.8f;
    public static final float Yaw = 28.0f;
    public static final float Rise = 6.0f;
    public static final float SuspendedScale = 0.6f;
    public static final float SuspendedHeight = 9.0f;
    public static final float SuspendedYaw = 34.0f;
    public static final float SuspendedRoll = 9.0f;

    private Scp513Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp513BellItem::is, StorageLayout.sized(layout(false), layout(true)));
    }

    public static InteriorLayout layout(boolean Tall) {
        float Scale = Tall ? TallScale : SmallScale;
        Placement Rest = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Tall) + Rise * Scale, StorageGeometry.CentreZ)
                .withRotation(Yaw, 0.0f, 0.0f).withScale(Scale);
        return InteriorLayout.pedestal(Tall).withStance(ItemPose.resting(Rest).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }

    public static StorageLayout interior(ItemStack Held, float Sway) {
        return Scp513BellItem.is(Held) ? suspended(Sway) : StorageLayouts.forStack(Held, false);
    }

    public static InteriorLayout suspended(float Sway) {
        Placement Held = Placement.at(StorageGeometry.CentreX, SuspendedHeight, StorageGeometry.CentreZ)
                .withRotation(SuspendedYaw + Sway * 4.0f, 0.0f, SuspendedRoll + Sway * 2.0f).withScale(SuspendedScale);
        return InteriorLayout.floor(false).withoutPlatform().withStance(ItemPose.resting(Held).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }
}
