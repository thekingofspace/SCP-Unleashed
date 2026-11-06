package net.rai.scp_unleashed.scp5001.props.hab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class Scp5001HabPropBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp5001HabPropBlock> PropCodec = simpleCodec(Props -> new Scp5001HabPropBlock(Props, Scp5001HabCatalog.MessStool));
    private final Scp5001HabCatalog Entry;
    private final Map<Direction, VoxelShape> Outlines;

    public Scp5001HabPropBlock(Properties Props, Scp5001HabCatalog Entry) {
        super(Props);
        this.Entry = Entry;
        this.Outlines = Scp5001HabShapes.horizontal(Entry.lower());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Scp5001HabCatalog entry() {
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
        Direction Facing = Context.getHorizontalDirection().getOpposite();
        if (Entry.kind() == Scp5001HabCatalog.Kind.Wall && Context.getClickedFace().getAxis().isHorizontal()) {
            Facing = Context.getClickedFace();
        }
        return defaultBlockState().setValue(FACING, Facing);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }
}
