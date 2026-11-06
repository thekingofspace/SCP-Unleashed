package net.rai.scp_unleashed.anomaly;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

public final class PackedAnomalies {
    public static final int MaxDepth = 4;
    public static final int PullTries = 64;

    private PackedAnomalies() {
    }

    public static boolean unpack(ItemStack Stack, List<ItemStack> Found) {
        return unpack(Stack, Found, 0);
    }

    public static boolean unpack(Container Bag, List<ItemStack> Found) {
        boolean Changed = false;
        for (int Slot = 0; Slot < Bag.getContainerSize(); Slot++) {
            ItemStack Inside = Bag.getItem(Slot);
            if (Inside.isEmpty()) {
                continue;
            }
            if (AnomalyDrops.isAnomalous(Inside)) {
                Bag.setItem(Slot, ItemStack.EMPTY);
                Found.add(Inside);
                Changed = true;
                continue;
            }
            ItemStack Copy = Inside.copy();
            if (unpack(Copy, Found, 1)) {
                Bag.setItem(Slot, Copy);
                Changed = true;
            }
        }
        if (Changed) {
            Bag.setChanged();
        }
        return Changed;
    }

    private static boolean unpack(ItemStack Stack, List<ItemStack> Found, int Depth) {
        if (Stack.isEmpty() || Depth >= MaxDepth || AnomalyDrops.isAnomalous(Stack) || !AnomalyDropConfig.opens(Stack)) {
            return false;
        }
        IItemHandler Handler = Stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (Handler != null) {
            return fromHandler(Handler, Found, Depth);
        }
        boolean Changed = false;
        ItemContainerContents Contents = Stack.get(DataComponents.CONTAINER);
        if (Contents != null) {
            Changed |= fromContents(Stack, Contents, Found, Depth);
        }
        BundleContents Bundle = Stack.get(DataComponents.BUNDLE_CONTENTS);
        if (Bundle != null) {
            Changed |= fromBundle(Stack, Bundle, Found, Depth);
        }
        return Changed;
    }

    private static boolean fromHandler(IItemHandler Handler, List<ItemStack> Found, int Depth) {
        boolean Changed = false;
        for (int Slot = 0; Slot < Handler.getSlots(); Slot++) {
            ItemStack Inside = Handler.getStackInSlot(Slot);
            if (Inside.isEmpty()) {
                continue;
            }
            if (AnomalyDrops.isAnomalous(Inside)) {
                Changed |= pull(Handler, Slot, Found);
                continue;
            }
            if (Handler instanceof IItemHandlerModifiable Modifiable) {
                if (!Modifiable.isItemValid(Slot, Inside)) {
                    continue;
                }
                ItemStack Copy = Inside.copy();
                if (unpack(Copy, Found, Depth + 1)) {
                    Modifiable.setStackInSlot(Slot, Copy);
                    Changed = true;
                }
            } else {
                Changed |= unpack(Inside, Found, Depth + 1);
            }
        }
        return Changed;
    }

    private static boolean pull(IItemHandler Handler, int Slot, List<ItemStack> Found) {
        boolean Changed = false;
        for (int Try = 0; Try < PullTries; Try++) {
            ItemStack Inside = Handler.getStackInSlot(Slot);
            if (Inside.isEmpty() || !AnomalyDrops.isAnomalous(Inside)) {
                return Changed;
            }
            ItemStack Taken = Handler.extractItem(Slot, Inside.getCount(), false);
            if (Taken.isEmpty()) {
                if (Handler instanceof IItemHandlerModifiable Modifiable) {
                    Modifiable.setStackInSlot(Slot, ItemStack.EMPTY);
                    Found.add(Inside.copy());
                    return true;
                }
                return Changed;
            }
            Found.add(Taken);
            Changed = true;
        }
        return Changed;
    }

    private static boolean fromContents(ItemStack Stack, ItemContainerContents Contents, List<ItemStack> Found, int Depth) {
        NonNullList<ItemStack> Slots = NonNullList.withSize(Contents.getSlots(), ItemStack.EMPTY);
        Contents.copyInto(Slots);
        boolean Changed = false;
        for (int Slot = 0; Slot < Slots.size(); Slot++) {
            ItemStack Inside = Slots.get(Slot);
            if (Inside.isEmpty()) {
                continue;
            }
            if (AnomalyDrops.isAnomalous(Inside)) {
                Found.add(Inside);
                Slots.set(Slot, ItemStack.EMPTY);
                Changed = true;
            } else {
                Changed |= unpack(Inside, Found, Depth + 1);
            }
        }
        if (Changed) {
            Stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Slots));
        }
        return Changed;
    }

    private static boolean fromBundle(ItemStack Stack, BundleContents Bundle, List<ItemStack> Found, int Depth) {
        List<ItemStack> Kept = new ArrayList<>();
        boolean Changed = false;
        for (ItemStack Inside : Bundle.itemsCopy()) {
            if (AnomalyDrops.isAnomalous(Inside)) {
                Found.add(Inside);
                Changed = true;
            } else {
                Changed |= unpack(Inside, Found, Depth + 1);
                Kept.add(Inside);
            }
        }
        if (Changed) {
            Stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(Kept));
        }
        return Changed;
    }
}
