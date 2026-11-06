package net.rai.scp_unleashed.scp500;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public class Scp500BottleBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<Scp500BottleBlock> BlockCodec = simpleCodec(Scp500BottleBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty Pills = IntegerProperty.create("pills", 0, Scp500Stock.Full);
    public static final String Key = "block.scp_unleashed.scp500_bottle.";
    public static final double Inset = 5.0;
    public static final double BrimInset = 4.0;
    public static final double BodyTop = 8.0;
    public static final double BrimTop = 9.0;
    public static final double Height = 11.0;
    public static final VoxelShape Shape = Shapes.or(Block.box(Inset, 0.0, Inset, 16.0 - Inset, BodyTop, 16.0 - Inset),
            Block.box(BrimInset, BodyTop, BrimInset, 16.0 - BrimInset, BrimTop, 16.0 - BrimInset), Block.box(Inset, BrimTop, Inset, 16.0 - Inset, Height, 16.0 - Inset));
    public static final float TakeVolume = 0.7f;
    public static final float EmptyVolume = 0.4f;

    public Scp500BottleBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Waterlogged, false).setValue(Pills, Scp500Stock.Full));
    }

    public static int pills(BlockState State) {
        return State.hasProperty(Pills) ? State.getValue(Pills) : 0;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Waterlogged, Pills);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader Reader, BlockPos Pos) {
        return Block.canSupportCenter(Reader, Pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (State.getValue(Waterlogged)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
        if (Side == Direction.DOWN && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Scp500Stock Carried = Context.getItemInHand().get(Scp500Registry.Stock.get());
        BlockState Placed = defaultBlockState().setValue(Waterlogged, Context.getLevel().getFluidState(Context.getClickedPos()).getType() == Fluids.WATER)
                .setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Pills, Carried == null ? Scp500Stock.Full : Carried.Pills());
        return Placed.canSurvive(Context.getLevel(), Context.getClickedPos()) ? Placed : null;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return State.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(State);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (User.isSpectator()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server) {
            take(Server, Pos, User);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World instanceof ServerLevel Server) {
            take(Server, Pos, User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static boolean take(ServerLevel World, BlockPos Pos, Player User) {
        Vec3 Mouth = Vec3.atBottomCenterOf(Pos).add(0.0, Height / 16.0, 0.0);
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        if (!(World.getBlockEntity(Pos) instanceof Scp500BottleBlockEntity Bottle) || !Bottle.take()) {
            Scp500Registry.playLayers(World, Mouth, Scp500Registry.EmptyLayers, SoundSource.BLOCKS, EmptyVolume, Pitch);
            User.displayClientMessage(Component.translatable(Key + "empty").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        ItemHandlerHelper.giveItemToPlayer(User, new ItemStack(Scp500Registry.Pill.get()));
        Scp500Registry.playLayers(World, Mouth, Scp500Registry.TakeLayers, SoundSource.BLOCKS, TakeVolume, Pitch);
        World.gameEvent(User, GameEvent.BLOCK_ACTIVATE, Pos);
        return true;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return pills(State) < Scp500Stock.Full;
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (World.getBlockEntity(Pos) instanceof Scp500BottleBlockEntity Bottle) {
            Bottle.refresh();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp500BottleBlockEntity(Pos, State);
    }
}
