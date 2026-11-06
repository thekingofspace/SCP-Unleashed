package net.rai.scp_unleashed.fusion;

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
import org.jetbrains.annotations.Nullable;

public class FusionMenu extends AbstractContainerMenu {
    public static final int FuelSlotX = 8;
    public static final int FuelSlotY = 18;
    public static final int InventoryX = 8;
    public static final int InventoryY = 140;
    public static final int HotbarY = 198;
    public static final int MachineSlots = 1;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;
    public static final int StartButton = 0;
    public static final int StopButton = 1;

    @Nullable
    private final FusionReactorBlockEntity Reactor;
    private final ContainerData Data;

    public FusionMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(1), new SimpleContainerData(FusionReactorBlockEntity.DataCount));
    }

    public FusionMenu(int ContainerId, Inventory PlayerInventory, FusionReactorBlockEntity Reactor, ContainerData Data) {
        this(ContainerId, PlayerInventory, Reactor, Reactor.getFuel(), Data);
    }

    private FusionMenu(int ContainerId, Inventory PlayerInventory, @Nullable FusionReactorBlockEntity Reactor, IItemHandler Fuel, ContainerData Data) {
        super(FusionRegistry.ReactorMenu.get(), ContainerId);
        this.Reactor = Reactor;
        this.Data = Data;
        addSlot(new SlotItemHandler(Fuel, 0, FuelSlotX, FuelSlotY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return FusionReactorBlockEntity.isFuel(Stack);
            }
        });
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

    public int value(int Index) {
        return FusionReactorBlockEntity.joinValue(Data, Index);
    }

    public FusionPhase getPhase() {
        return FusionPhase.byIndex(value(FusionReactorBlockEntity.ValuePhase));
    }

    public FusionNotice getNotice() {
        return FusionNotice.byIndex(value(FusionReactorBlockEntity.ValueNotice));
    }

    public float getStability() {
        return Math.min(1.0f, Math.max(0, value(FusionReactorBlockEntity.ValueStability)) / (float) FusionReactorBlockEntity.StabilityScale);
    }

    public float getFuelFraction() {
        return Math.min(1.0f, Math.max(0, value(FusionReactorBlockEntity.ValueFuel)) / (float) FusionReactorBlockEntity.FuelScale);
    }

    public int getWater() {
        return Math.max(0, value(FusionReactorBlockEntity.ValueWater));
    }

    public int getCharge() {
        return Math.max(0, value(FusionReactorBlockEntity.ValueCharge));
    }

    public int getDelivered() {
        return Math.max(0, value(FusionReactorBlockEntity.ValueDelivered));
    }

    public int getIntake() {
        return Math.max(0, value(FusionReactorBlockEntity.ValueIntake));
    }

    public int getInputBus() {
        return Math.max(0, value(FusionReactorBlockEntity.ValueInputBus));
    }

    public int getOutputBus() {
        return Math.max(0, value(FusionReactorBlockEntity.ValueOutputBus));
    }

    public boolean isEnabled() {
        return value(FusionReactorBlockEntity.ValueEnabled) != 0;
    }

    public int getPhaseTicks() {
        return Math.max(0, value(FusionReactorBlockEntity.ValuePhaseTicks));
    }

    public int getFuelUnits() {
        ItemStack Stack = slots.get(0).getItem();
        return FusionReactorBlockEntity.isFuel(Stack) ? Stack.getCount() : 0;
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Reactor == null) {
            return false;
        }
        if (Id == StartButton) {
            Reactor.requestStart();
            return true;
        }
        if (Id == StopButton) {
            Reactor.requestStop();
            return true;
        }
        return false;
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
        } else if (FusionReactorBlockEntity.isFuel(Stack)) {
            if (!moveItemStackTo(Stack, 0, MachineSlots, false)) {
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
        return Reactor == null || Reactor.isUsableBy(User);
    }
}
