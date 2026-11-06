package net.rai.scp_unleashed.scp111.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp111.Scp111BoxItem;
import net.rai.scp_unleashed.scp111.Scp111Clutch;
import net.rai.scp_unleashed.scp111.Scp111EggBlock;
import net.rai.scp_unleashed.scp111.Scp111EggItem;
import net.rai.scp_unleashed.scp111.Scp111Shelf;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp111Display {
    public static final ModelResourceLocation Tray = standalone("egg_tray");
    public static final ModelResourceLocation Shelf = standalone("egg_shelf");
    public static final int Columns = 4;
    public static final int Rows = 4;
    public static final float FirstX = 4.25f;
    public static final float StepX = 2.5f;
    public static final float FirstZ = 5.75f;
    public static final float StepZ = 2.5f;
    public static final float TrayHeight = 1.0f;
    public static final float FloorY = StorageGeometry.CavityFloor;
    public static final float ShelfY = 16.0f;
    public static final float ShelfThickness = 1.0f;
    public static final float EggScale = 1.4f;
    public static final float BoxSmallScale = 0.62f;
    public static final float BoxTallScale = 0.9f;
    public static final float BoxYaw = 162.0f;
    public static final float BoxZ = 9.5f;

    private Scp111Display() {
    }

    public static ModelResourceLocation standalone(String Path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp111/" + Path));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Tray, Shelf);
    }

    public static void register() {
        StorageLayouts.register(Scp111EggItem::is, StorageLayout.sized(Context -> eggs(Context, false), Context -> eggs(Context, true)));
        StorageLayouts.register(Scp111BoxItem::is, StorageLayout.sized(Context -> box(Context, BoxSmallScale), Context -> box(Context, BoxTallScale)));
    }

    private static void eggs(StorageLayoutContext Context, boolean Tall) {
        Context.model(Tray, Placement.at(StorageGeometry.CentreX, FloorY, StorageGeometry.CentreZ));
        if (Tall) {
            Context.model(Shelf, Placement.at(StorageGeometry.CentreX, ShelfY, StorageGeometry.CentreZ));
            Context.model(Tray, Placement.at(StorageGeometry.CentreX, ShelfY + ShelfThickness, StorageGeometry.CentreZ));
        }
        if (Context.isEmpty()) {
            return;
        }
        List<Scp111Clutch.Entry> Eggs = Scp111Clutch.of(Context.Stack()).Eggs();
        int Shown = Math.min(Eggs.size(), Tall ? Scp111Shelf.TallCapacity : Scp111Shelf.SmallCapacity);
        long Seed = Context.seed();
        for (int Index = 0; Index < Shown; Index++) {
            Scp111Clutch.Entry Egg = Eggs.get(Index);
            int Slot = Index % (Columns * Rows);
            boolean Upper = Index >= Columns * Rows;
            float Base = (Upper ? ShelfY + ShelfThickness : FloorY) + TrayHeight;
            float X = FirstX + Slot % Columns * StepX;
            float Z = FirstZ + Slot / Columns * StepZ;
            float Yaw = Math.floorMod(Seed * 31L + Index * 7919L, 360L);
            Context.block(Scp111EggBlock.clutch(Egg.Variant(), 1, Egg.egg().Stage()), Placement.at(X, Base, Z).withYaw(Yaw).withScale(EggScale));
        }
    }

    private static void box(StorageLayoutContext Context, float Scale) {
        if (Context.isEmpty()) {
            return;
        }
        ItemStack Shown = Context.Stack();
        Placement Spot = Placement.at(StorageGeometry.CentreX, FloorY, BoxZ).withYaw(BoxYaw).withScale(Scale);
        Context.item(Shown, Spot, ItemDisplayContext.FIXED, ItemAnchor.Bottom);
    }
}
