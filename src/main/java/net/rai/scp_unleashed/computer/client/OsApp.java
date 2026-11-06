package net.rai.scp_unleashed.computer.client;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface OsApp {
    void render(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H);

    void accept(CompoundTag Data, HolderLookup.Provider Registries);

    default void tick() {
    }

    default boolean back() {
        return false;
    }

    default boolean submit(OsField Field) {
        return false;
    }

    default void opened() {
    }
}
