package net.rai.scp_unleashed.scp151;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.TallProps;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Scp151PaintingBlock extends HorizontalDirectionalBlock implements EntityBlock, LargeBlock {
    public static final MapCodec<Scp151PaintingBlock> BlockCodec = simpleCodec(Scp151PaintingBlock::new);
    public static final EnumProperty<Scp151Sheet> Cover = EnumProperty.create("cover", Scp151Sheet.class);
    public static final String Key = "block.scp_unleashed.scp151_painting.";
    public static final int WaveChance = 6;
    public static final float WaveVolume = 0.32f;
    private static final VoxelShape LowerEasel = PropShapes.of(PropShapes.box(2, 0, 2, 14, 1, 6), PropShapes.box(3, 1, 3, 5, 16, 5), PropShapes.box(11, 1, 3, 13, 16, 5),
            PropShapes.box(5, 4, 3, 11, 5, 5), PropShapes.box(1, 13, 0, 15, 15, 3), PropShapes.box(0, 15, 2, 16, 16, 3),
            PropShapes.box(7, 0, 12, 9, 6, 16), PropShapes.box(7, 6, 10, 9, 12, 14), PropShapes.box(7, 12, 8, 9, 16, 12));
    private static final VoxelShape UpperEasel = PropShapes.of(PropShapes.box(0, 0, 2, 16, 11, 3), PropShapes.box(3, 0, 3, 5, 14, 5), PropShapes.box(11, 0, 3, 13, 14, 5),
            PropShapes.box(5, 12, 3, 11, 14, 5), PropShapes.box(6, 11, 1, 10, 13, 3), PropShapes.box(7, 0, 4, 9, 13, 9));
    private static final VoxelShape LowerSheet = PropShapes.of(LowerEasel, PropShapes.box(0, 9, 0, 16, 16, 8));
    private static final VoxelShape UpperSheet = PropShapes.of(UpperEasel, PropShapes.box(0, 0, 0, 16, 16, 8));
    private static final Map<Direction, VoxelShape> LowerOpen = PropShapes.horizontal(LowerEasel);
    private static final Map<Direction, VoxelShape> UpperOpen = PropShapes.horizontal(UpperEasel);
    private static final Map<Direction, VoxelShape> LowerDraped = PropShapes.horizontal(LowerSheet);
    private static final Map<Direction, VoxelShape> UpperDraped = PropShapes.horizontal(UpperSheet);

    public Scp151PaintingBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TallProps.Half, DoubleBlockHalf.LOWER).setValue(Cover, Scp151Sheet.None));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, TallProps.Half, Cover);
    }

    public static boolean isLower(BlockState State) {
        return State.getValue(TallProps.Half) == DoubleBlockHalf.LOWER;
    }

    public static boolean isCovered(BlockState State) {
        return State.getValue(Cover).isDrawn();
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
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        boolean Draped = isCovered(State);
        Map<Direction, VoxelShape> Table = isLower(State) ? (Draped ? LowerDraped : LowerOpen) : (Draped ? UpperDraped : UpperOpen);
        return Table.get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        BlockPos Lower = TallProps.lowerPos(State, Pos);
        BlockState Base = World.getBlockState(Lower);
        if (!Base.is(this)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Scp151Sheet Current = Base.getValue(Cover);
        if (Scp151Sheet.isWool(Stack)) {
            Scp151Sheet Wanted = Scp151Sheet.fromWool(Stack);
            if (Wanted == Current) {
                return ItemInteractionResult.CONSUME;
            }
            if (!World.isClientSide) {
                ItemStack Returned = Current.wool();
                drape(World, Lower, Base, Wanted, User);
                Stack.consume(1, User);
                handBack(User, Returned);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (Current.isDrawn() && isShears(Stack)) {
            if (!World.isClientSide) {
                World.playSound(null, Lower.above(), SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.5f, 1.2f);
                lift(World, Lower, Base, User);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        BlockPos Lower = TallProps.lowerPos(State, Pos);
        BlockState Base = World.getBlockState(Lower);
        if (!Base.is(this) || !isCovered(Base) || !User.isSecondaryUseActive() || !User.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            lift(World, Lower, Base, User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static boolean isShears(ItemStack Stack) {
        return Stack.getItem() instanceof ShearsItem || Stack.is(Tags.Items.TOOLS_SHEAR);
    }

    private static void handBack(Player User, ItemStack Returned) {
        if (Returned.isEmpty()) {
            return;
        }
        if (!User.getInventory().add(Returned)) {
            User.drop(Returned, false);
        }
    }

    public static void lift(Level World, BlockPos Lower, BlockState Base, @Nullable Player User) {
        ItemStack Returned = Base.getValue(Cover).wool();
        drape(World, Lower, Base, Scp151Sheet.None, User);
        if (User != null) {
            handBack(User, Returned);
        } else if (!Returned.isEmpty()) {
            Block.popResource(World, Lower.above(), Returned);
        }
    }

    public static void drape(Level World, BlockPos Lower, BlockState Base, Scp151Sheet Sheet, @Nullable Player User) {
        World.setBlock(Lower, Base.setValue(Cover, Sheet), Block.UPDATE_ALL);
        BlockPos Upper = Lower.above();
        BlockState Top = World.getBlockState(Upper);
        if (Top.is(Base.getBlock())) {
            World.setBlock(Upper, Top.setValue(Cover, Sheet), Block.UPDATE_ALL);
        }
        Vec3 Centre = Scp151Gaze.centre(Lower, Base.getValue(FACING));
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp151Registry.RustleLayers) {
            World.playSound(null, Centre.x, Centre.y, Centre.z, Layer.get(), SoundSource.BLOCKS, 0.8f, Sheet.isDrawn() ? 1.0f : 1.15f);
        }
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Lower);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!isLower(State) || isCovered(State) || Random.nextInt(WaveChance) != 0) {
            return;
        }
        Vec3 Centre = Scp151Gaze.centre(Pos, State.getValue(FACING));
        float Pitch = 0.85f + Random.nextFloat() * 0.2f;
        World.playLocalSound(Centre.x, Centre.y, Centre.z, Scp151Registry.Waves.get(), SoundSource.BLOCKS, WaveVolume, Pitch, false);
        World.playLocalSound(Centre.x, Centre.y, Centre.z, Scp151Registry.WavesWash.get(), SoundSource.BLOCKS, WaveVolume, Pitch, false);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "place").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "warning").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable(Key + "storage").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return isLower(State) ? new Scp151PaintingBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || Type != Scp151Registry.PaintingEntity.get() || !isLower(State)) {
            return null;
        }
        return (Where, At, Current, Holder) -> Scp151PaintingBlockEntity.serverTick((ServerLevel) Where, At, Current, (Scp151PaintingBlockEntity) Holder);
    }
}
