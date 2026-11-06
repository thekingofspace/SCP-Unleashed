package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventData;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;

import java.util.Map;

public class Scp5001RodEmitterBlock extends Scp5001DeviceEntityBlock {
    public static final BooleanProperty Spent = BooleanProperty.create("spent");
    public static final Map<Direction, VoxelShape> Shapes0 = Scp5001Shapes.rotations(Shapes.or(Shapes.block(), Block.box(3.0, 3.0, -3.0, 13.0, 13.0, 0.0)));

    public Scp5001RodEmitterBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Spent, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Spent);
    }

    @Override
    protected Map<Direction, VoxelShape> shapes() {
        return Shapes0;
    }

    @Override
    protected boolean facesClickedWall() {
        return true;
    }

    @Override
    public Scp5001EventData.Kind kind() {
        return Scp5001EventData.Kind.Rod;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp5001DeviceBlockEntity(Scp5001EventRegistry.RodEntity.get(), Pos, State);
    }
}
