package net.rai.scp_unleashed.furnishing;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public class TableBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<TableBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            TableKind.KindCodec.fieldOf("kind").forGetter(TableBlock::getKind), propertiesCodec()).apply(Instance, TableBlock::new));
    public static final BooleanProperty North = BlockStateProperties.NORTH;
    public static final BooleanProperty East = BlockStateProperties.EAST;
    public static final BooleanProperty South = BlockStateProperties.SOUTH;
    public static final BooleanProperty West = BlockStateProperties.WEST;

    private final TableKind Kind;
    private final ImmutableMap<BlockState, VoxelShape> Outlines;

    public TableBlock(TableKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        registerDefaultState(stateDefinition.any().setValue(North, false).setValue(East, false).setValue(South, false).setValue(West, false)
                .setValue(FurnishingWater.Waterlogged, false));
        this.Outlines = getShapeForEachState(this::outline);
    }

    public TableKind getKind() {
        return Kind;
    }

    public static BooleanProperty property(Direction Side) {
        return switch (Side) {
            case EAST -> East;
            case SOUTH -> South;
            case WEST -> West;
            default -> North;
        };
    }

    private VoxelShape outline(BlockState State) {
        int Top = Kind.getTopLow();
        VoxelShape Shape = box(0, Top, 0, 16, 16, 16);
        boolean OpenNorth = !State.getValue(North);
        boolean OpenSouth = !State.getValue(South);
        boolean OpenWest = !State.getValue(West);
        boolean OpenEast = !State.getValue(East);
        if (OpenNorth && OpenWest) {
            Shape = FurnishingShapes.of(Shape, box(1, 0, 1, 3, Top, 3));
        }
        if (OpenNorth && OpenEast) {
            Shape = FurnishingShapes.of(Shape, box(13, 0, 1, 15, Top, 3));
        }
        if (OpenSouth && OpenWest) {
            Shape = FurnishingShapes.of(Shape, box(1, 0, 13, 3, Top, 15));
        }
        if (OpenSouth && OpenEast) {
            Shape = FurnishingShapes.of(Shape, box(13, 0, 13, 15, Top, 15));
        }
        return Shape;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(North, East, South, West, FurnishingWater.Waterlogged);
    }

    private BlockState connected(BlockState State, BlockGetter World, BlockPos Pos) {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            State = State.setValue(property(Side), World.getBlockState(Pos.relative(Side)).is(this));
        }
        return State;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return FurnishingWater.placed(connected(defaultBlockState(), Context.getLevel(), Context.getClickedPos()), Context);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        FurnishingWater.update(State, World, Pos);
        if (Side.getAxis().isHorizontal()) {
            return State.setValue(property(Side), NeighborState.is(this));
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return FurnishingWater.fluid(State);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        BlockState Result = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Result = Result.setValue(property(Turn.rotate(Side)), State.getValue(property(Side)));
        }
        return Result;
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        BlockState Result = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Result = Result.setValue(property(Flip.mirror(Side)), State.getValue(property(Side)));
        }
        return Result;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        FurnishingTooltips.add(this, Lines, "joins");
    }
}
