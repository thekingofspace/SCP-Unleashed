package net.rai.scp_unleashed.scp500.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp500.Scp500Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp500Display {
    public static final float SmallScale = 0.55f;
    public static final float TallScale = 0.75f;
    public static final float Yaw = 25.0f;
    public static final float Rise = 2.0f;

    private Scp500Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp500Registry.Pill.get(), StorageLayout.sized(layout(false), layout(true)));
    }

    public static InteriorLayout layout(boolean Tall) {
        float Scale = Tall ? TallScale : SmallScale;
        Placement Rest = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Tall) + Rise * Scale, StorageGeometry.CentreZ)
                .withRotation(Yaw, 0.0f, 0.0f).withScale(Scale);
        return InteriorLayout.pedestal(Tall).withStance(ItemPose.resting(Rest).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }
}
