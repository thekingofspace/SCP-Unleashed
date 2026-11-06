package net.rai.scp_unleashed.scp019;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp019PotBlock extends BaseEntityBlock implements LargeBlock {
    public static final MapCodec<Scp019PotBlock> BlockCodec = simpleCodec(Scp019PotBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final float SealedResistance = 1200.0f;
    public static final float OpenResistance = 3600000.0f;
    private static final VoxelShape LowerShape = Shapes.or(Block.box(4, 0, 4, 12, 1, 12), Block.box(3, 1, 3, 13, 3, 13), Block.box(2, 3, 2, 14, 6, 14),
            Block.box(1, 6, 1, 15, 16, 15));
    private static final VoxelShape UpperOpenShape = Shapes.or(Block.box(1, 0, 1, 15, 5, 15), Block.box(2, 5, 2, 14, 7, 14), Block.box(3, 7, 3, 13, 13, 13));
    private static final VoxelShape UpperSealedShape = Shapes.or(UpperOpenShape, Block.box(2, 13, 2, 14, 14, 14), Block.box(4, 14, 4, 12, 15, 12), Block.box(6, 15, 6, 10, 16, 10));

    public Scp019PotBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Open, false));
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half, Open);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        if (State.getValue(Half) == DoubleBlockHalf.LOWER) {
            return LowerShape;
        }
        return State.getValue(Open) ? UpperOpenShape : UpperSealedShape;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above());
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
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Partner = NeighborState.is(this) && NeighborState.getValue(Half) != ThisHalf;
            if (!Partner) {
                return Blocks.AIR.defaultBlockState();
            }
            return State.setValue(Open, NeighborState.getValue(Open)).setValue(Facing, NeighborState.getValue(Facing));
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Half) == DoubleBlockHalf.UPPER) {
            BlockPos LowerPos = Pos.below();
            BlockState LowerState = World.getBlockState(LowerPos);
            if (LowerState.is(this) && LowerState.getValue(Half) == DoubleBlockHalf.LOWER) {
                World.setBlock(LowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, LowerPos, Block.getId(LowerState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        if (State.getValue(Open)) {
            return 0.0f;
        }
        return super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    public float getExplosionResistance(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return State.getValue(Open) ? OpenResistance : SealedResistance;
    }

    @Override
    public boolean canDropFromExplosion(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return !State.getValue(Open);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(lowerPos(State, Pos)) instanceof Scp019PotBlockEntity Pot) {
            Pot.toggle(User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void setOpen(Level World, BlockPos LowerPos, boolean Value) {
        BlockState Lower = World.getBlockState(LowerPos);
        if (!(Lower.getBlock() instanceof Scp019PotBlock)) {
            return;
        }
        World.setBlock(LowerPos, Lower.setValue(Open, Value), Block.UPDATE_ALL);
        BlockState Upper = World.getBlockState(LowerPos.above());
        if (Upper.getBlock() instanceof Scp019PotBlock) {
            World.setBlock(LowerPos.above(), Upper.setValue(Open, Value), Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp019_pot.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp019_pot.toggle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp019_pot.creatures", Scp019PotBlockEntity.MaxDrones, Scp019PotBlockEntity.MaxBrutes).withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("block.scp_unleashed.scp019_pot.sealed").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp019_pot.fixed").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new Scp019PotBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Half) != DoubleBlockHalf.LOWER) {
            return null;
        }
        if (World.isClientSide) {
            return createTickerHelper(Type, Scp019Registry.PotEntity.get(), Scp019PotBlockEntity::clientTick);
        }
        return createTickerHelper(Type, Scp019Registry.PotEntity.get(), Scp019PotBlockEntity::serverTick);
    }
}
