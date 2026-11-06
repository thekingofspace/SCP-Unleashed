package net.rai.scp_unleashed.zoneprops;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.props.PropMounts;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropWater;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class ZoneStackPropBlock extends ZonePropBlock {
    public static final MapCodec<ZoneStackPropBlock> StackCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ZonePropKind.KindCodec.fieldOf("kind").forGetter(ZonePropBlock::getKind), propertiesCodec()).apply(Instance, ZoneStackPropBlock::new));
    public static final EnumProperty<PillarPart> Part = EnumProperty.create("part", PillarPart.class);

    private final Map<PillarPart, Map<Direction, VoxelShape>> PartOutlines = new EnumMap<>(PillarPart.class);

    public ZoneStackPropBlock(ZonePropKind Kind, Properties Properties) {
        super(Kind, Properties);
        for (PillarPart Piece : PillarPart.values()) {
            PartOutlines.put(Piece, PropShapes.horizontal(Kind.getShape(Piece.ordinal())));
        }
        registerDefaultState(defaultBlockState().setValue(Part, PillarPart.Single));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return StackCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Part);
    }

    private boolean onWall() {
        return Kind.getMount() == ZoneMount.WallStack;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState Placed = null;
        for (BlockPos Neighbour : new BlockPos[]{Pos.below(), Pos.above()}) {
            BlockState Other = World.getBlockState(Neighbour);
            if (Other.is(this)) {
                BlockState Candidate = defaultBlockState().setValue(FACING, Other.getValue(FACING));
                if (Candidate.canSurvive(World, Pos)) {
                    Placed = Candidate;
                    break;
                }
            }
        }
        if (Placed == null && onWall()) {
            Direction Facing = PropMounts.wallFacing(Context);
            Placed = Facing == null ? null : defaultBlockState().setValue(FACING, Facing);
        } else if (Placed == null) {
            Placed = defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
        }
        if (Placed == null) {
            return null;
        }
        return PropWater.placed(withPart(Placed, World, Pos), Context);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return !onWall() || PropMounts.onWall(World, Pos, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (onWall() && Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState Updated = super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
        if (Side.getAxis() != Direction.Axis.Y || !Updated.is(this)) {
            return Updated;
        }
        return withPart(Updated, World, Pos);
    }

    private BlockState withPart(BlockState State, BlockGetter World, BlockPos Pos) {
        boolean Below = joins(World.getBlockState(Pos.below()), State);
        boolean Above = joins(World.getBlockState(Pos.above()), State);
        return State.setValue(Part, PillarPart.of(Below, Above));
    }

    private boolean joins(BlockState Other, BlockState State) {
        return Other.is(this) && Other.getValue(FACING) == State.getValue(FACING);
    }

    @Override
    protected VoxelShape outline(BlockState State) {
        return PartOutlines.get(State.getValue(Part)).get(State.getValue(FACING));
    }
}
