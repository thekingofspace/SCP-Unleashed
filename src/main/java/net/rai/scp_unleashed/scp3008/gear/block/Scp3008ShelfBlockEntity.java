package net.rai.scp_unleashed.scp3008.gear.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
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
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;

public class Scp3008ShelfBlockEntity extends RandomizableContainerBlockEntity {
    public static final int RowSlots = 9;
    private NonNullList<ItemStack> Items;
    private final ContainerOpenersCounter Openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level World, BlockPos Pos, BlockState State) {
            sound(World, Pos, Scp3008GearRegistry.ShelfOpen.get());
        }

        @Override
        protected void onClose(Level World, BlockPos Pos, BlockState State) {
            sound(World, Pos, Scp3008GearRegistry.ShelfClose.get());
        }

        @Override
        protected void openerCountChanged(Level World, BlockPos Pos, BlockState State, int Previous, int Count) {
        }

        @Override
        protected boolean isOwnContainer(Player User) {
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == Scp3008ShelfBlockEntity.this;
        }
    };

    public Scp3008ShelfBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp3008GearRegistry.Shelf.get(), Pos, State);
        this.Items = NonNullList.withSize(rowsOf(State) * RowSlots, ItemStack.EMPTY);
    }

    public static int rowsOf(BlockState State) {
        return State.getBlock() instanceof Scp3008ShelfBlock Shelf ? Shelf.rows() : 1;
    }

    private static void sound(Level World, BlockPos Pos, SoundEvent Event) {
        World.playSound(null, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Event, SoundSource.BLOCKS, 0.6f, 0.95f + World.random.nextFloat() * 0.1f);
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
        if (Items.size() > RowSlots) {
            return new ChestMenu(MenuType.GENERIC_9x2, ContainerId, PlayerInventory, this, 2);
        }
        return new ChestMenu(MenuType.GENERIC_9x1, ContainerId, PlayerInventory, this, 1);
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
}
