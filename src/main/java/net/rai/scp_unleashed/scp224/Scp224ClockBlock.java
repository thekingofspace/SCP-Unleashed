package net.rai.scp_unleashed.scp224;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.TallProps;
import net.rai.scp_unleashed.scp224.client.Scp224ClientHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Scp224ClockBlock extends HorizontalDirectionalBlock implements EntityBlock, LargeBlock {
    public static final MapCodec<Scp224ClockBlock> BlockCodec = simpleCodec(Scp224ClockBlock::new);
    public static final String Key = "block.scp_unleashed.scp224_clock.";
    private static final VoxelShape LowerCase = PropShapes.of(PropShapes.box(1, 0, 3, 15, 2, 15), PropShapes.box(2, 2, 4, 14, 9, 14),
            PropShapes.box(1, 9, 3, 15, 10, 15), PropShapes.box(3, 10, 5, 13, 16, 14));
    private static final VoxelShape UpperCase = PropShapes.of(PropShapes.box(3, 0, 5, 13, 3, 14), PropShapes.box(2, 3, 4, 14, 4, 15),
            PropShapes.box(2, 4, 3, 14, 14, 14), PropShapes.box(1, 14, 3, 15, 15, 15), PropShapes.box(4, 15, 4, 12, 16, 13),
            PropShapes.box(1, 15, 3, 3, 16, 5), PropShapes.box(13, 15, 3, 15, 16, 5));
    private static final Map<Direction, VoxelShape> Lowers = PropShapes.horizontal(LowerCase);
    private static final Map<Direction, VoxelShape> Uppers = PropShapes.horizontal(UpperCase);

    public Scp224ClockBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TallProps.Half, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, TallProps.Half);
    }

    public static boolean isLower(BlockState State) {
        return State.getValue(TallProps.Half) == DoubleBlockHalf.LOWER;
    }

    @Nullable
    public static Scp224ClockBlockEntity clockAt(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof Scp224ClockBlock)) {
            return null;
        }
        BlockEntity Found = World.getBlockEntity(TallProps.lowerPos(State, Pos));
        return Found instanceof Scp224ClockBlockEntity Clock ? Clock : null;
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
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
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
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean Moved) {
        if (World instanceof ServerLevel Server && isLower(State) && !NewState.is(this) && Server.getBlockEntity(Pos) instanceof Scp224ClockBlockEntity Clock) {
            Scp224Aging.releaseAll(Server.getServer(), Clock.getSerial());
        }
        super.onRemove(State, World, Pos, NewState, Moved);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (isLower(State) ? Lowers : Uppers).get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        Scp224ClockBlockEntity Clock = clockAt(World, Pos);
        if (Clock == null) {
            return InteractionResult.PASS;
        }
        if (World instanceof ServerLevel Server) {
            if (Clock.isStilled()) {
                Clock.restart(Server, true);
            } else {
                Clock.still(Server);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "place").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "warning").withStyle(ChatFormatting.RED));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isLower(State) ? new Scp224ClockBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (Type != Scp224Registry.ClockEntity.get() || !isLower(State)) {
            return null;
        }
        if (World.isClientSide) {
            return (Where, At, Current, Holder) -> Scp224ClientHooks.clockTick(Where, At, (Scp224ClockBlockEntity) Holder);
        }
        return (Where, At, Current, Holder) -> Scp224ClockBlockEntity.serverTick((ServerLevel) Where, At, Current, (Scp224ClockBlockEntity) Holder);
    }
}
