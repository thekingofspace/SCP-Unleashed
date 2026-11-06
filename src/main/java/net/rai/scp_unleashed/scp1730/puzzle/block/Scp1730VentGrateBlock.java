package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class Scp1730VentGrateBlock extends Scp1730FloorDeviceBlock {
    public static final MapCodec<Scp1730VentGrateBlock> Codec = simpleCodec(Scp1730VentGrateBlock::new);
    public static final BooleanProperty Open = BooleanProperty.create("open");
    private static final Map<Direction, VoxelShape> Frames = new EnumMap<>(Direction.class);

    static {
        Frames.put(Direction.NORTH, Block.box(0, 0, 0, 16, 16, 3));
        Frames.put(Direction.SOUTH, Block.box(0, 0, 13, 16, 16, 16));
        Frames.put(Direction.WEST, Block.box(0, 0, 0, 3, 16, 16));
        Frames.put(Direction.EAST, Block.box(13, 0, 0, 16, 16, 16));
    }

    public Scp1730VentGrateBlock(Properties Props) {
        super(Props, Shapes.block());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Open, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Open);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Open) ? Frames.get(State.getValue(FACING)) : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Open) ? Shapes.empty() : Shapes.block();
    }
}
