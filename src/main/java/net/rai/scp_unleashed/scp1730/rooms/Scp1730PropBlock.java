package net.rai.scp_unleashed.scp1730.rooms;

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
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.scp1730.block.Scp1730PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Scp1730PropBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp1730PropBlock> PropCodec = simpleCodec(Props -> new Scp1730PropBlock(Props, Block.box(4, 0, 4, 12, 16, 12), Block.box(4, 0, 4, 12, 16, 12)));
    private final VoxelShape NorthSouth;
    private final VoxelShape EastWest;
    private final Map<Direction, VoxelShape> Resting = new EnumMap<>(Direction.class);

    public Scp1730PropBlock(Properties Props, VoxelShape NorthSouth, VoxelShape EastWest) {
        this(Props, NorthSouth, EastWest, null);
    }

    public Scp1730PropBlock(Properties Props, VoxelShape NorthSouth, VoxelShape EastWest, @Nullable VoxelShape Rest) {
        super(Props);
        this.NorthSouth = NorthSouth;
        this.EastWest = EastWest;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Resting.put(Side, Rest == null ? (Side.getAxis() == Direction.Axis.Z ? NorthSouth : EastWest) : PropShapes.turn(Rest, Side));
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(FACING).getAxis() == Direction.Axis.Z ? NorthSouth : EastWest;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Scp1730PropShapes.item(Context) ? Resting.get(State.getValue(FACING)) : getShape(State, World, Pos, Context);
    }
}
