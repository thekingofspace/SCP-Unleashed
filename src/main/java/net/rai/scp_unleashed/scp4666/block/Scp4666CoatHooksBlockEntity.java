package net.rai.scp_unleashed.scp4666.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

public class Scp4666CoatHooksBlockEntity extends BlockEntity {
    public static final String[] Names = {"Hat", "Coat", "Boots"};
    private final ItemStack[] Hung = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private int Stocked = Integer.MIN_VALUE;

    public Scp4666CoatHooksBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp4666Registry.CoatHooksEntity.get(), Pos, State);
    }

    public ItemStack get(int Slot) {
        return Hung[Slot];
    }

    public void set(int Slot, ItemStack Stack) {
        Hung[Slot] = Stack;
        setChanged();
    }

    public int stocked() {
        return Stocked;
    }

    public void stock(int Epoch) {
        Stocked = Epoch;
        Hung[0] = new ItemStack(Scp4666Registry.WinterHat.get());
        Hung[1] = new ItemStack(Scp4666Registry.WinterCoat.get());
        Hung[2] = new ItemStack(Scp4666Registry.WinterBoots.get());
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Stocked", Stocked);
        for (int Slot = 0; Slot < Hung.length; Slot++) {
            if (!Hung[Slot].isEmpty()) {
                Tag.put(Names[Slot], Hung[Slot].save(Registries));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Stocked = Tag.contains("Stocked") ? Tag.getInt("Stocked") : Integer.MIN_VALUE;
        for (int Slot = 0; Slot < Hung.length; Slot++) {
            Hung[Slot] = Tag.contains(Names[Slot]) ? ItemStack.parse(Registries, Tag.getCompound(Names[Slot])).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        }
    }
}
