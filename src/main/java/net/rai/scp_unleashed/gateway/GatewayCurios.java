package net.rai.scp_unleashed.gateway;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GatewayCurios {
    private GatewayCurios() {
    }

    public static ListTag capture(ServerPlayer User, HolderLookup.Provider Lookup) {
        ListTag Rows = new ListTag();
        CuriosApi.getCuriosInventory(User).ifPresent(Worn -> {
            for (Map.Entry<String, ICurioStacksHandler> Entry : Worn.getCurios().entrySet()) {
                write(Rows, Entry.getKey(), false, Entry.getValue().getStacks(), Lookup);
                write(Rows, Entry.getKey(), true, Entry.getValue().getCosmeticStacks(), Lookup);
            }
        });
        return Rows;
    }

    private static void write(ListTag Rows, String Kind, boolean Cosmetic, IDynamicStackHandler Stacks, HolderLookup.Provider Lookup) {
        for (int Slot = 0; Slot < Stacks.getSlots(); Slot++) {
            ItemStack Stack = Stacks.getStackInSlot(Slot);
            if (Stack.isEmpty()) {
                continue;
            }
            CompoundTag Row = new CompoundTag();
            Row.putString("Kind", Kind);
            Row.putBoolean("Cosmetic", Cosmetic);
            Row.putInt("Slot", Slot);
            Row.put("Item", Stack.save(Lookup));
            Rows.add(Row);
        }
    }

    public static void wipe(ServerPlayer User) {
        CuriosApi.getCuriosInventory(User).ifPresent(Worn -> {
            for (ICurioStacksHandler Handler : Worn.getCurios().values()) {
                clear(Handler.getStacks());
                clear(Handler.getCosmeticStacks());
            }
        });
    }

    private static void clear(IDynamicStackHandler Stacks) {
        for (int Slot = 0; Slot < Stacks.getSlots(); Slot++) {
            if (!Stacks.getStackInSlot(Slot).isEmpty()) {
                Stacks.setStackInSlot(Slot, ItemStack.EMPTY);
            }
        }
    }

    public static List<ItemStack> restore(ServerPlayer User, ListTag Rows, HolderLookup.Provider Lookup) {
        List<ItemStack> Homeless = new ArrayList<>();
        ICuriosItemHandler Worn = CuriosApi.getCuriosInventory(User).orElse(null);
        for (Tag Raw : Rows) {
            CompoundTag Row = (CompoundTag) Raw;
            ItemStack Stack = ItemStack.parse(Lookup, Row.getCompound("Item")).orElse(ItemStack.EMPTY);
            if (Stack.isEmpty()) {
                continue;
            }
            ICurioStacksHandler Handler = Worn == null ? null : Worn.getCurios().get(Row.getString("Kind"));
            IDynamicStackHandler Stacks = Handler == null ? null : Row.getBoolean("Cosmetic") ? Handler.getCosmeticStacks() : Handler.getStacks();
            int Slot = Row.getInt("Slot");
            if (Stacks != null && Slot >= 0 && Slot < Stacks.getSlots() && Stacks.getStackInSlot(Slot).isEmpty()) {
                Stacks.setStackInSlot(Slot, Stack);
            } else {
                Homeless.add(Stack);
            }
        }
        return Homeless;
    }
}
