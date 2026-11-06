package net.rai.scp_unleashed.furnishing;

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

public class FridgeBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> Contents = NonNullList.withSize(FridgeBlock.Slots, ItemStack.EMPTY);
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
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == FridgeBlockEntity.this;
        }
    };

    public FridgeBlockEntity(BlockPos Pos, BlockState State) {
        super(FurnishingRegistry.FridgeEntity.get(), Pos, State);
    }

    private void changed(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        if (!State.hasProperty(FridgeBlock.Open)) {
            return;
        }
        World.playSound(null, Pos, Opened ? FurnishingSounds.FridgeOpen.get() : FurnishingSounds.FridgeClose.get(), SoundSource.BLOCKS, 0.8f, 0.95f + World.random.nextFloat() * 0.1f);
        World.playSound(null, Pos, FurnishingSounds.BottleClink.get(), SoundSource.BLOCKS, Opened ? 0.8f : 0.5f, 0.9f + World.random.nextFloat() * 0.2f);
        if (State.getValue(FridgeBlock.Open) != Opened) {
            World.setBlock(Pos, State.setValue(FridgeBlock.Open, Opened), Block.UPDATE_ALL);
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
        return new ChestMenu(MenuType.GENERIC_9x2, ContainerId, PlayerInventory, this, 2);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!trySaveLootTable(Tag)) {
            ContainerHelper.saveAllItems(Tag, Contents, Registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        this.Contents = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(Tag)) {
            ContainerHelper.loadAllItems(Tag, Contents, Registries);
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
