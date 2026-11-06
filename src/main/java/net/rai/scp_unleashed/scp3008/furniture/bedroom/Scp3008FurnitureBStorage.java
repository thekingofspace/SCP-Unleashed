package net.rai.scp_unleashed.scp3008.furniture.bedroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp3008FurnitureBStorage extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> Items;
    private final ContainerOpenersCounter Openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level World, BlockPos Pos, BlockState State) {
            changed(World, Pos, State, true);
        }

        @Override
        protected void onClose(Level World, BlockPos Pos, BlockState State) {
            changed(World, Pos, State, false);
        }

        @Override
        protected void openerCountChanged(Level World, BlockPos Pos, BlockState State, int Previous, int Count) {
        }

        @Override
        protected boolean isOwnContainer(Player User) {
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == Scp3008FurnitureBStorage.this;
        }
    };

    public Scp3008FurnitureBStorage(BlockPos Pos, BlockState State) {
        super(Scp3008FurnitureBRegistry.Storage.get(), Pos, State);
        this.Items = NonNullList.withSize(slots(State), ItemStack.EMPTY);
    }

    public static int slots(BlockState State) {
        return State.getBlock() instanceof Scp3008FurnitureBBlock Furniture && Furniture.piece().storage() ? Furniture.piece().Slots : 9;
    }

    private static void changed(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        if (State.getBlock() instanceof Scp3008FurnitureBBlock Furniture) {
            Furniture.setOpen(World, Pos, Opened);
        }
    }

    @Override
    public int getContainerSize() {
        return Items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return Items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> Stacks) {
        this.Items = Stacks;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory) {
        if (Items.size() == 9) {
            return new ChestMenu(MenuType.GENERIC_9x1, ContainerId, PlayerInventory, this, 1);
        }
        return ChestMenu.threeRows(ContainerId, PlayerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!trySaveLootTable(Tag)) {
            ContainerHelper.saveAllItems(Tag, Items, Registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        this.Items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(Tag)) {
            ContainerHelper.loadAllItems(Tag, Items, Registries);
        }
    }

    @Override
    public void startOpen(Player User) {
        if (!remove && !User.isSpectator() && level != null) {
            Openers.incrementOpeners(User, level, getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player User) {
        if (!remove && !User.isSpectator() && level != null) {
            Openers.decrementOpeners(User, level, getBlockPos(), getBlockState());
        }
    }

    public void recheckOpen() {
        if (!remove && level != null) {
            Openers.recheckOpeners(level, getBlockPos(), getBlockState());
        }
    }

    public int openers() {
        return Openers.getOpenerCount();
    }
}
