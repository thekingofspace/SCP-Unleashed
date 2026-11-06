package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Scp407PodBlock extends Scp407GrowthBlock {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> Shape = EnumProperty.create("shape", Part.class);
    private static final VoxelShape Shoulder = box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final Map<Direction, VoxelShape> Corners = new EnumMap<>(Direction.class);

    static {
        Corners.put(Direction.NORTH, box(0.0, 0.0, 8.0, 8.0, 16.0, 16.0));
        Corners.put(Direction.EAST, box(0.0, 0.0, 0.0, 8.0, 16.0, 8.0));
        Corners.put(Direction.SOUTH, box(8.0, 0.0, 0.0, 16.0, 16.0, 8.0));
        Corners.put(Direction.WEST, box(8.0, 0.0, 8.0, 16.0, 16.0, 16.0));
    }

    public Scp407PodBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Wither, 0).setValue(Facing, Direction.NORTH).setValue(Shape, Part.Side).setValue(Carpet, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Facing, Shape, Carpet);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return switch (State.getValue(Shape)) {
            case Corner -> Corners.get(State.getValue(Facing));
            case Shoulder -> Shoulder;
            default -> Shapes.block();
        };
    }

    @Nullable
    @Override
    public Direction base(BlockState State) {
        Part Current = State.getValue(Shape);
        return Current == Part.Side || Current == Part.Corner ? Direction.DOWN : null;
    }

    @Override
    public boolean skirted(BlockState State) {
        Part Current = State.getValue(Shape);
        return Current == Part.Corner || Current == Part.Shoulder;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return super.updateShape(refreshCarpet(State, Side, World, Pos), Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected float sporeChance(BlockState State) {
        return 0.05f;
    }

    @Override
    protected float glintChance(BlockState State) {
        return 0.04f;
    }

    public enum Part implements StringRepresentable {
        Side("side"),
        Corner("corner"),
        Cap("cap"),
        Shoulder("shoulder");

        private final String Name;

        Part(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }
}
