package net.rai.scp_unleashed.scp093;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.TallProps;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class Scp093MirrorBlock extends HorizontalDirectionalBlock implements EntityBlock, LargeBlock {
    public static final MapCodec<Scp093MirrorBlock> BlockCodec = simpleCodec(Scp093MirrorBlock::new);
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty Seated = BooleanProperty.create("seated");
    public static final int SeatedLight = 7;
    public static final String Prefix = "message.scp_unleashed.scp093.";
    public static final Vector3f Sheen = new Vector3f(1.0f, 0.55f, 0.18f);
    private static final Map<Direction, VoxelShape> LowerShapes = PropShapes.horizontal(PropShapes.of(PropShapes.box(1, 0, 4, 15, 2, 12), PropShapes.box(1, 2, 6, 15, 16, 10)));
    private static final Map<Direction, VoxelShape> UpperShapes = PropShapes.horizontal(PropShapes.of(PropShapes.box(1, 0, 6, 15, 8, 10), PropShapes.box(2, 8, 6, 14, 16, 10)));

    public Scp093MirrorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Seated, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half, Seated);
    }

    public static BlockPos lower(BlockPos Pos, BlockState State) {
        return TallProps.lowerPos(State, Pos);
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
        if (Checked != null && Checked.is(this) && NeighborState.is(this)) {
            return Checked.setValue(Seated, NeighborState.getValue(Seated));
        }
        return Checked != null ? Checked : super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        TallProps.creativeBreak(this, World, Pos, State, User);
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (!Replaced.is(this) && State.getValue(Half) == DoubleBlockHalf.LOWER && World.getBlockEntity(Pos) instanceof Scp093MirrorBlockEntity Holder && Holder.hasDisc()) {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 1.5, Pos.getZ() + 0.5, Holder.take());
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerShapes : UpperShapes).get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new Scp093MirrorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || State.getValue(Half) != DoubleBlockHalf.LOWER || Type != Scp093Registry.MirrorEntity.get()) {
            return null;
        }
        return (Where, At, Current, Holder) -> Scp093MirrorBlockEntity.serverTick(Where, At, Current, (Scp093MirrorBlockEntity) Holder);
    }

    @Nullable
    private static Scp093MirrorBlockEntity mirrorAt(Level World, BlockPos Lower) {
        return World.getBlockEntity(Lower) instanceof Scp093MirrorBlockEntity Holder ? Holder : null;
    }

    public static void setSeated(Level World, BlockPos Lower, boolean Value) {
        BlockState Base = World.getBlockState(Lower);
        BlockState Top = World.getBlockState(Lower.above());
        if (Base.getBlock() instanceof Scp093MirrorBlock) {
            World.setBlock(Lower, Base.setValue(Seated, Value), Block.UPDATE_ALL);
        }
        if (Top.getBlock() instanceof Scp093MirrorBlock) {
            World.setBlock(Lower.above(), Top.setValue(Seated, Value), Block.UPDATE_ALL);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        BlockPos Lower = lower(Pos, State);
        if (!State.getValue(Seated)) {
            if (!Scp093DiscItem.is(Stack)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!World.isClientSide) {
                Scp093MirrorBlockEntity Holder = mirrorAt(World, Lower);
                if (Holder == null || Holder.hasDisc()) {
                    return ItemInteractionResult.FAIL;
                }
                Holder.insert(Stack);
                Stack.consume(1, User);
                setSeated(World, Lower, true);
                play(World, Lower, Scp093Registry.SeatLayers, 0.9f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (User.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            travel(World, Lower, State, User);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        BlockPos Lower = lower(Pos, State);
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!State.getValue(Seated)) {
            User.displayClientMessage(Component.translatable(Prefix + "mirror_blank").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            World.playSound(null, Lower.above(), Scp093Registry.MirrorBlank.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            return InteractionResult.CONSUME;
        }
        if (User.isSecondaryUseActive()) {
            Scp093MirrorBlockEntity Holder = mirrorAt(World, Lower);
            if (Holder != null && Holder.hasDisc()) {
                ItemStack Taken = Holder.take();
                if (User.getMainHandItem().isEmpty()) {
                    User.setItemInHand(InteractionHand.MAIN_HAND, Taken);
                } else if (!User.getInventory().add(Taken)) {
                    User.drop(Taken, false);
                }
            }
            setSeated(World, Lower, false);
            World.playSound(null, Lower.above(), Scp093Registry.DiscRemove.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
            return InteractionResult.CONSUME;
        }
        travel(World, Lower, State, User);
        return InteractionResult.CONSUME;
    }

    private static void travel(Level World, BlockPos Lower, BlockState State, Player User) {
        if (!(User instanceof ServerPlayer Traveller) || User instanceof FakePlayer || User.isSpectator()) {
            return;
        }
        if (Scp093Wastes.inside(World)) {
            User.displayClientMessage(Component.translatable(Prefix + "mirror_here").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            return;
        }
        if (!Scp093Transit.mirrorReady((ServerLevel) World, Lower)) {
            setSeated(World, Lower, false);
            return;
        }
        Scp093Transit.enterByMirror(Traveller, Lower, State.getValue(FACING));
    }

    private static void play(Level World, BlockPos Lower, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Lower.getX() + 0.5, Lower.getY() + 1.8, Lower.getZ() + 0.5, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Seated) || Random.nextInt(3) != 0) {
            return;
        }
        Direction Front = State.getValue(FACING);
        double Across = (Random.nextDouble() - 0.5) * 0.6;
        double X = Pos.getX() + 0.5 + Front.getStepX() * 0.18 + Front.getClockWise().getStepX() * Across;
        double Z = Pos.getZ() + 0.5 + Front.getStepZ() * 0.18 + Front.getClockWise().getStepZ() * Across;
        double Y = Pos.getY() + 0.2 + Random.nextDouble() * 0.7;
        World.addParticle(new DustParticleOptions(Sheen, 0.6f), X, Y, Z, Front.getStepX() * 0.01, 0.01, Front.getStepZ() * 0.01);
    }

    public static boolean seatedAt(ServerLevel World, BlockPos Lower) {
        return World.getBlockState(Lower).getBlock() instanceof Scp093MirrorBlock && World.getBlockState(Lower).getValue(Seated);
    }
}
