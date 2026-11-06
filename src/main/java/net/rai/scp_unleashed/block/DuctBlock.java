package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.entity.DuctBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class DuctBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
    public static final MapCodec<DuctBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            DuctKind.KindCodec.fieldOf("kind").forGetter(DuctBlock::getKind),
            propertiesCodec()
    ).apply(Instance, DuctBlock::new));
    public static final Map<Direction, BooleanProperty> Connections = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;

    private final DuctKind Kind;
    private final VoxelShape[] ShapeByMask;

    public DuctBlock(DuctKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.ShapeByMask = buildShapes(Kind);
        BlockState Default = stateDefinition.any().setValue(Waterlogged, false);
        for (BooleanProperty Connection : Connections.values()) {
            Default = Default.setValue(Connection, false);
        }
        registerDefaultState(Default);
    }

    public DuctKind getKind() {
        return Kind;
    }

    public static int connectionMask(BlockState State) {
        int Mask = 0;
        for (Map.Entry<Direction, BooleanProperty> Entry : Connections.entrySet()) {
            if (State.getValue(Entry.getValue())) {
                Mask |= 1 << Entry.getKey().get3DDataValue();
            }
        }
        return Mask;
    }

    public static boolean isAxial(int Mask) {
        if (Mask == 0) {
            return false;
        }
        for (Direction.Axis Axis : Direction.Axis.values()) {
            int AxisBits = (1 << Direction.get(Direction.AxisDirection.POSITIVE, Axis).get3DDataValue())
                    | (1 << Direction.get(Direction.AxisDirection.NEGATIVE, Axis).get3DDataValue());
            if ((Mask & ~AxisBits) == 0) {
                return true;
            }
        }
        return false;
    }

    private static VoxelShape[] buildShapes(DuctKind Kind) {
        VoxelShape[] Built = new VoxelShape[64];
        for (int Mask = 0; Mask < Built.length; Mask++) {
            Built[Mask] = shapeFor(Kind, Mask);
        }
        return Built;
    }

    private static VoxelShape shapeFor(DuctKind Kind, int Mask) {
        int Arm = Kind.getArmInset();
        int Core = Kind.getCoreInset();
        if (isAxial(Mask)) {
            return run(Direction.from3DDataValue(Integer.numberOfTrailingZeros(Mask)).getAxis(), Arm);
        }
        VoxelShape Shape = Block.box(Core, Core, Core, 16 - Core, 16 - Core, 16 - Core);
        for (Direction Side : Direction.values()) {
            if ((Mask & (1 << Side.get3DDataValue())) != 0) {
                Shape = Shapes.or(Shape, arm(Side, Arm, Core));
            }
        }
        return Shape.optimize();
    }

    private static VoxelShape run(Direction.Axis Axis, int Inset) {
        int High = 16 - Inset;
        return switch (Axis) {
            case X -> Block.box(0, Inset, Inset, 16, High, High);
            case Y -> Block.box(Inset, 0, Inset, High, 16, High);
            case Z -> Block.box(Inset, Inset, 0, High, High, 16);
        };
    }

    private static VoxelShape arm(Direction Side, int Inset, int Core) {
        int High = 16 - Inset;
        return switch (Side) {
            case DOWN -> Block.box(Inset, 0, Inset, High, Core, High);
            case UP -> Block.box(Inset, 16 - Core, Inset, High, 16, High);
            case NORTH -> Block.box(Inset, Inset, 0, High, High, Core);
            case SOUTH -> Block.box(Inset, Inset, 16 - Core, High, High, 16);
            case WEST -> Block.box(0, Inset, Inset, Core, High, High);
            case EAST -> Block.box(16 - Core, Inset, Inset, 16, High, High);
        };
    }

    public boolean acceptsConnection(BlockState State, Direction Side) {
        return true;
    }

    protected boolean connectsTo(BlockState Neighbor, Direction Side) {
        if (Neighbor.getBlock() instanceof DuctBlock Other) {
            return Other.Kind == Kind && Other.acceptsConnection(Neighbor, Side.getOpposite());
        }
        if (Neighbor.getBlock() instanceof PressurizerBlock Pump) {
            return Pump.getKind() == Kind;
        }
        if (Neighbor.getBlock() instanceof CreativePressurizerBlock) {
            return true;
        }
        if (Neighbor.getBlock() instanceof DuctConnectable Port && Port.acceptsDuct(Neighbor, Side.getOpposite(), Kind)) {
            return true;
        }
        if (Kind == DuctKind.Vent && Neighbor.getBlock() instanceof GasPort Port && Port.gasRole(Neighbor, Side.getOpposite()) != null) {
            return true;
        }
        return Kind == DuctKind.Pipe && Neighbor.getBlock() instanceof FluidPort Port && Port.portRole(Neighbor, Side.getOpposite()) != null;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Connections.values().toArray(new BooleanProperty[0]));
        Builder.add(Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = defaultBlockState().setValue(Waterlogged, World.getFluidState(Pos).getType() == Fluids.WATER);
        for (Map.Entry<Direction, BooleanProperty> Entry : Connections.entrySet()) {
            State = State.setValue(Entry.getValue(), connectsTo(World.getBlockState(Pos.relative(Entry.getKey())), Entry.getKey()));
        }
        return State;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (State.getValue(Waterlogged)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
        return State.setValue(Connections.get(Side), connectsTo(NeighborState, Side));
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return State.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(State);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return !State.getValue(Waterlogged);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByMask[connectionMask(State)];
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = "block.scp_unleashed." + Kind.getSerializedName();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.duct.rating", Kind.getRating()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".detail").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new DuctBlockEntity(Pos, State);
    }
}
