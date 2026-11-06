package net.rai.scp_unleashed.props;

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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PropStorageBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> Items;
    private final List<PendingSound> Pending = new ArrayList<>();
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
            return User.containerMenu instanceof ChestMenu Menu && Menu.getContainer() == PropStorageBlockEntity.this;
        }
    };

    public PropStorageBlockEntity(BlockPos Pos, BlockState State) {
        super(PropsRegistry.Storage.get(), Pos, State);
        this.Items = NonNullList.withSize(slotCount(State), ItemStack.EMPTY);
    }

    private static int slotCount(BlockState State) {
        return State.getBlock() instanceof StorageBlock Storage ? Storage.getKind().getSlots() : 27;
    }

    private void changed(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        if (!(State.getBlock() instanceof StorageBlock Storage)) {
            return;
        }
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        Pending.clear();
        for (PropSound Layer : Storage.getKind().getSounds(Opened)) {
            if (Layer.Delay() > 0) {
                Pending.add(new PendingSound(Layer.Event().get(), Layer.Delay(), Pitch));
            } else {
                play(World, State, Pos, Layer.Event().get(), Pitch);
            }
        }
        Storage.setOpen(World, Pos, State, Opened);
    }

    private static void play(Level World, BlockState State, BlockPos Pos, SoundEvent Event, float Pitch) {
        if (State.getBlock() instanceof StorageBlock Storage) {
            Vec3 Where = Storage.soundAt(State, Pos);
            World.playSound(null, Where.x, Where.y, Where.z, Event, SoundSource.BLOCKS, 1.0f, Pitch);
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PropStorageBlockEntity Storage) {
        if (Storage.Pending.isEmpty()) {
            return;
        }
        Iterator<PendingSound> Queue = Storage.Pending.iterator();
        while (Queue.hasNext()) {
            PendingSound Entry = Queue.next();
            Entry.Remaining--;
            if (Entry.Remaining <= 0) {
                play(World, State, Pos, Entry.Event, Entry.Pitch);
                Queue.remove();
            }
        }
    }

    private static final class PendingSound {
        private final SoundEvent Event;
        private final float Pitch;
        private int Remaining;

        private PendingSound(SoundEvent Event, int Remaining, float Pitch) {
            this.Event = Event;
            this.Remaining = Remaining;
            this.Pitch = Pitch;
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
}
