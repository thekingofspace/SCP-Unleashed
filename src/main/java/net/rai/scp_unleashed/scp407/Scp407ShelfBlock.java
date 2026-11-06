package net.rai.scp_unleashed.scp407;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class Scp407ShelfBlock extends Scp407PlantBlock {
    public static final DirectionProperty Attached = DirectionProperty.create("attached", Direction.Plane.HORIZONTAL);
    private static final Map<Direction, VoxelShape> Outlines = new EnumMap<>(Direction.class);

    static {
        Outlines.put(Direction.NORTH, box(2.0, 3.0, 0.0, 14.0, 12.0, 6.0));
        Outlines.put(Direction.SOUTH, box(2.0, 3.0, 10.0, 14.0, 12.0, 16.0));
        Outlines.put(Direction.WEST, box(0.0, 3.0, 2.0, 6.0, 12.0, 14.0));
        Outlines.put(Direction.EAST, box(10.0, 3.0, 2.0, 16.0, 12.0, 14.0));
    }

    public Scp407ShelfBlock(Properties Settings, Traits Kind) {
        super(Settings, Kind);
        registerDefaultState(stateDefinition.any().setValue(Wither, 0).setValue(Carpet, true).setValue(Attached, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Attached);
    }

    @Override
    public Direction base(BlockState State) {
        return State.getValue(Attached);
    }

    @Override
    protected VoxelShape outline(BlockState State) {
        return Outlines.get(State.getValue(Attached));
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Attached, Turn.rotate(State.getValue(Attached)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Attached, Flip.mirror(State.getValue(Attached)));
    }
}
