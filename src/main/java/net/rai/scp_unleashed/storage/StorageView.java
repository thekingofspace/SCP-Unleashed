package net.rai.scp_unleashed.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface StorageView {
    BlockPos getStoragePos();

    Direction getFacing();

    boolean isTall();

    ItemStack getContents();

    boolean hasContents();

    boolean isOpen();

    boolean hasGlassFront();

    ItemStack getGlassFront();

    StorageEnvironment getEnvironment();

    default boolean isVisible() {
        return isOpen() || hasGlassFront();
    }
}
