package net.rai.scp_unleashed.scp2317;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp2317DoorBlock extends BaseEntityBlock {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 5);
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final int Master = 0;
    public static final int Columns = 2;
    public static final int Rows = 3;
    public static final int Cells = Columns * Rows;
    public static final double OpenLeft = 4.5 / 16.0;
    public static final double OpenRight = 27.5 / 16.0;
    public static final double OpenTop = 36.0 / 16.0;
    public static final double FieldDepth = 0.22;
    public static final String MessagePrefix = "message.scp_unleashed.scp2317.";
    public static final MapCodec<Scp2317DoorBlock> DoorCodec = simpleCodec(Settings -> new Scp2317DoorBlock(Settings, false));
    private final boolean Far;

    public Scp2317DoorBlock(Properties Settings, boolean Far) {
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
        return State.getValue(Open) && State.getValue(Part) < 4 ? 7 : 0;
    }

    public static int column(int Index) {
        return Index % Columns;
    }

    public static int row(int Index) {
        return Index / Columns;
    }

    public static BlockPos cellPos(BlockPos MasterPos, Direction Front, int Index) {
        return MasterPos.relative(Front.getClockWise(), column(Index)).above(row(Index));
    }

    public static BlockPos masterOf(BlockPos Pos, BlockState State) {
        int Index = State.getValue(Part);
        return Pos.relative(State.getValue(Facing).getClockWise(), -column(Index)).below(row(Index));
    }

    public static Vec3 world(BlockPos MasterPos, Direction Front, double Across, double Up, double Depth) {
        int Column = Math.max(0, Math.min(Columns - 1, (int) Math.floor(Across)));
        double LocalX = Across - Column;
        BlockPos Cell = MasterPos.relative(Front.getClockWise(), Column);
        double[] Turned = turn(Front, LocalX, Depth);
        return new Vec3(Cell.getX() + Turned[0], MasterPos.getY() + Up, Cell.getZ() + Turned[1]);
    }

    public static double[] turn(Direction Front, double X, double Z) {
        return switch (Front) {
            case EAST -> new double[]{1.0 - Z, X};
            case SOUTH -> new double[]{1.0 - X, 1.0 - Z};
            case WEST -> new double[]{Z, 1.0 - X};
            default -> new double[]{X, Z};
        };
    }

    public static AABB field(BlockPos MasterPos, Direction Front) {
        Vec3 Low = world(MasterPos, Front, OpenLeft + 0.05, 0.0, 0.5 - FieldDepth);
        Vec3 High = world(MasterPos, Front, OpenRight - 0.05, OpenTop, 0.5 + FieldDepth);
        return new AABB(Low, High);
    }

    @Nullable
    public static Scp2317DoorBlockEntity frame(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp2317DoorBlock)) {
            return null;
        }
        return World.getBlockEntity(masterOf(Pos, State)) instanceof Scp2317DoorBlockEntity Found ? Found : null;
    }

    public static boolean sealed(BlockGetter World, BlockPos Pos, BlockState State) {
        Scp2317DoorBlockEntity Found = frame(World, Pos, State);
        return Found != null && Found.sealed();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        BlockPos MasterPos = Context.getClickedPos();
        for (int Index = 0; Index < Cells; Index++) {
            if (!LargeBlock.fits(Context, cellPos(MasterPos, Front, Index))) {
                return null;
            }
        }
        return defaultBlockState().setValue(Facing, Front).setValue(Part, Master).setValue(Open, false);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (!World.isClientSide && State.getValue(Part) == Master) {
            assemble(World, Pos, State, Block.UPDATE_ALL);
        }
    }

    public static void assemble(LevelAccessor World, BlockPos MasterPos, BlockState Base, int Flags) {
        Direction Front = Base.getValue(Facing);
        for (int Index = 1; Index < Cells; Index++) {
            World.setBlock(cellPos(MasterPos, Front, Index), Base.setValue(Part, Index), Flags);
        }
    }

    public static void placeWhole(LevelAccessor World, BlockPos MasterPos, Direction Front, Block Kind, int Flags) {
        BlockState Base = Kind.defaultBlockState().setValue(Facing, Front).setValue(Open, false).setValue(Part, Master);
        World.setBlock(MasterPos, Base, Flags);
        assemble(World, MasterPos, Base, Flags);
    }

    public static void setOpen(Level World, BlockPos MasterPos, boolean Value) {
        BlockState Base = World.getBlockState(MasterPos);
        if (!(Base.getBlock() instanceof Scp2317DoorBlock) || Base.getValue(Open) == Value) {
            return;
        }
        Direction Front = Base.getValue(Facing);
        for (int Index = 0; Index < Cells; Index++) {
            BlockPos Cell = cellPos(MasterPos, Front, Index);
            BlockState Here = World.getBlockState(Cell);
            if (Here.getBlock() == Base.getBlock()) {
                World.setBlock(Cell, Here.setValue(Open, Value), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private boolean partner(BlockState State, Direction Side, BlockState Neighbour) {
        Direction Front = State.getValue(Facing);
        int Index = State.getValue(Part);
        int Column = column(Index);
        int Row = row(Index);
        int Expected;
        if (Side == Direction.UP) {
            if (Row == Rows - 1) {
                return true;
            }
            Expected = Index + Columns;
        } else if (Side == Direction.DOWN) {
            if (Row == 0) {
                return true;
            }
            Expected = Index - Columns;
        } else if (Side == Front.getClockWise()) {
            if (Column == Columns - 1) {
                return true;
            }
            Expected = Index + 1;
        } else if (Side == Front.getCounterClockWise()) {
            if (Column == 0) {
                return true;
            }
            Expected = Index - 1;
        } else {
            return true;
        }
        return Neighbour.getBlock() == this && Neighbour.getValue(Facing) == Front && Neighbour.getValue(Part) == Expected;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return partner(State, Side, Neighbour) ? State : Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Part) != Master) {
            BlockPos MasterPos = masterOf(Pos, State);
            if (World.getBlockState(MasterPos).getBlock() == this) {
                World.setBlock(MasterPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moving) {
        if (!World.isClientSide && State.getBlock() != Replaced.getBlock() && State.getValue(Part) != Master && !Far) {
            BlockPos MasterPos = masterOf(Pos, State);
            BlockState MasterState = World.getBlockState(MasterPos);
            if (MasterState.getBlock() == this && MasterState.getValue(Facing) == State.getValue(Facing)) {
                Block.dropResources(MasterState, World, MasterPos, World.getBlockEntity(MasterPos));
                World.setBlock(MasterPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
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
            return Scp2317Registry.SealedResistance;
        }
        return super.getExplosionResistance(State, World, Pos, Blast);
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return !Far && !sealed(World, Pos, State) && super.canEntityDestroy(State, World, Pos, Breaker);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        Scp2317DoorBlockEntity Frame = frame(World, Pos, State);
        if (Frame == null) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Frame.use(User);
        return InteractionResult.CONSUME;
    }

    private static VoxelShape box(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        double[] A = turn(Front, X0 / 16.0, Z0 / 16.0);
        double[] B = turn(Front, X1 / 16.0, Z1 / 16.0);
        return Shapes.box(Math.min(A[0], B[0]), Y0 / 16.0, Math.min(A[1], B[1]), Math.max(A[0], B[0]), Y1 / 16.0, Math.max(A[1], B[1]));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return box(State.getValue(Facing), 0.0, 0.0, 1.5, 16.0, 16.0, 14.5);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        Direction Front = State.getValue(Facing);
        int Index = State.getValue(Part);
        int Column = column(Index);
        int Row = row(Index);
        VoxelShape Shape = Shapes.empty();
        if (Column == 0) {
            Shape = Shapes.or(Shape, box(Front, 0.0, 0.0, 2.0, 4.5, 16.0, 14.0));
        } else {
            Shape = Shapes.or(Shape, box(Front, 11.5, 0.0, 2.0, 16.0, 16.0, 14.0));
        }
        if (Row == Rows - 1) {
            Shape = Shapes.or(Shape, box(Front, 0.0, 5.0, 1.0, 16.0, 16.0, 15.0));
        }
        if (Row == 0) {
            Shape = Shapes.or(Shape, box(Front, 0.0, 0.0, 2.0, 16.0, 1.0, 14.0));
        }
        if (!State.getValue(Open)) {
            double Top = Row == Rows - 1 ? 5.0 : 16.0;
            Shape = Shapes.or(Shape, Column == 0 ? box(Front, 4.5, 0.0, 6.5, 16.0, Top, 9.5) : box(Front, 0.0, 0.0, 6.5, 11.5, Top, 9.5));
        }
        return Shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == Master ? new Scp2317DoorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != Master) {
            return null;
        }
        return createTickerHelper(Type, Scp2317Registry.DoorEntity.get(), World.isClientSide ? Scp2317DoorBlockEntity::clientTick : Scp2317DoorBlockEntity::serverTick);
    }

    @Override
    protected boolean triggerEvent(BlockState State, Level World, BlockPos Pos, int Id, int Param) {
        super.triggerEvent(State, World, Pos, Id, Param);
        BlockEntity Found = World.getBlockEntity(Pos);
        return Found != null && Found.triggerEvent(Id, Param);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp2317_door.tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("block.scp_unleashed.scp2317_door.detail").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public static void tell(Player User, String Key) {
        User.displayClientMessage(Component.translatable(MessagePrefix + Key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }
}
