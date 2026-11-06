package net.rai.scp_unleashed.scp2845;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp2845PillarBlock extends Block {
    public static final MapCodec<Scp2845PillarBlock> Codec = simpleCodec(Scp2845PillarBlock::new);
    public static final EnumProperty<Segment> Part = EnumProperty.create("segment", Segment.class);
    public static final VoxelShape Shaft = Shapes.or(Block.box(4.0, 0.0, 1.0, 12.0, 16.0, 15.0), Block.box(1.0, 0.0, 4.0, 15.0, 16.0, 12.0));
    public static final VoxelShape Foot = Shapes.or(Shaft, Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0));

    public Scp2845PillarBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Part, Segment.Single));
    }

    public enum Segment implements StringRepresentable {
        Single("single"),
        Base("base"),
        Middle("middle"),
        Top("top");

        private final String Name;

        Segment(String Name) {
            this.Name = Name;
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Part);
    }

    public static Segment segmentFor(boolean Below, boolean Above) {
        if (Below && Above) {
            return Segment.Middle;
        }
        if (Above) {
            return Segment.Base;
        }
        if (Below) {
            return Segment.Top;
        }
        return Segment.Single;
    }

    private boolean pillarAt(BlockGetter World, BlockPos Pos) {
        return World.getBlockState(Pos).is(this);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return defaultBlockState().setValue(Part, segmentFor(pillarAt(Context.getLevel(), Pos.below()), pillarAt(Context.getLevel(), Pos.above())));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side.getAxis() != Direction.Axis.Y) {
            return State;
        }
        return State.setValue(Part, segmentFor(pillarAt(World, Pos.below()), pillarAt(World, Pos.above())));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        Segment Kind = State.getValue(Part);
        return Kind == Segment.Base || Kind == Segment.Single ? Foot : Shaft;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }
}
