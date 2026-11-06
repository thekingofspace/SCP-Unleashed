package net.rai.scp_unleashed.fission;

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

public class FissionReactorMenu extends AbstractContainerMenu {
    public static final int[] FuelX = {15, 33, 15, 33};
    public static final int[] FuelY = {23, 23, 41, 41};
    public static final int RodX = 59;
    public static final int RodY = 32;
    public static final int[] SpentX = {15, 33};
    public static final int SpentY = 69;
    public static final int IrradiateInX = 15;
    public static final int IrradiateInY = 100;
    public static final int IrradiateOutX = 63;
    public static final int IrradiateOutY = 100;
    public static final int InventoryX = 24;
    public static final int InventoryY = 140;
    public static final int HotbarY = 198;
    public static final int MachineSlots = FissionReactorBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final FissionReactorBlockEntity Reactor;
    private final ContainerData Data;
    private final Player Viewer;

    public FissionReactorMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(FissionReactorBlockEntity.SlotCount), new SimpleContainerData(FissionReactorBlockEntity.DataCount));
    }

    public FissionReactorMenu(int ContainerId, Inventory PlayerInventory, FissionReactorBlockEntity Reactor, ContainerData Data) {
        this(ContainerId, PlayerInventory, Reactor, Reactor.getContents(), Data);
    }

    private FissionReactorMenu(int ContainerId, Inventory PlayerInventory, @Nullable FissionReactorBlockEntity Reactor, IItemHandler Handler, ContainerData Data) {
        super(FissionRegistry.ReactorMenu.get(), ContainerId);
        this.Reactor = Reactor;
        this.Data = Data;
        this.Viewer = PlayerInventory.player;
        for (int Index = 0; Index < FissionReactorBlockEntity.FuelSlots; Index++) {
            addSlot(new SlotItemHandler(Handler, FissionReactorBlockEntity.FirstFuel + Index, FuelX[Index], FuelY[Index]) {
                @Override
                public boolean mayPlace(ItemStack Stack) {
                    return ReactorFuel.isFuel(Stack);
                }
            });
        }
        for (int Index = 0; Index < FissionReactorBlockEntity.SpentSlots; Index++) {
            addSlot(new SlotItemHandler(Handler, FissionReactorBlockEntity.FirstSpent + Index, SpentX[Index], SpentY) {
                @Override
                public boolean mayPlace(ItemStack Stack) {
                    return false;
                }
            });
        }
        addSlot(new SlotItemHandler(Handler, FissionReactorBlockEntity.IrradiateInput, IrradiateInX, IrradiateInY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return FissionReactorBlockEntity.isIrradiatable(Viewer.level(), Stack);
            }
        });
        addSlot(new SlotItemHandler(Handler, FissionReactorBlockEntity.IrradiateOutput, IrradiateOutX, IrradiateOutY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(Handler, FissionReactorBlockEntity.ActiveRod, RodX, RodY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player User) {
                return false;
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

    public int get(int Index) {
        return Data.get(Index);
    }

    public float getPower() {
        return Data.get(FissionReactorBlockEntity.DataPower) / 1000.0f;
    }

    public int getLimit() {
        return Data.get(FissionReactorBlockEntity.DataLimit);
    }

    public int getTemperature() {
        return Data.get(FissionReactorBlockEntity.DataTemperature);
    }

    public boolean isRunning() {
        return Data.get(FissionReactorBlockEntity.DataRunning) != 0;
    }

    public FissionStatus getStatus() {
        return FissionStatus.byIndex(Data.get(FissionReactorBlockEntity.DataStatus));
    }

    public ReactorThermal getThermal() {
        return ReactorThermal.of(getTemperature());
    }

    public float getRodFraction() {
        int Remaining = Data.get(FissionReactorBlockEntity.DataRod);
        return Remaining < 0 ? -1.0f : Remaining / 1000.0f;
    }

    public int getOutput() {
        return Data.get(FissionReactorBlockEntity.DataOutput);
    }

    public int getWaste() {
        return Data.get(FissionReactorBlockEntity.DataWaste);
    }

    public int getGas() {
        return Data.get(FissionReactorBlockEntity.DataGas);
    }

    public float getIrradiation() {
        return Data.get(FissionReactorBlockEntity.DataIrradiation) / 1000.0f;
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Reactor == null || Id < FissionReactorBlockEntity.ButtonToggle || Id > FissionReactorBlockEntity.ButtonRaise) {
            return false;
        }
        Reactor.pressButton(Id, User);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem() || !Source.mayPickup(User)) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        int FuelEnd = FissionReactorBlockEntity.FirstFuel + FissionReactorBlockEntity.FuelSlots;
        if (Index < MachineSlots) {
            if (!moveItemStackTo(Stack, MachineSlots, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ReactorFuel.isFuel(Stack)) {
            if (!moveItemStackTo(Stack, FissionReactorBlockEntity.FirstFuel, FuelEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (FissionReactorBlockEntity.isIrradiatable(User.level(), Stack)) {
            if (!moveItemStackTo(Stack, FissionReactorBlockEntity.IrradiateInput, FissionReactorBlockEntity.IrradiateInput + 1, false)) {
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
