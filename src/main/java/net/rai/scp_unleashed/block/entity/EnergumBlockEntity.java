package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.WireDevice;

public class EnergumBlockEntity extends BlockEntity implements WireDevice {
    public static final int PowerOutput = 1;

    public EnergumBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.Energum.get(), Pos, State);
    }

    @Override
    public int getPowerOutput() {
        return PowerOutput;
    }

    @Override
    public boolean isGenerator() {
        return true;
    }

    @Override
    public int getGenerationRate() {
        return PowerOutput;
    }
}
