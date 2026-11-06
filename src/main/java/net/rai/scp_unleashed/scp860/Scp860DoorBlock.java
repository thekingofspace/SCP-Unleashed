package net.rai.scp_unleashed.scp860;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp860DoorBlock extends BaseEntityBlock implements LiquidBlockContainer {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 8);
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final int Master = 1;
    public static final int Upper = 4;
    public static final float SealedResistance = 3600000.0f;
    public static final String MessagePrefix = "message.scp_unleashed.scp860.";
    public static final int[] Halves = {Master, Upper};
    private static final VoxelShape FrameZ = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
    private static final VoxelShape FrameX = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
    private static final VoxelShape SillZ = Block.box(0.0, 0.0, 3.0, 16.0, 1.5, 13.0);
    private static final VoxelShape SillX = Block.box(3.0, 0.0, 0.0, 13.0, 1.5, 16.0);
    public static final MapCodec<Scp860DoorBlock> DoorCodec = simpleCodec(Settings -> new Scp860DoorBlock(Settings, false));
    private final boolean Far;

    public Scp860DoorBlock(Properties Settings, boolean Far) {
        super(Settings);
        this.Far = Far;
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, Master).setValue(Open, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return DoorCodec;
    }

    public boolean far() {
        return Far;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Open);
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Open) && opening(State.getValue(Part)) ? Scp860Registry.PortalLight : 0;
    }

    public static boolean opening(int Index) {
        return Index == Master || Index == Upper;
    }

    public static int column(int Index) {
        return Index % 3;
    }

    public static int row(int Index) {
        return Index / 3;
    }

    public static BlockPos cellPos(BlockPos MasterPos, Direction Front, int Index) {
        return MasterPos.relative(Front.getClockWise(), column(Index) - 1).above(row(Index));
    }

    public static BlockPos masterOf(BlockPos Pos, BlockState State) {
        int Index = State.getValue(Part);
        return Pos.relative(State.getValue(Facing).getClockWise(), 1 - column(Index)).below(row(Index));
    }

    @Nullable
    public static Scp860DoorBlockEntity frame(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp860DoorBlock)) {
            return null;
        }
        return World.getBlockEntity(masterOf(Pos, State)) instanceof Scp860DoorBlockEntity Found ? Found : null;
    }

    public static boolean sealed(BlockGetter World, BlockPos Pos, BlockState State) {
        Scp860DoorBlockEntity Found = frame(World, Pos, State);
        return Found != null && Found.sealed();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        BlockPos MasterPos = Context.getClickedPos();
        for (int Index : Halves) {
            if (!LargeBlock.fits(Context, cellPos(MasterPos, Front, Index))) {
                return null;
            }
        }
        return defaultBlockState().setValue(Facing, Front).setValue(Part, Master).setValue(Open, false);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (World.isClientSide || State.getValue(Part) != Master) {
            return;
        }
        assemble(World, Pos, State.getValue(Facing), Block.UPDATE_ALL);
    }

    public static void assemble(Level World, BlockPos MasterPos, Direction Front, int Flags) {
        BlockState Base = World.getBlockState(MasterPos);
        if (!(Base.getBlock() instanceof Scp860DoorBlock)) {
            return;
        }
        World.setBlock(cellPos(MasterPos, Front, Upper), Base.setValue(Part, Upper), Flags);
    }

    public static void placeWhole(Level World, BlockPos MasterPos, Direction Front, Block Kind, int Flags) {
        BlockState Base = Kind.defaultBlockState().setValue(Facing, Front).setValue(Open, false);
        World.setBlock(MasterPos, Base.setValue(Part, Master), Flags);
        World.setBlock(cellPos(MasterPos, Front, Upper), Base.setValue(Part, Upper), Flags);
    }

    public static boolean tidyLegacy(Level World, BlockPos MasterPos, Direction Front) {
        boolean Done = true;
        for (int Index = 0; Index < 9; Index++) {
            if (opening(Index)) {
                continue;
            }
            BlockPos Cell = cellPos(MasterPos, Front, Index);
            if (!World.isLoaded(Cell)) {
                Done = false;
                continue;
            }
            BlockState Here = World.getBlockState(Cell);
            if (Here.getBlock() instanceof Scp860DoorBlock && Here.getValue(Part) == Index && Here.getValue(Facing) == Front) {
                World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return Done;
    }

    public static void setOpen(Level World, BlockPos MasterPos, boolean Value) {
        BlockState Base = World.getBlockState(MasterPos);
        if (!(Base.getBlock() instanceof Scp860DoorBlock) || Base.getValue(Open) == Value) {
            return;
        }
        Direction Front = Base.getValue(Facing);
        for (int Index : Halves) {
            BlockPos Cell = cellPos(MasterPos, Front, Index);
            BlockState Here = World.getBlockState(Cell);
            if (Here.getBlock() instanceof Scp860DoorBlock) {
                World.setBlock(Cell, Here.setValue(Open, Value), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        int Index = State.getValue(Part);
        if (!opening(Index)) {
            return Blocks.AIR.defaultBlockState();
        }
        Direction Toward = Index == Master ? Direction.UP : Direction.DOWN;
        if (Side != Toward) {
            return State;
        }
        int Expected = Index == Master ? Upper : Master;
        if (Neighbour.getBlock() == this && Neighbour.getValue(Facing) == State.getValue(Facing) && Neighbour.getValue(Part) == Expected) {
            return State;
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player User, BlockGetter World, BlockPos Pos, BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor World, BlockPos Pos, BlockState State, FluidState Liquid) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Part) != Master) {
            BlockPos MasterPos = masterOf(Pos, State);
            BlockState MasterState = World.getBlockState(MasterPos);
            if (MasterState.getBlock() == this) {
                World.setBlock(MasterPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moving) {
        if (State.getBlock() != Replaced.getBlock() && World.getBlockEntity(Pos) instanceof Scp860DoorBlockEntity Frame) {
            Frame.dropKey();
        }
        super.onRemove(State, World, Pos, Replaced, Moving);
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        if (Far || sealed(World, Pos, State)) {
            return 0.0f;
        }
        return super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    public float getExplosionResistance(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        if (Far || sealed(World, Pos, State)) {
            return SealedResistance;
        }
        return super.getExplosionResistance(State, World, Pos, Blast);
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return !Far && !sealed(World, Pos, State) && super.canEntityDestroy(State, World, Pos, Breaker);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Scp860KeyItem.is(Stack) || Far) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Scp860DoorBlockEntity Frame = frame(World, Pos, State);
        if (Frame == null || Frame.keyed()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            Frame.insert(Stack.split(1), User);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        Scp860DoorBlockEntity Frame = frame(World, Pos, State);
        if (Frame == null) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Frame.use(User);
        return InteractionResult.CONSUME;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        if (!opening(State.getValue(Part))) {
            return Shapes.empty();
        }
        return State.getValue(Facing).getAxis() == Direction.Axis.Z ? FrameZ : FrameX;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        int Index = State.getValue(Part);
        if (!opening(Index)) {
            return Shapes.empty();
        }
        boolean AlongX = State.getValue(Facing).getAxis() == Direction.Axis.Z;
        if (State.getValue(Open)) {
            return Index == Master ? (AlongX ? SillZ : SillX) : Shapes.empty();
        }
        return AlongX ? FrameZ : FrameX;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == Master ? new Scp860DoorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != Master) {
            return null;
        }
        return createTickerHelper(Type, Scp860Registry.DoorEntity.get(), World.isClientSide ? Scp860DoorBlockEntity::clientTick : Scp860DoorBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp860_door.tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("block.scp_unleashed.scp860_door.detail").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public static void tell(Player User, String Key) {
        User.displayClientMessage(Component.translatable(MessagePrefix + Key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    public static boolean isDoor(BlockState State) {
        return State.getBlock() instanceof Scp860DoorBlock;
    }

    public static void serverOnly(Level World, Runnable Work) {
        if (World instanceof ServerLevel) {
            Work.run();
        }
    }
}
