package net.rai.scp_unleashed.scp087;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp087RailBlock extends Scp087SealedBlock {
    public static final MapCodec<Scp087RailBlock> RailCodec = simpleCodec(Scp087RailBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Left = BooleanProperty.create("left");
    public static final EnumProperty<Scp087RailShape> Shape = EnumProperty.create("shape", Scp087RailShape.class);
    public static final double Thickness = 3.0;
    private static final VoxelShape NorthSide = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, Thickness);
    private static final VoxelShape SouthSide = Block.box(0.0, 0.0, 16.0 - Thickness, 16.0, 16.0, 16.0);
    private static final VoxelShape WestSide = Block.box(0.0, 0.0, 0.0, Thickness, 16.0, 16.0);
    private static final VoxelShape EastSide = Block.box(16.0 - Thickness, 0.0, 0.0, 16.0, 16.0, 16.0);

    public Scp087RailBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Left, false).setValue(Shape, Scp087RailShape.SlopePost));
    }

    @Override
    protected MapCodec<? extends Scp087RailBlock> codec() {
        return RailCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Left, Shape);
    }

    public static Direction mountSide(BlockState State) {
        Direction Descent = State.getValue(Facing);
        return State.getValue(Left) ? Descent.getCounterClockWise() : Descent.getClockWise();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Descent = Context.getHorizontalDirection();
        Vec3 Hit = Context.getClickLocation().subtract(Vec3.atLowerCornerOf(Context.getClickedPos()));
        Direction Right = Descent.getClockWise();
        double Across = Right.getStepX() * (Hit.x - 0.5) + Right.getStepZ() * (Hit.z - 0.5);
        return defaultBlockState().setValue(Facing, Descent).setValue(Left, Across < 0.0).setValue(Shape, Scp087RailShape.SlopePost);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return switch (mountSide(State)) {
            case NORTH -> NorthSide;
            case SOUTH -> SouthSide;
            case EAST -> EastSide;
            default -> WestSide;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing))).setValue(Left, Flip == Mirror.NONE ? State.getValue(Left) : !State.getValue(Left));
    }
}
