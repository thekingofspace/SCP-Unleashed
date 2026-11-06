package net.rai.scp_unleashed.scp3008.furniture.living;

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

public class Scp3008FurnitureAStorageBlockEntity extends RandomizableContainerBlockEntity {
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
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == Scp3008FurnitureAStorageBlockEntity.this;
        }
    };

    public Scp3008FurnitureAStorageBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp3008FurnitureARegistry.Storage.get(), Pos, State);
        this.Items = NonNullList.withSize(slotsOf(State), ItemStack.EMPTY);
    }

    public static int slotsOf(BlockState State) {
        return State.getBlock() instanceof Scp3008FurnitureACabinetBlock Cabinet ? Cabinet.piece().Slots : 9;
    }

    private void changed(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        if (State.getBlock() instanceof Scp3008FurnitureACabinetBlock Cabinet) {
            Cabinet.setOpen(World, Pos, State, Opened);
            Cabinet.voice(World, Pos, State, Opened);
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
        int Rows = Math.max(1, Math.min(3, Items.size() / 9));
        if (Rows == 1) {
            return new ChestMenu(MenuType.GENERIC_9x1, ContainerId, PlayerInventory, this, 1);
        }
        if (Rows == 2) {
            return new ChestMenu(MenuType.GENERIC_9x2, ContainerId, PlayerInventory, this, 2);
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
