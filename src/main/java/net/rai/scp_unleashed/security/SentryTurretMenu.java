package net.rai.scp_unleashed.security;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

public class SentryTurretMenu extends AbstractContainerMenu {
    public static final int AmmoX = 8;
    public static final int AmmoY = 20;
    public static final int InventoryX = 8;
    public static final int InventoryY = 150;
    public static final int HotbarY = 208;
    public static final int AmmoEnd = SentryTurretBlockEntity.AmmoSlots;
    public static final int InventoryEnd = AmmoEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final SentryTurretBlockEntity Turret;
    private final ContainerData Data;
    private final BlockPos Pos;

    public SentryTurretMenu(int ContainerId, Inventory PlayerInventory, RegistryFriendlyByteBuf Buffer) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(SentryTurretBlockEntity.AmmoSlots), new SimpleContainerData(SentryTurretBlockEntity.DataCount),
                Buffer.readBlockPos());
    }

    public SentryTurretMenu(int ContainerId, Inventory PlayerInventory, SentryTurretBlockEntity Turret, ContainerData Data) {
        this(ContainerId, PlayerInventory, Turret, Turret.getAmmo(), Data, Turret.getBlockPos());
    }

    private SentryTurretMenu(int ContainerId, Inventory PlayerInventory, @Nullable SentryTurretBlockEntity Turret, IItemHandler Ammo, ContainerData Data, BlockPos Pos) {
        super(SecurityRegistry.SentryTurretMenuType.get(), ContainerId);
        this.Turret = Turret;
        this.Data = Data;
        this.Pos = Pos;
        for (int Slot = 0; Slot < SentryTurretBlockEntity.AmmoSlots; Slot++) {
            addSlot(new SlotItemHandler(Ammo, Slot, AmmoX + Slot * 18, AmmoY));
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

    public BlockPos getPos() {
        return Pos;
    }

    public TurretStatus getStatus() {
        return TurretStatus.byIndex(Data.get(SentryTurretBlockEntity.DataStatus));
    }

    public int getRounds() {
        return Data.get(SentryTurretBlockEntity.DataRounds);
    }

    @Nullable
    public TurretRound getRound() {
        int Index = Data.get(SentryTurretBlockEntity.DataRound);
        return Index < 0 ? null : TurretRound.byIndex(Index);
    }

    public int getDemand() {
        return Data.get(SentryTurretBlockEntity.DataDemand);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < AmmoEnd) {
            if (!moveItemStackTo(Stack, AmmoEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (TurretRound.of(Stack) != null && moveItemStackTo(Stack, 0, AmmoEnd, false)) {
            Source.setChanged();
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, AmmoEnd, InventoryEnd, false)) {
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
        return Turret == null || Turret.isUsableBy(User) && Turret.canConfigure(User);
    }
}
