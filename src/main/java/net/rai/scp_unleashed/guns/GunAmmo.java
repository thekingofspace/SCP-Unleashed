package net.rai.scp_unleashed.guns;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GunAmmo {
    private GunAmmo() {
    }

    public static int count(Player User, GunKind Kind) {
        if (!Kind.usesAmmo()) {
            return 0;
        }
        Item Ammo = Kind.getAmmo();
        if (Ammo == Items.AIR) {
            return 0;
        }
        Inventory Pack = User.getInventory();
        int Total = 0;
        for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
            ItemStack Stack = Pack.getItem(Slot);
            if (Stack.is(Ammo)) {
                Total += Stack.getItem() instanceof AmmoItem Rounds ? Rounds.rounds(Stack) : Stack.getCount();
            }
        }
        return Total;
    }

    public static int take(Player User, GunKind Kind, int Wanted) {
        if (!Kind.usesAmmo() || Wanted <= 0) {
            return 0;
        }
        Item Ammo = Kind.getAmmo();
        if (Ammo == Items.AIR) {
            return 0;
        }
        Inventory Pack = User.getInventory();
        int Taken = 0;
        for (int Slot = Pack.getContainerSize() - 1; Slot >= 0 && Taken < Wanted; Slot--) {
            ItemStack Stack = Pack.getItem(Slot);
            if (!Stack.is(Ammo)) {
                continue;
            }
            Item Kept = Stack.getItem();
            int Used = Kept instanceof AmmoItem Rounds ? Rounds.take(User, Stack, Wanted - Taken) : Math.min(Wanted - Taken, Stack.getCount());
            if (!(Kept instanceof AmmoItem)) {
                Stack.shrink(Used);
            }
            Taken += Used;
        }
        if (Taken > 0) {
            Pack.setChanged();
        }
        return Taken;
    }
}
