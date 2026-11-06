package net.rai.scp_unleashed.scp3008.plywood;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp3008PlywoodChestEntity extends RandomizableContainerBlockEntity {
    public static final int Rows = 3;
    public static final int Slots = Rows * 9;
    public static final float OpenVolume = 0.7f;
    public static final float LatchVolume = 0.45f;
    private NonNullList<ItemStack> Contents = NonNullList.withSize(Slots, ItemStack.EMPTY);
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
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == Scp3008PlywoodChestEntity.this;
        }
    };

    public Scp3008PlywoodChestEntity(BlockPos Pos, BlockState State) {
        super(Scp3008PlywoodRegistry.ChestEntity.get(), Pos, State);
    }

    private void changed(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        World.playSound(null, Pos, Opened ? Scp3008PlywoodRegistry.ChestOpen.get() : Scp3008PlywoodRegistry.ChestClose.get(), SoundSource.BLOCKS, OpenVolume, Pitch);
        World.playSound(null, Pos, Scp3008PlywoodRegistry.ChestLatch.get(), SoundSource.BLOCKS, LatchVolume, Opened ? Pitch + 0.1f : Pitch);
        if (State.hasProperty(Scp3008PlywoodChestBlock.Open) && State.getValue(Scp3008PlywoodChestBlock.Open) != Opened) {
            World.setBlock(Pos, State.setValue(Scp3008PlywoodChestBlock.Open, Opened), Block.UPDATE_ALL);
        }
    }

    @Override
    public int getContainerSize() {
        return Slots;
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
        return ChestMenu.threeRows(ContainerId, PlayerInventory, this);
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

    public int openCount() {
        return Openers.getOpenerCount();
    }
}
