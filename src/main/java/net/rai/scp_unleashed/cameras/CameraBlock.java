package net.rai.scp_unleashed.cameras;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CameraBlock extends BaseEntityBlock {
    public static final MapCodec<CameraBlock> BlockCodec = simpleCodec(CameraBlock::new);
    public static final EnumProperty<AttachFace> Face = EnumProperty.create("face", AttachFace.class, AttachFace.WALL, AttachFace.CEILING);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> WallShapes = turned(Shapes.or(box(5, 5, 15, 11, 12, 16), box(7, 7.5, 9, 9, 9, 15), box(5, 9, 3, 11, 15, 12)));
    private static final Map<Direction, VoxelShape> CeilingShapes = turned(Shapes.or(box(5, 15, 9, 11, 16, 16), box(7, 7.5, 9, 9, 15, 15.5), box(5, 9, 3, 11, 15, 12)));

    public CameraBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Face, AttachFace.WALL).setValue(Facing, Direction.NORTH));
    }

    private static Map<Direction, VoxelShape> turned(VoxelShape North) {
        Map<Direction, VoxelShape> Turned = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Turned.put(Side, turn(North, Side));
        }
        return Turned;
    }

    private static VoxelShape turn(VoxelShape North, Direction Side) {
        VoxelShape[] Result = {Shapes.empty()};
        North.forAllBoxes((X0, Y0, Z0, X1, Y1, Z1) -> {
            VoxelShape Piece = switch (Side) {
                case EAST -> Shapes.box(1.0 - Z1, Y0, X0, 1.0 - Z0, Y1, X1);
                case SOUTH -> Shapes.box(1.0 - X1, Y0, 1.0 - Z1, 1.0 - X0, Y1, 1.0 - Z0);
                case WEST -> Shapes.box(Z0, Y0, 1.0 - X1, Z1, Y1, 1.0 - X0);
                default -> Shapes.box(X0, Y0, Z0, X1, Y1, Z1);
            };
            Result[0] = Shapes.or(Result[0], Piece);
        });
        return Result[0].optimize();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Face, Facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    public static Direction support(BlockState State) {
        return State.getValue(Face) == AttachFace.CEILING ? Direction.UP : State.getValue(Facing).getOpposite();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        for (Direction Side : Context.getNearestLookingDirections()) {
            BlockState Candidate;
            if (Side == Direction.UP) {
                Candidate = defaultBlockState().setValue(Face, AttachFace.CEILING).setValue(Facing, Context.getHorizontalDirection().getOpposite());
            } else if (Side.getAxis().isHorizontal()) {
                Candidate = defaultBlockState().setValue(Face, AttachFace.WALL).setValue(Facing, Side.getOpposite());
            } else {
                continue;
            }
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return Candidate;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction Toward = support(State);
        BlockPos Behind = Pos.relative(Toward);
        return World.getBlockState(Behind).isFaceSturdy(World, Behind, Toward.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == support(State) && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Face) == AttachFace.CEILING ? CeilingShapes : WallShapes).get(State.getValue(Facing));
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new CameraBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, CameraRegistry.CameraEntity.get(), (Where, At, Placed, Camera) -> Camera.clientTick()) : null;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.cctv_camera.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.cctv_camera.detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cctv_camera.mount").withStyle(ChatFormatting.GOLD));
        }
    }
}
