package net.rai.scp_unleashed.gateway.client;

import net.rai.scp_unleashed.gateway.GatewayRegistry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class GatewayDisplay {
    public static final float FlatScale = 0.45f;
    public static final float ShardScale = 0.58f;
    public static final float Yaw = 30.0f;

    private GatewayDisplay() {
    }

    public static void register() {
        Placement Spot = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(false), StorageGeometry.CentreZ);
        InteriorLayout Small = InteriorLayout.pedestal(false).withStance(ItemPose.resting(Spot.withScale(FlatScale), Spot.withYaw(Yaw).withScale(ShardScale)));
        StorageLayouts.register(GatewayRegistry.Shard.get(), StorageLayout.sized(Small, InteriorLayout.pedestal(true)));
    }
}
