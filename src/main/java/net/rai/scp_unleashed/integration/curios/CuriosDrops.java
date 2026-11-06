package net.rai.scp_unleashed.integration.curios;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.anomaly.AnomalyDrops;
import net.rai.scp_unleashed.anomaly.PackedAnomalies;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.List;

public final class CuriosDrops {
    private CuriosDrops() {
    }

    public static void drop(ServerPlayer User, List<ItemStack> Packed) {
        CuriosApi.getCuriosInventory(User).ifPresent(Worn -> {
            for (ICurioStacksHandler Handler : Worn.getCurios().values()) {
                empty(User, Handler.getStacks(), Packed);
                empty(User, Handler.getCosmeticStacks(), Packed);
            }
        });
    }

    private static void empty(ServerPlayer User, IDynamicStackHandler Stacks, List<ItemStack> Packed) {
        for (int Slot = 0; Slot < Stacks.getSlots(); Slot++) {
            ItemStack Stack = Stacks.getStackInSlot(Slot);
            if (AnomalyDrops.isAnomalous(Stack)) {
                Stacks.setStackInSlot(Slot, ItemStack.EMPTY);
                User.drop(Stack, true, false);
            } else if (PackedAnomalies.unpack(Stack, Packed)) {
                Stacks.setStackInSlot(Slot, Stack);
            }
        }
    }
}
