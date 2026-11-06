package net.rai.scp_unleashed.dclass;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DClassPack implements Container {
    public static final int MainCount = DClass.MainSize;
    public static final int ArmorStart = MainCount;
    public static final int OffhandSlot = ArmorStart + 4;
    public static final int Size = OffhandSlot + 1;
    public static final EquipmentSlot[] ArmorOrder = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final DClass Owner;

    public DClassPack(DClass Owner) {
        this.Owner = Owner;
    }

    public DClass owner() {
        return Owner;
    }

    public static EquipmentSlot slotFor(int Index) {
        if (Index == OffhandSlot) {
            return EquipmentSlot.OFFHAND;
        }
        if (Index >= ArmorStart && Index < OffhandSlot) {
            return ArmorOrder[Index - ArmorStart];
        }
        return null;
    }

    @Override
    public int getContainerSize() {
        return Size;
    }

    @Override
    public boolean isEmpty() {
        for (int Index = 0; Index < Size; Index++) {
            if (!getItem(Index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int Index) {
        EquipmentSlot Slot = slotFor(Index);
        if (Slot != null) {
            return Owner.getItemBySlot(Slot);
        }
        return Index >= 0 && Index < MainCount ? Owner.main().get(Index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int Index, int Count) {
        ItemStack Current = getItem(Index);
        if (Current.isEmpty() || Count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack Taken = Current.split(Count);
        if (Current.isEmpty()) {
            setItem(Index, ItemStack.EMPTY);
        }
        setChanged();
        return Taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int Index) {
        ItemStack Current = getItem(Index);
        setItem(Index, ItemStack.EMPTY);
        return Current;
    }

    @Override
    public void setItem(int Index, ItemStack Stack) {
        EquipmentSlot Slot = slotFor(Index);
        if (Slot != null) {
            Owner.setItemSlot(Slot, Stack);
        } else if (Index == Owner.selected()) {
            Owner.setItemSlot(EquipmentSlot.MAINHAND, Stack);
        } else if (Index >= 0 && Index < MainCount) {
            Owner.main().set(Index, Stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player User) {
        return Owner.stillValidFor(User);
    }

    @Override
    public void clearContent() {
        for (int Index = 0; Index < Size; Index++) {
            setItem(Index, ItemStack.EMPTY);
        }
    }
}
