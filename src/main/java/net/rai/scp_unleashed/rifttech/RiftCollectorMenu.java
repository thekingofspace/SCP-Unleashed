package net.rai.scp_unleashed.rifttech;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import org.jetbrains.annotations.Nullable;

public class RiftCollectorMenu extends AbstractContainerMenu {
    public static final int BufferX = 98;
    public static final int BufferY = 58;
    public static final int BufferColumns = 3;
    public static final int InventoryX = 8;
    public static final int InventoryY = 106;
    public static final int HotbarY = 164;
    public static final int MachineSlots = CollectorTuning.BufferSlots;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final RiftCollectorBlockEntity Collector;
    private final ContainerData Data;

    public RiftCollectorMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(CollectorTuning.BufferSlots), new SimpleContainerData(RiftCollectorBlockEntity.DataCount));
    }

    public RiftCollectorMenu(int ContainerId, Inventory PlayerInventory, RiftCollectorBlockEntity Collector, ContainerData Data) {
        this(ContainerId, PlayerInventory, Collector, Collector.getBuffer(), Data);
    }

    private RiftCollectorMenu(int ContainerId, Inventory PlayerInventory, @Nullable RiftCollectorBlockEntity Collector, IItemHandler Handler, ContainerData Data) {
        super(RiftTechRegistry.RiftCollectorMenuType.get(), ContainerId);
        checkContainerDataCount(Data, RiftCollectorBlockEntity.DataCount);
        this.Collector = Collector;
        this.Data = Data;
        for (int Index = 0; Index < CollectorTuning.BufferSlots; Index++) {
            addSlot(new SlotItemHandler(Handler, Index, BufferX + (Index % BufferColumns) * 18, BufferY + (Index / BufferColumns) * 18) {
                @Override
                public boolean mayPlace(ItemStack Stack) {
                    return false;
                }
            });
        }
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, InventoryX + Column * 18, InventoryY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, InventoryX + Column * 18, HotbarY));
        }
        addDataSlots(Data);
    }

    public CollectorStatus getStatus() {
        return CollectorStatus.byIndex(Data.get(RiftCollectorBlockEntity.DataStatus));
    }

    public int getReceived() {
        return Math.max(0, Data.get(RiftCollectorBlockEntity.DataReceived));
    }

    public int getDemand() {
        return Math.max(0, Data.get(RiftCollectorBlockEntity.DataDemand));
    }

    public int getRange() {
        return Math.max(1, Data.get(RiftCollectorBlockEntity.DataRange));
    }

    public float getProgress() {
        return Math.min(1.0f, Math.max(0, Data.get(RiftCollectorBlockEntity.DataProgress)) / (float) RiftCollectorBlockEntity.ProgressScale);
    }

    public int getKind() {
        return Data.get(RiftCollectorBlockEntity.DataKind);
    }

    public boolean isApocalyptic() {
        return getKind() == RiftCollectorBlockEntity.KindApocalyptic;
    }

    @Nullable
    public FragmentRarity getTargetRarity() {
        int Kind = getKind();
        if (Kind <= 0) {
            return null;
        }
        return isApocalyptic() ? FragmentRarity.Abyssal : FragmentRarity.byTier(Kind);
    }

    public float getDistance() {
        return Math.max(0, Data.get(RiftCollectorBlockEntity.DataDistance)) / 10.0f;
    }

    public int getCollected() {
        return Math.max(0, Data.get(RiftCollectorBlockEntity.DataCollected));
    }

    public int getRangeUpgrades() {
        return Math.max(0, Data.get(RiftCollectorBlockEntity.DataUpgrades));
    }

    public int getOffsetX() {
        return Data.get(RiftCollectorBlockEntity.DataOffsetX);
    }

    public int getOffsetZ() {
        return Data.get(RiftCollectorBlockEntity.DataOffsetZ);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < MachineSlots) {
            if (!moveItemStackTo(Stack, MachineSlots, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, MachineSlots, InventoryEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (Stack.isEmpty()) {
            Source.setByPlayer(ItemStack.EMPTY);
        } else {
            Source.setChanged();
        }
        if (Stack.getCount() == Original.getCount()) {
            return ItemStack.EMPTY;
        }
        Source.onTake(User, Stack);
        return Original;
    }

    @Override
    public boolean stillValid(Player User) {
        return Collector == null || Collector.isUsableBy(User);
    }
}
