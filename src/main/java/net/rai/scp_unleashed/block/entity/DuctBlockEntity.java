package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;

public class DuctBlockEntity extends BlockEntity {
    private float Pressure;

    public DuctBlockEntity(BlockPos Pos, BlockState State) {
        this(ModBlockEntityTypes.Duct.get(), Pos, State);
    }

    protected DuctBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public float getPressure() {
        return Pressure;
    }

    public void setPressure(float Value) {
        if (Math.abs(Value - Pressure) > 1.0E-3f) {
            Pressure = Value;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (Pressure > 0.0f) {
            Tag.putFloat("Pressure", Pressure);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Pressure = Math.max(0.0f, Tag.getFloat("Pressure"));
    }
}
