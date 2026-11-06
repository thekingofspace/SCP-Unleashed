package net.rai.scp_unleashed.scp3008.furniture.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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

public class Scp3008FurnitureEStorageEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> Contents;
    private final ContainerOpenersCounter Openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level World, BlockPos Pos, BlockState State) {
            sound(World, Pos, true);
        }

        @Override
        protected void onClose(Level World, BlockPos Pos, BlockState State) {
            sound(World, Pos, false);
        }

        @Override
        protected void openerCountChanged(Level World, BlockPos Pos, BlockState State, int Previous, int Count) {
        }

        @Override
        protected boolean isOwnContainer(Player User) {
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == Scp3008FurnitureEStorageEntity.this;
        }
    };

    public Scp3008FurnitureEStorageEntity(BlockPos Pos, BlockState State) {
        super(Scp3008FurnitureERegistry.StorageEntity.get(), Pos, State);
        this.Contents = NonNullList.withSize(rowsOf(State) * 9, ItemStack.EMPTY);
    }

    public static int rowsOf(BlockState State) {
        return State.getBlock() instanceof Scp3008FurnitureEBlock Piece ? Math.max(1, Math.min(3, Piece.kind().Rows)) : 1;
    }

    private void sound(Level World, BlockPos Pos, boolean Opened) {
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        World.playSound(null, Pos, Opened ? Scp3008FurnitureERegistry.DrawerOpen.get() : Scp3008FurnitureERegistry.DrawerClose.get(), SoundSource.BLOCKS, 0.7f, Pitch);
    }

    @Override
    public int getContainerSize() {
        return Contents.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return Contents;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> Stacks) {
        this.Contents = Stacks;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory) {
        return switch (Contents.size() / 9) {
            case 1 -> new ChestMenu(MenuType.GENERIC_9x1, ContainerId, PlayerInventory, this, 1);
            case 2 -> new ChestMenu(MenuType.GENERIC_9x2, ContainerId, PlayerInventory, this, 2);
            default -> ChestMenu.threeRows(ContainerId, PlayerInventory, this);
        };
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        if (!trySaveLootTable(Saved)) {
            ContainerHelper.saveAllItems(Saved, Contents, Registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        this.Contents = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(Saved)) {
            ContainerHelper.loadAllItems(Saved, Contents, Registries);
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
}
