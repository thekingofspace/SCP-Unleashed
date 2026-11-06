package net.rai.scp_unleashed.scp207.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp207.Scp207BottleItem;
import net.rai.scp_unleashed.scp207.Scp207Rack;
import net.rai.scp_unleashed.scp207.Scp207Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp207Display {
    public static final ModelResourceLocation Rack = ModelResourceLocation.standalone(Scp207Registry.location("block/scp207/storage_rack"));
    public static final float SmallScale = 0.46f;
    public static final float TallScale = 1.0f;
    public static final float SmallYaw = 20.0f;
    public static final float TallYaw = 20.0f;
    public static final float GroupYaw = 200.0f;
    public static final float SmallGroupScale = 0.5f;
    public static final float SmallPitch = 3.0f;
    public static final float TallGroupScale = 0.7f;
    public static final float TallPitch = 4.5f;
    public static final float LowerTier = 5.0f;
    public static final float UpperTier = 17.0f;
    public static final int TierSize = 4;
    public static final float[][][] Spots = {
            {{0.0f, 0.0f}},
            {{-0.5f, 0.0f}, {0.5f, 0.0f}},
            {{-0.5f, 0.5f}, {0.5f, 0.5f}, {0.0f, -0.5f}},
            {{-0.5f, 0.5f}, {0.5f, 0.5f}, {-0.5f, -0.5f}, {0.5f, -0.5f}}
    };
    private static final Placement PieceOrigin = Placement.at(8.0f, 0.0f, 8.0f);
    private static ItemStack Plain = ItemStack.EMPTY;
    private static ItemStack Blue = ItemStack.EMPTY;

    private Scp207Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp207BottleItem::is, StorageLayout.sized(Scp207Display::small, Scp207Display::tall));
    }

    private static void small(StorageLayoutContext Context) {
        Scp207Rack Held = Scp207Rack.of(Context.Stack());
        if (Held.size() <= 1) {
            onPedestal(Context, SmallScale, SmallYaw);
            return;
        }
        Context.platform();
        tier(Context, Held, 0, Held.size(), InteriorLayout.platformTop(false), SmallPitch, SmallGroupScale);
    }

    private static void tall(StorageLayoutContext Context) {
        Scp207Rack Held = Scp207Rack.of(Context.Stack());
        if (Held.size() <= 1) {
            onPedestal(Context, TallScale, TallYaw);
            return;
        }
        Context.model(Rack, PieceOrigin);
        int Lower = Math.min(TierSize, Held.size());
        tier(Context, Held, 0, Lower, LowerTier, TallPitch, TallGroupScale);
        if (Held.size() > TierSize) {
            tier(Context, Held, TierSize, Held.size() - TierSize, UpperTier, TallPitch, TallGroupScale);
        }
    }

    private static void tier(StorageLayoutContext Context, Scp207Rack Held, int First, int Count, float Floor, float Pitch, float Scale) {
        float[][] Layout = Spots[Math.min(TierSize, Count) - 1];
        for (int Index = 0; Index < Layout.length; Index++) {
            float X = StorageGeometry.CentreX + Layout[Index][0] * Pitch;
            float Z = StorageGeometry.CentreZ + Layout[Index][1] * Pitch;
            Placement Standing = Placement.at(X, Floor, Z).withYaw(GroupYaw).withScale(Scale);
            Context.item(bottle(Held.isBlue(First + Index)), Standing, ItemDisplayContext.NONE, ItemAnchor.Bottom);
        }
    }

    private static ItemStack bottle(boolean IsBlue) {
        if (Plain.isEmpty()) {
            Plain = Scp207BottleItem.stack(false);
            Blue = Scp207BottleItem.stack(true);
        }
        return IsBlue ? Blue : Plain;
    }

    private static void onPedestal(StorageLayoutContext Context, float Scale, float Yaw) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        Placement Standing = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Context.Tall()), StorageGeometry.CentreZ).withYaw(Yaw).withScale(Scale);
        Context.item(Context.Stack(), Standing, ItemDisplayContext.NONE, ItemAnchor.Bottom);
    }
}
