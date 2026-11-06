package net.rai.scp_unleashed.scp215.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.rai.scp_unleashed.scp215.Scp215GlassesItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp215Display {
    public static final float SmallScale = 0.46f;
    public static final float TallScale = 0.62f;
    public static final float SmallYaw = -24.0f;
    public static final float TallYaw = -20.0f;
    public static final float Tilt = 11.1f;
    public static final float RestDrop = 0.66f;
    public static final float CushionWidth = 9.0f;
    public static final float CushionDepth = 8.0f;
    public static final float CushionHeight = 1.0f;
    public static final float Lift = 0.05f;

    private Scp215Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp215GlassesItem::is, (StorageLayout) Scp215Display::onCushion);
    }

    public static float cushionTop(boolean Tall) {
        return InteriorLayout.platformTop(Tall) + CushionHeight;
    }

    public static Placement rest(boolean Tall) {
        float Scale = Tall ? TallScale : SmallScale;
        float Yaw = Tall ? TallYaw : SmallYaw;
        float Centre = cushionTop(Tall) + RestDrop * Scale + Lift;
        return Placement.at(StorageGeometry.CentreX, Centre, StorageGeometry.CentreZ).withRotation(Yaw, Tilt, 0.0f).withScale(Scale);
    }

    private static void onCushion(StorageLayoutContext Context) {
        boolean Tall = Context.Tall();
        Context.platform();
        float Top = InteriorLayout.platformTop(Tall);
        Context.block(Blocks.GRAY_WOOL.defaultBlockState(), Placement.at(StorageGeometry.CentreX, Top, StorageGeometry.CentreZ)
                .withScale(CushionWidth / 16.0f, CushionHeight / 16.0f, CushionDepth / 16.0f));
        if (!Context.isEmpty()) {
            Context.item(Context.Stack(), rest(Tall), ItemDisplayContext.NONE, ItemAnchor.Centre);
        }
    }
}
