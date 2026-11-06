package net.rai.scp_unleashed.scp5001.props.ctrl;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class Scp5001CtrlBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp5001CtrlBlock> BlockCodec = simpleCodec(Properties -> new Scp5001CtrlBlock(Properties, Scp5001CtrlProp.Workstation));
    private final Scp5001CtrlProp Entry;
    private final Map<Direction, VoxelShape> Shaped;

    public Scp5001CtrlBlock(Properties Props, Scp5001CtrlProp Entry) {
        super(Props);
        this.Entry = Entry;
        this.Shaped = Scp5001CtrlShapes.build(Entry.lower());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Scp5001CtrlProp entry() {
        return Entry;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Facing = Context.getHorizontalDirection().getOpposite();
        if (Entry.kind() == Scp5001CtrlProp.Kind.Wall && Context.getClickedFace().getAxis().isHorizontal()) {
            Facing = Context.getClickedFace();
        }
        return defaultBlockState().setValue(FACING, Facing);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shaped.get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }
}
