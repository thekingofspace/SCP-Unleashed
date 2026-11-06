package net.rai.scp_unleashed.scp148.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp148.Scp148Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp148Display {
    public static final float SmallScale = 0.6f;
    public static final float TallScale = 0.8f;
    public static final float Yaw = -12.0f;
    public static final float Pitch = 0.0f;
    public static final float Rise = 4.0f;
    public static final float ShiftX = 0.0f;
    public static final float ShiftZ = 0.0f;
    public static final float BarSmallScale = 0.42f;
    public static final float BarTallScale = 0.56f;
    public static final float BarYaw = -10.0f;
    public static final float BarRise = 8.0f;
    public static final float BarHeight = 4.0f;
    public static final float BarSpacing = 3.5f;
    public static final int PairCount = 2;
    public static final int PyramidCount = 3;

    private Scp148Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp148Registry.Helmet.get(), StorageLayout.sized(layout(false), layout(true)));
        StorageLayouts.register(Scp148Registry.Ingot.get(), Scp148Display::bars);
    }

    public static InteriorLayout layout(boolean Tall) {
        float Scale = Tall ? TallScale : SmallScale;
        Placement Rest = Placement.at(StorageGeometry.CentreX + ShiftX * Scale, InteriorLayout.platformTop(Tall) + Rise * Scale, StorageGeometry.CentreZ + ShiftZ * Scale)
                .withRotation(Yaw, Pitch, 0.0f).withScale(Scale);
        return InteriorLayout.pedestal(Tall).withStance(ItemPose.resting(Rest).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }

    public static void bars(StorageLayoutContext Context) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        ItemStack Shown = Context.Stack();
        int Count = Shown.getCount();
        float Scale = Context.Tall() ? BarTallScale : BarSmallScale;
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Context.Tall()), StorageGeometry.CentreZ).withYaw(BarYaw).withScale(Scale).apply(Pose);
        if (Count < PairCount) {
            bar(Context, Shown, 0.0f, 0.0f);
        } else {
            bar(Context, Shown, 0.0f, -BarSpacing);
            bar(Context, Shown, 0.0f, BarSpacing);
            if (Count >= PyramidCount) {
                bar(Context, Shown, BarHeight, 0.0f);
            }
        }
        Pose.popPose();
    }

    private static void bar(StorageLayoutContext Context, ItemStack Shown, float Base, float Z) {
        Context.item(Shown, Placement.at(0.0f, Base + BarRise, Z), ItemDisplayContext.NONE, ItemAnchor.Centre);
    }
}
