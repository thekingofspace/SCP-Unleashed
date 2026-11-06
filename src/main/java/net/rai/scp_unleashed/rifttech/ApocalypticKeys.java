package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.Scp_unleashed;

public final class ApocalypticKeys {
    public static final TagKey<Item> Fragments = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "apocalyptic_fragments"));

    private ApocalypticKeys() {
    }

    public static boolean isKey(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(Fragments);
    }

    public static int count(Container Storage) {
        int Total = 0;
        for (int Slot = 0; Slot < Storage.getContainerSize(); Slot++) {
            ItemStack Stack = Storage.getItem(Slot);
            if (isKey(Stack)) {
                Total += Stack.getCount();
            }
        }
        return Total;
    }

    public static int count(IItemHandler Storage) {
        int Total = 0;
        for (int Slot = 0; Slot < Storage.getSlots(); Slot++) {
            ItemStack Stack = Storage.getStackInSlot(Slot);
            if (isKey(Stack)) {
                Total += Stack.getCount();
            }
        }
        return Total;
    }

    public static int count(Player User) {
        return count(User.getInventory());
    }

    public static boolean holds(Player User) {
        return count(User) > 0;
    }

    public static ItemStack take(Container Storage) {
        for (int Slot = 0; Slot < Storage.getContainerSize(); Slot++) {
            if (isKey(Storage.getItem(Slot))) {
                ItemStack Taken = Storage.removeItem(Slot, 1);
                Storage.setChanged();
                return Taken;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack take(IItemHandler Storage) {
        for (int Slot = 0; Slot < Storage.getSlots(); Slot++) {
            if (isKey(Storage.getStackInSlot(Slot))) {
                ItemStack Taken = Storage.extractItem(Slot, 1, false);
                if (!Taken.isEmpty()) {
                    return Taken;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean consume(Player User) {
        Inventory Pockets = User.getInventory();
        if (User.getAbilities().instabuild) {
            return holds(User);
        }
        return !take(Pockets).isEmpty();
    }
}
