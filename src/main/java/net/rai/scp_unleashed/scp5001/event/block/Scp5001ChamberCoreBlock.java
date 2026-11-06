package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventData;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;

import java.util.Map;

public class Scp5001ChamberCoreBlock extends Scp5001DeviceEntityBlock {
    public static final BooleanProperty Breached = BooleanProperty.create("breached");
    public static final Map<Direction, VoxelShape> Shapes0 = Scp5001Shapes.rotations(Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0));

    public Scp5001ChamberCoreBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Breached, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Breached);
    }

    @Override
    protected Map<Direction, VoxelShape> shapes() {
        return Shapes0;
    }

    @Override
    public Scp5001EventData.Kind kind() {
        return Scp5001EventData.Kind.Core;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp5001DeviceBlockEntity(Scp5001EventRegistry.CoreEntity.get(), Pos, State);
    }
}
