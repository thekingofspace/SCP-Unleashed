package net.rai.scp_unleashed.food;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
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

public class VendingMenu extends AbstractContainerMenu {
    public static final int CoinX = 117;
    public static final int CoinY = 51;
    public static final int TrayX = 125;
    public static final int TrayY = 72;
    public static final int InventoryX = 8;
    public static final int InventoryY = 122;
    public static final int HotbarY = 180;
    public static final int CoinSlot = 0;
    public static final int TrayStart = 1;
    public static final int TrayEnd = TrayStart + VendingMachineBlockEntity.TraySlots;
    public static final int InventoryEnd = TrayEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;
    public static final int ReturnButton = Snack.values().length;

    @Nullable
    private final VendingMachineBlockEntity Machine;
    private final SimpleContainer Coins = new SimpleContainer(1);
    private final ContainerData Data;

    public VendingMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(VendingMachineBlockEntity.TraySlots), new SimpleContainerData(VendingMachineBlockEntity.DataCount));
    }

    public VendingMenu(int ContainerId, Inventory PlayerInventory, VendingMachineBlockEntity Machine, ContainerData Data) {
        this(ContainerId, PlayerInventory, Machine, Machine.getTray(), Data);
    }

    private VendingMenu(int ContainerId, Inventory PlayerInventory, @Nullable VendingMachineBlockEntity Machine, IItemHandler Tray, ContainerData Data) {
        super(FoodRegistry.VendingMenuType.get(), ContainerId);
        this.Machine = Machine;
        this.Data = Data;
        Coins.addListener(this::coinsChanged);
        addSlot(new Slot(Coins, 0, CoinX, CoinY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return VendingMachineBlock.isCoin(Stack);
            }
        });
        for (int Index = 0; Index < VendingMachineBlockEntity.TraySlots; Index++) {
            addSlot(new SlotItemHandler(Tray, Index, TrayX + (Index % 2) * 18, TrayY + (Index / 2) * 18) {
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

    private void coinsChanged(Container Changed) {
        if (Machine == null) {
            return;
        }
        ItemStack Stack = Coins.getItem(0);
        if (VendingMachineBlock.isCoin(Stack)) {
            int Taken = Machine.deposit(Stack.getCount());
            if (Taken > 0) {
                Stack.shrink(Taken);
            }
        }
    }

    public int getCredit() {
        return Data.get(VendingMachineBlockEntity.CreditData);
    }

    @Nullable
    public Snack getVending() {
        return Snack.byIndex(Data.get(VendingMachineBlockEntity.VendingData) - 1);
    }

    public int getNotice() {
        return Data.get(VendingMachineBlockEntity.NoticeData);
    }

    public boolean trayAccepts(ItemStack Goods) {
        for (int Index = TrayStart; Index < TrayEnd; Index++) {
            ItemStack Held = slots.get(Index).getItem();
            if (Held.isEmpty() || (ItemStack.isSameItemSameComponents(Held, Goods) && Held.getCount() + Goods.getCount() <= Held.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Machine == null) {
            return false;
        }
        if (Id == ReturnButton) {
            Machine.returnCredit(User);
            return true;
        }
        if (Snack.byIndex(Id) != null) {
            Machine.buy(User, Id);
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
        if (Index < TrayEnd) {
            if (!moveItemStackTo(Stack, TrayEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean Paid = VendingMachineBlock.isCoin(Stack) && moveItemStackTo(Stack, CoinSlot, CoinSlot + 1, false);
            if (!Paid) {
                boolean Shuffled = Index < InventoryEnd ? moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false) : moveItemStackTo(Stack, TrayEnd, InventoryEnd, false);
                if (!Shuffled) {
                    return ItemStack.EMPTY;
                }
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
    public void removed(Player User) {
        super.removed(User);
        clearContainer(User, Coins);
    }

    @Override
    public boolean stillValid(Player User) {
        return Machine == null || Machine.isUsableBy(User);
    }
}
