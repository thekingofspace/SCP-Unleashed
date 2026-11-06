package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class Scp407SprigBlock extends Scp407GrowthBlock {
    private static final VoxelShape Shape = box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);
    private static final Map<Direction, VoxelShape> Pads = facing(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final VoxelShape[] Outlines = new VoxelShape[64];

    public Scp407SprigBlock(Properties Settings) {
        super(Settings);
        BlockState Start = stateDefinition.any().setValue(Wither, 0).setValue(Variant, 0);
        for (Direction Side : Direction.values()) {
            Start = Scp407Faces.with(Start, Side, false);
        }
        registerDefaultState(Start);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Variant);
        for (Direction Side : Direction.values()) {
            if (Side != Direction.DOWN) {
                Builder.add(Scp407Faces.Sides.get(Side));
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        int Mask = Scp407Faces.mask(State);
        VoxelShape Found = Outlines[Mask];
        if (Found == null) {
            Found = Scp407Faces.union(Shape, Pads, State);
            Outlines[Mask] = Found;
        }
        return Found;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return supported(World, Pos, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == Direction.DOWN && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (Scp407Faces.has(State, Side) && !Scp407Faces.holds(World, Pos, Side)) {
            return Scp407Faces.with(State, Side, false);
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    public boolean soft(BlockState State) {
        return true;
    }

    @Override
    public boolean skirted(BlockState State) {
        return false;
    }

    @Override
    public void carpetFaces(BlockState State, List<Direction> Out) {
        Out.add(Direction.DOWN);
        for (Direction Side : Direction.values()) {
            if (Scp407Faces.has(State, Side)) {
                Out.add(Side);
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return Scp407Faces.rotate(State, Turn);
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return Scp407Faces.mirror(State, Flip);
    }

    @Override
    protected float sporeChance(BlockState State) {
        return 0.06f;
    }

    @Override
    protected float moteChance(BlockState State) {
        return 0.16f;
    }

    @Override
    protected float glintChance(BlockState State) {
        return 0.05f;
    }
}
