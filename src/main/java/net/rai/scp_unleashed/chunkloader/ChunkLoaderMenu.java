package net.rai.scp_unleashed.chunkloader;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ChunkLoaderMenu extends AbstractContainerMenu {
    @Nullable
    private final ChunkLoaderBlockEntity Loader;
    private final ContainerData Data;
    private final BlockPos Pos;

    public ChunkLoaderMenu(int ContainerId, Inventory PlayerInventory, RegistryFriendlyByteBuf Buffer) {
        this(ContainerId, PlayerInventory, null, new SimpleContainerData(ChunkLoaderBlockEntity.DataCount), Buffer.readBlockPos());
    }

    public ChunkLoaderMenu(int ContainerId, Inventory PlayerInventory, ChunkLoaderBlockEntity Loader, ContainerData Data) {
        this(ContainerId, PlayerInventory, Loader, Data, Loader.getBlockPos());
    }

    private ChunkLoaderMenu(int ContainerId, Inventory PlayerInventory, @Nullable ChunkLoaderBlockEntity Loader, ContainerData Data, BlockPos Pos) {
        super(ChunkLoaderRegistry.ChunkLoaderMenuType.get(), ContainerId);
        this.Loader = Loader;
        this.Data = Data;
        this.Pos = Pos.immutable();
        addDataSlots(Data);
    }

    public BlockPos getPos() {
        return Pos;
    }

    public int getRadius() {
        return Data.get(ChunkLoaderBlockEntity.DataRadius);
    }

    public int getMaxRadius() {
        return Data.get(ChunkLoaderBlockEntity.DataMaxRadius);
    }

    public ChunkLoaderStatus getStatus() {
        return ChunkLoaderStatus.byIndex(Data.get(ChunkLoaderBlockEntity.DataStatus));
    }

    public int getLoaded() {
        return Data.get(ChunkLoaderBlockEntity.DataLoaded);
    }

    public boolean isEnabled() {
        return Data.get(ChunkLoaderBlockEntity.DataEnabled) != 0;
    }

    public int getGraceTicks() {
        return Data.get(ChunkLoaderBlockEntity.DataGrace);
    }

    private int joined(int First) {
        return ChunkLoaderBlockEntity.join(Data.get(First), Data.get(First + 1));
    }

    public int getDemand() {
        return joined(ChunkLoaderBlockEntity.DataDemand);
    }

    public int getReceived() {
        return joined(ChunkLoaderBlockEntity.DataReceived);
    }

    public int getBasePower() {
        return joined(ChunkLoaderBlockEntity.DataBase);
    }

    public int getPowerPerChunk() {
        return joined(ChunkLoaderBlockEntity.DataPerChunk);
    }

    public int demandFor(int Radius) {
        return ChunkLoaderArea.demand(Radius, getBasePower(), getPowerPerChunk());
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Loader == null || Id < ChunkLoaderBlockEntity.ButtonShrink || Id > ChunkLoaderBlockEntity.ButtonToggle) {
            return false;
        }
        Loader.pressButton(Id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player User) {
        return Loader == null || Loader.isUsableBy(User);
    }
}
