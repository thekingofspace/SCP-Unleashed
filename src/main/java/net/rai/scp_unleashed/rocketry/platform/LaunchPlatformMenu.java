package net.rai.scp_unleashed.rocketry.platform;

import net.minecraft.server.level.ServerPlayer;
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
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteItem;
import org.jetbrains.annotations.Nullable;

public class LaunchPlatformMenu extends AbstractContainerMenu {
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int TallInventoryY = 106;
    public static final int TallHotbarY = 164;
    public static final int PayloadX = 129;
    public static final int PayloadY = 35;
    public static final int LaunchButton = 0;
    public static final int PreviousButton = 1;
    public static final int NextButton = 2;

    private final LaunchPlatformKind Kind;
    @Nullable
    private final LaunchPlatformBlockEntity Platform;
    private final ContainerData Data;
    private final int MachineSlots;
    private final int InventoryEnd;
    private final int HotbarEnd;

    public LaunchPlatformMenu(LaunchPlatformKind Kind, int ContainerId, Inventory PlayerInventory) {
        this(Kind, ContainerId, PlayerInventory, null, new SimpleContainerData(LaunchPlatformBlockEntity.DataCount),
                Kind == LaunchPlatformKind.Satellite ? new ItemStackHandler(1) : null);
    }

    public LaunchPlatformMenu(LaunchPlatformKind Kind, int ContainerId, Inventory PlayerInventory, @Nullable LaunchPlatformBlockEntity Platform, ContainerData Data,
                              @Nullable IItemHandler Payload) {
        super(Kind.menuType(), ContainerId);
        this.Kind = Kind;
        this.Platform = Platform;
        this.Data = Data;
        int Added = 0;
        if (Payload != null) {
            addSlot(new SlotItemHandler(Payload, 0, PayloadX, PayloadY) {
                @Override
                public boolean mayPlace(ItemStack Stack) {
                    return SatelliteItem.isPayload(Stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
            Added++;
        }
        MachineSlots = Added;
        int Top = Kind == LaunchPlatformKind.Rocket ? TallInventoryY : InventoryY;
        int Bottom = Kind == LaunchPlatformKind.Rocket ? TallHotbarY : HotbarY;
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, InventoryX + Column * 18, Top + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, InventoryX + Column * 18, Bottom));
        }
        InventoryEnd = MachineSlots + 27;
        HotbarEnd = InventoryEnd + 9;
        addDataSlots(Data);
    }

    public LaunchPlatformKind getKind() {
        return Kind;
    }

    public LaunchStatus getStatus() {
        return LaunchStatus.byIndex(Data.get(LaunchPlatformBlockEntity.DataStatus));
    }

    public int getChecks() {
        return Data.get(LaunchPlatformBlockEntity.DataChecks);
    }

    public boolean passes(LaunchCheck Check) {
        return Check.passes(getChecks());
    }

    public int getFuel() {
        return Data.get(LaunchPlatformBlockEntity.DataFuel);
    }

    public int getPower() {
        int Low = Data.get(LaunchPlatformBlockEntity.DataPowerLow) & LaunchPlatformBlockEntity.PowerMask;
        int High = Data.get(LaunchPlatformBlockEntity.DataPowerHigh) & 0xFFFF;
        return High << LaunchPlatformBlockEntity.PowerSplit | Low;
    }

    public int getReceived() {
        return Data.get(LaunchPlatformBlockEntity.DataReceived);
    }

    public int getDestination() {
        return Data.get(LaunchPlatformBlockEntity.DataDestination);
    }

    public boolean isReady() {
        return getStatus() == LaunchStatus.Ready;
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Platform == null || !(User instanceof ServerPlayer Server)) {
            return false;
        }
        if (Id == LaunchButton) {
            Platform.pressLaunch(Server);
            return true;
        }
        if (Id == PreviousButton || Id == NextButton) {
            Platform.pressCycle(Server, Id == NextButton ? 1 : -1);
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
        } else if (!(MachineSlots > 0 && SatelliteItem.isPayload(Stack) && moveItemStackTo(Stack, 0, MachineSlots, false))) {
            if (Index < InventoryEnd) {
                if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(Stack, MachineSlots, InventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
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
        return Platform == null || Platform.isUsableBy(User);
    }
}
