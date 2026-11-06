package net.rai.scp_unleashed.labware;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class LabVialRack extends ItemStackHandler {
    public static final int SlotCount = 9;

    public LabVialRack() {
        super(SlotCount);
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return LabVials.isVial(Stack);
    }

    @Override
    public int getSlotLimit(int Slot) {
        return 1;
    }

    public int countVials() {
        int Found = 0;
        for (int Slot = 0; Slot < getSlots(); Slot++) {
            if (!getStackInSlot(Slot).isEmpty()) {
                Found++;
            }
        }
        return Found;
    }

    public int countCultures() {
        int Found = 0;
        for (int Slot = 0; Slot < getSlots(); Slot++) {
            if (LabVials.carriesCulture(getStackInSlot(Slot))) {
                Found++;
            }
        }
        return Found;
    }
}
