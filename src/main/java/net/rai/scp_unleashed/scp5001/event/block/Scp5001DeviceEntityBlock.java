package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class Scp5001DeviceEntityBlock extends Scp5001DeviceBlock implements EntityBlock {
    protected Scp5001DeviceEntityBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide) {
            return null;
        }
        return (TickWorld, TickPos, TickState, Found) -> {
            if (Found instanceof Scp5001DeviceBlockEntity Device) {
                Device.serverTick();
            }
        };
    }
}
