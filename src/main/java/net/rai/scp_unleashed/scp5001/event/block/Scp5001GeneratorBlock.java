package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventData;

import java.util.Map;

public class Scp5001GeneratorBlock extends Scp5001DeviceEntityBlock {
    public static final BooleanProperty Overdrive = BooleanProperty.create("overdrive");
    public static final Map<Direction, VoxelShape> Shapes0 = Scp5001Shapes.rotations(Shapes.or(Block.box(-14.0, 0.0, -14.0, 30.0, 8.0, 30.0),
            Block.box(1.0, 8.0, 1.0, 15.0, 32.0, 15.0), Block.box(-10.0, 8.0, -10.0, -4.0, 32.0, -4.0), Block.box(20.0, 8.0, -10.0, 26.0, 32.0, -4.0),
            Block.box(-10.0, 8.0, 20.0, -4.0, 32.0, 26.0), Block.box(20.0, 8.0, 20.0, 26.0, 32.0, 26.0)));

    public Scp5001GeneratorBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Overdrive, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Overdrive);
    }

    @Override
    protected Map<Direction, VoxelShape> shapes() {
        return Shapes0;
    }

    @Override
    public Scp5001EventData.Kind kind() {
        return Scp5001EventData.Kind.Generator;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp5001GeneratorBlockEntity(Pos, State);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!World.isClientSide) {
            return super.getTicker(World, State, Type);
        }
        return (TickWorld, TickPos, TickState, Found) -> {
            if (Found instanceof Scp5001GeneratorBlockEntity Generator) {
                Generator.clientTick();
            }
        };
    }
}
