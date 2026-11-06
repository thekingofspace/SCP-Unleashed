package net.rai.scp_unleashed.scp5001.props.eng;

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

public class Scp5001EngPropBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp5001EngPropBlock> PropCodec = simpleCodec(Properties -> new Scp5001EngPropBlock(Properties, Scp5001EngCatalog.Entries.get(0)));
    private final Scp5001EngProp Entry;
    private final Map<Direction, VoxelShape> Shaped;

    public Scp5001EngPropBlock(Properties Props, Scp5001EngProp Entry) {
        super(Props);
        this.Entry = Entry;
        this.Shaped = Scp5001EngShapes.horizontal(Entry.lower());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Scp5001EngProp entry() {
        return Entry;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return PropCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Clicked = Context.getClickedFace();
        if (Entry.kind() == Scp5001EngProp.Kind.Wall && Clicked.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACING, Clicked);
        }
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
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
