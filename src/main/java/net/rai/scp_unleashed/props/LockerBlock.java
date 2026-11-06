package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class LockerBlock extends StorageBlock implements LargeBlock {
    public static final MapCodec<LockerBlock> BlockCodec = simpleCodec(LockerBlock::new);
    private static final Map<Direction, VoxelShape> LowerOutlines = PropShapes.horizontal(PropShapes.of(box(1, 0, 1, 15, 16, 16)));
    private static final Map<Direction, VoxelShape> UpperOutlines = PropShapes.horizontal(PropShapes.of(box(1, 0, 1, 15, 15, 16)));

    public LockerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false).setValue(TallProps.Half, DoubleBlockHalf.LOWER));
    }

    @Override
    public StorageKind getKind() {
        return StorageKind.Locker;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open, TallProps.Half);
    }

    @Override
    public BlockPos containerPos(BlockState State, BlockPos Pos) {
        return TallProps.lowerPos(State, Pos);
    }

    @Override
    public Vec3 soundAt(BlockState State, BlockPos Pos) {
        return Vec3.atBottomCenterOf(TallProps.lowerPos(State, Pos)).add(0.0, 1.0, 0.0);
    }

    @Override
    public void setOpen(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        BlockPos LowerPos = TallProps.lowerPos(State, Pos);
        for (BlockPos Cell : List.of(LowerPos, LowerPos.above())) {
            BlockState Current = World.getBlockState(Cell);
            if (Current.is(this) && Current.getValue(Open) != Opened) {
                World.setBlock(Cell, Current.setValue(Open, Opened), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        return TallProps.footprint(Context);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        TallProps.placeUpper(World, Pos, State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        BlockState Checked = TallProps.partnerCheck(this, State, Side, NeighborState);
        return Checked != null ? Checked : super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        TallProps.creativeBreak(this, World, Pos, State, User);
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(TallProps.Half) == DoubleBlockHalf.LOWER ? LowerOutlines : UpperOutlines).get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(TallProps.Half) == DoubleBlockHalf.LOWER ? super.newBlockEntity(Pos, State) : null;
    }
}
