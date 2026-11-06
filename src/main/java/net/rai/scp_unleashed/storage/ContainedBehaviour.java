package net.rai.scp_unleashed.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ContainedBehaviour {
    @Nullable
    default Component refuse(StorageView Locker, ItemStack Offered) {
        return null;
    }

    default void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
    }

    default void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
    }

    @Nullable
    default ItemStack absorb(StorageView Locker, ItemStack Stored, ItemStack Held, Player User) {
        return null;
    }

    default List<ItemStack> unpack(ItemStack Stored) {
        return List.of(Stored);
    }

    @Nullable
    default Split takeOne(ItemStack Stored) {
        return null;
    }

    @Nullable
    default Component occupied(StorageView Locker, ItemStack Stored, ItemStack Held) {
        return null;
    }

    record Split(ItemStack Taken, ItemStack Left) {
    }
}
