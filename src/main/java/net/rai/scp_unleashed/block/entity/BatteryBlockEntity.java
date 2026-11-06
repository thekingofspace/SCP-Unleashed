package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.BatteryBank;

public class BatteryBlockEntity extends BlockEntity {
    private int Stored;

    public BatteryBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.Battery.get(), Pos, State);
    }

    public int getStored() {
        return Stored;
    }

    public int addPower(int Amount) {
        int Accepted = Math.max(0, Math.min(Amount, BatteryBank.CapacityPerCell - Stored));
        if (Accepted > 0) {
            Stored += Accepted;
            setChanged();
        }
        return Accepted;
    }

    public int takePower(int Amount) {
        int Taken = Math.max(0, Math.min(Amount, Stored));
        if (Taken > 0) {
            Stored -= Taken;
            setChanged();
        }
        return Taken;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Stored", Stored);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Stored = Math.max(0, Math.min(BatteryBank.CapacityPerCell, Tag.getInt("Stored")));
    }
}
