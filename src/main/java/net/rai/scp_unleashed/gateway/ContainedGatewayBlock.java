package net.rai.scp_unleashed.gateway;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ContainedGatewayBlock extends BaseEntityBlock implements LargeBlock, WireHostBlock {
    public static final MapCodec<ContainedGatewayBlock> BlockCodec = simpleCodec(ContainedGatewayBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 5);
    public static final IntegerProperty Glow = IntegerProperty.create("glow", 0, 2);
    public static final int Width = 2;
    public static final int Height = 3;
    public static final int Depth = 1;
    public static final int CellCount = Width * Height * Depth;
    public static final int OriginPart = index(1, 0, 0);
    public static final int MasterPart = OriginPart;
    public static final int PortPart = index(0, 0, 0);
    public static final String Prefix = "block.scp_unleashed.contained_gateway.";
    private static final VoxelShape[] FrameShapes = new VoxelShape[4 * CellCount];
    private static final VoxelShape[] SupportShapes = new VoxelShape[4 * CellCount];
    private static boolean Dismantling;

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Index = 0; Index < CellCount; Index++) {
                VoxelShape Frame = frameShape(Front, Index);
                FrameShapes[slot(Front, Index)] = Frame;
                SupportShapes[slot(Front, Index)] = Index == PortPart ? Frame : Shapes.empty();
            }
        }
    }

    public ContainedGatewayBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, OriginPart).setValue(Glow, 0));
    }

    public static int index(int X, int Y, int Z) {
        return X + Y * Width + Z * Width * Height;
    }

    public static int column(int Index) {
        return Index % Width;
    }

    public static int row(int Index) {
        return Index / Width % Height;
    }

    public static int layer(int Index) {
        return Index / (Width * Height);
    }

    public static VoxelShape shapeOf(Direction Front, int Index) {
        return FrameShapes[slot(Front, Index)];
    }

    public static int lightOf(BlockState State) {
        return switch (State.getValue(Glow)) {
            case 1 -> 5;
            case 2 -> 12;
            default -> 0;
        };
    }

    private static int slot(Direction Front, int Index) {
        return Front.get2DDataValue() * CellCount + Index;
    }

    private static VoxelShape turned(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Front) {
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    private static VoxelShape frameShape(Direction Front, int Index) {
        double CellX = column(Index) * 16.0;
        double CellY = row(Index) * 16.0;
        double CellZ = layer(Index) * 16.0;
        VoxelShape Shape = Shapes.empty();
        for (AABB Piece : GatewayGeometry.Frame) {
            double X0 = Math.max(Piece.minX, CellX) - CellX;
            double Y0 = Math.max(Piece.minY, CellY) - CellY;
            double Z0 = Math.max(Piece.minZ, CellZ) - CellZ;
            double X1 = Math.min(Piece.maxX, CellX + 16.0) - CellX;
            double Y1 = Math.min(Piece.maxY, CellY + 16.0) - CellY;
            double Z1 = Math.min(Piece.maxZ, CellZ + 16.0) - CellZ;
            if (X1 > X0 && Y1 > Y0 && Z1 > Z0) {
                Shape = Shapes.or(Shape, turned(Front, X0, Y0, Z0, X1, Y1, Z1));
            }
        }
        return Shape.optimize();
    }

    private static BlockPos turn(int X, int Y, int Z, Direction Front) {
        return switch (Front) {
            case EAST -> new BlockPos(-Z, Y, X);
            case SOUTH -> new BlockPos(-X, Y, -Z);
            case WEST -> new BlockPos(Z, Y, -X);
            default -> new BlockPos(X, Y, Z);
        };
    }

    private static BlockPos offsetOf(int Index, Direction Front) {
        return turn(column(Index) - column(OriginPart), row(Index), layer(Index), Front);
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Index) {
        return Origin.offset(offsetOf(Index, Front));
    }

    public static BlockPos originOf(BlockPos CellPos, BlockState State) {
        return CellPos.subtract(offsetOf(State.getValue(Part), State.getValue(Facing)));
    }

    public static BlockPos masterOf(BlockPos CellPos, BlockState State) {
        return cellPos(originOf(CellPos, State), State.getValue(Facing), MasterPart);
    }

    public static BlockPos portPos(BlockPos Origin, Direction Front) {
        return cellPos(Origin, Front, PortPart).relative(GatewayGeometry.portSide(Front));
    }

    @Nullable
    public static ContainedGatewayBlockEntity masterEntity(BlockGetter World, BlockPos CellPos, BlockState State) {
        if (!(State.getBlock() instanceof ContainedGatewayBlock)) {
            return null;
        }
        return World.getBlockEntity(masterOf(CellPos, State)) instanceof ContainedGatewayBlockEntity Gate ? Gate : null;
    }

    public static List<BlockPos> footprint(BlockPos Origin, Direction Front) {
        List<BlockPos> Cells = new ArrayList<>(CellCount);
        Cells.add(Origin);
        for (int Index = 0; Index < CellCount; Index++) {
            if (Index != OriginPart) {
                Cells.add(cellPos(Origin, Front, Index));
            }
        }
        return Cells;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        return footprint(Context.getClickedPos(), Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return masterOf(Pos, State);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Glow);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return FrameShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return FrameShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return SupportShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, OriginPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < CellCount; Index++) {
            if (Index != OriginPart) {
                World.setBlock(cellPos(Pos, Front, Index), State.setValue(Part, Index).setValue(Glow, 0), Block.UPDATE_ALL);
            }
        }
    }

    public static void assemble(Level World, BlockPos Origin, Direction Front) {
        BlockState Base = GatewayRegistry.Gateway.get().defaultBlockState().setValue(Facing, Front);
        for (int Index = 0; Index < CellCount; Index++) {
            World.setBlock(cellPos(Origin, Front, Index), Base.setValue(Part, Index), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide) {
            if (State.getValue(Part) == MasterPart && World.getBlockEntity(Pos) instanceof ContainedGatewayBlockEntity Gate) {
                Gate.dropShard(World, Pos);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    BlockPos Origin = originOf(Pos, State);
                    Direction Front = State.getValue(Facing);
                    for (int Index = 0; Index < CellCount; Index++) {
                        BlockPos Cell = cellPos(Origin, Front, Index);
                        if (Cell.equals(Pos)) {
                            continue;
                        }
                        BlockState CellState = World.getBlockState(Cell);
                        if (CellState.is(this) && CellState.getValue(Part) == Index && CellState.getValue(Facing) == Front) {
                            if (Index == MasterPart && World.getBlockEntity(Cell) instanceof ContainedGatewayBlockEntity Gate) {
                                Gate.dropShard(World, Cell);
                            }
                            World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                        }
                    }
                } finally {
                    Dismantling = false;
                }
            }
            if (World instanceof ServerLevel ServerWorld) {
                WireNetworkManager.get(ServerWorld).markDirty();
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(GatewayRegistry.Shard.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ContainedGatewayBlockEntity Gate = masterEntity(World, Pos, State);
        if (Gate == null) {
            return ItemInteractionResult.FAIL;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return Gate.insert(User, Stack) ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        ContainedGatewayBlockEntity Gate = masterEntity(World, Pos, State);
        if (Gate == null || !User.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return Gate.hasShard() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return Gate.takeOut(User) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "draw", ContainedGatewayBlockEntity.StandbyDraw, ContainedGatewayBlockEntity.OpenDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "shard").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == MasterPart ? new ContainedGatewayBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != MasterPart) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, GatewayRegistry.GatewayEntity.get(), ContainedGatewayBlockEntity::clientTick)
                : createTickerHelper(Type, GatewayRegistry.GatewayEntity.get(), ContainedGatewayBlockEntity::serverTick);
    }
}
