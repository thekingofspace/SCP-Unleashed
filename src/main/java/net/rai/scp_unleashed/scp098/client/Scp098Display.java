package net.rai.scp_unleashed.scp098.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp098.Scp098Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp098Display {
    public static final float SmallScale = 0.52f;
    public static final float TallScale = 0.8f;
    public static final float Yaw = 20.0f;
    public static final float Pitch = 0.0f;
    public static final float Rise = 7.0f;
    public static final float ShiftX = 0.0f;
    public static final float ShiftZ = 0.0f;

    private Scp098Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp098Registry.Vial.get(), StorageLayout.sized(layout(false), layout(true)));
    }

    public static InteriorLayout layout(boolean Tall) {
        float Scale = Tall ? TallScale : SmallScale;
        Placement Rest = Placement.at(StorageGeometry.CentreX + ShiftX * Scale, InteriorLayout.platformTop(Tall) + Rise * Scale, StorageGeometry.CentreZ + ShiftZ * Scale)
                .withRotation(Yaw, Pitch, 0.0f).withScale(Scale);
        return InteriorLayout.pedestal(Tall).withStance(ItemPose.resting(Rest).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }
}
