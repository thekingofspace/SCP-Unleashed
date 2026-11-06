package net.rai.scp_unleashed.creativeoutlet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.entity.CreativeEnergySourceBlockEntity;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.wire.OutletDevice;

public class CreativeOutletBlockEntity extends OutletBlockEntity implements OutletDevice {
    public static final int Supply = CreativeEnergySourceBlockEntity.Supply;

    public CreativeOutletBlockEntity(BlockPos Pos, BlockState State) {
        super(CreativeOutletRegistry.OutletEntity.get(), Pos, State);
    }

    @Override
    public int getPowerOutput() {
        return Supply;
    }

    @Override
    public boolean isGenerator() {
        return true;
    }

    @Override
    public int getGenerationRate() {
        return Supply;
    }
}
