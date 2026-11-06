package net.rai.scp_unleashed.scp3008.furniture.children;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp3008FurnitureCStorageEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> Contents;
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
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == Scp3008FurnitureCStorageEntity.this;
        }
    };

    public Scp3008FurnitureCStorageEntity(BlockPos Pos, BlockState State) {
        super(Scp3008FurnitureCRegistry.StorageEntity.get(), Pos, State);
        this.Contents = NonNullList.withSize(rowsOf(State) * 9, ItemStack.EMPTY);
    }

    public static int rowsOf(BlockState State) {
        return State.getBlock() instanceof Scp3008FurnitureCBlock Piece ? Math.max(1, Math.min(3, Piece.kind().Rows)) : 1;
    }

    private void changed(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        if (!(State.getBlock() instanceof Scp3008FurnitureCBlock Piece)) {
            return;
        }
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        switch (Piece.kind()) {
            case ToyChest -> {
                World.playSound(null, Pos, Opened ? Scp3008FurnitureCRegistry.LidOpen.get() : Scp3008FurnitureCRegistry.LidClose.get(), SoundSource.BLOCKS, 0.7f, Pitch);
                if (Opened) {
                    World.playSound(null, Pos, Scp3008FurnitureCRegistry.ToyRattle.get(), SoundSource.BLOCKS, 0.5f, Pitch);
                }
            }
            case CushionBin -> World.playSound(null, Pos, Scp3008FurnitureCRegistry.Rummage.get(), SoundSource.BLOCKS, 0.7f, Pitch);
            default -> World.playSound(null, Pos, Opened ? Scp3008FurnitureCRegistry.BoxOpen.get() : Scp3008FurnitureCRegistry.BoxClose.get(), SoundSource.BLOCKS, 0.7f, Pitch);
        }
        if (State.hasProperty(Scp3008FurnitureCBlock.Open) && State.getValue(Scp3008FurnitureCBlock.Open) != Opened) {
            World.setBlock(Pos, State.setValue(Scp3008FurnitureCBlock.Open, Opened), Block.UPDATE_ALL);
        }
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
