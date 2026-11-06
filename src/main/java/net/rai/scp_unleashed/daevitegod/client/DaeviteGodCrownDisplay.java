package net.rai.scp_unleashed.daevitegod.client;

import net.minecraft.world.level.block.Blocks;
import net.rai.scp_unleashed.daevitegod.DaeviteGodCrownItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.LayoutMotion;
import net.rai.scp_unleashed.storage.client.LayoutPiece;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class DaeviteGodCrownDisplay {
    public static final float CushionHeight = 2.0f;
    public static final float CushionWidth = 8.0f;
    public static final int WarmTint = 0xFFFFE6CC;

    private DaeviteGodCrownDisplay() {
    }

    public static void register() {
        StorageLayouts.register(DaeviteGodCrownItem::is, StorageLayout.sized(cushion(false, 0.42f, 0.62f), cushion(true, 0.6f, 0.95f)));
    }

    private static InteriorLayout cushion(boolean Tall, float Flat, float Solid) {
        float Top = InteriorLayout.platformTop(Tall);
        Placement Pad = Placement.at(StorageGeometry.CentreX, Top, StorageGeometry.CentreZ).withScale(CushionWidth / 16.0f, CushionHeight / 16.0f, CushionWidth / 16.0f);
        Placement Spot = Placement.at(StorageGeometry.CentreX, Top + CushionHeight, StorageGeometry.CentreZ);
        return InteriorLayout.pedestal(Tall).withPiece(LayoutPiece.block(Blocks.RED_WOOL.defaultBlockState(), Pad))
                .withStance(ItemPose.resting(Spot.withScale(Flat), Spot.withScale(Solid)))
                .withMotion(LayoutMotion.spin(0.6f)).withLight(InteriorLayout.FullBright).withTint(WarmTint);
    }
}
