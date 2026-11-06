package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class FleshFixtureBlock extends FleshBlock {
    public static final MapCodec<FleshFixtureBlock> FixtureBlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            propertiesCodec(),
            Fixture.FixtureCodec.fieldOf("fixture").forGetter(Placed -> Placed.Kind)
    ).apply(Instance, FleshFixtureBlock::new));
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;

    protected final Fixture Kind;
    private final Map<Direction, VoxelShape> ShapeByFacing;

    public FleshFixtureBlock(Properties Properties, Fixture Kind) {
        super(Properties);
        this.Kind = Kind;
        this.ShapeByFacing = shapesFor(Kind);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FleshFixtureBlock> codec() {
        return FixtureBlockCodec;
    }

    public Fixture getKind() {
        return Kind;
    }

    public static VoxelShape rotated(Direction Side, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Side) {
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    public static Map<Direction, VoxelShape> shapesFor(Fixture Kind) {
        Map<Direction, VoxelShape> Built = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape Combined = Shapes.empty();
            for (double[] Box : Kind.getBoxes()) {
                Combined = Shapes.or(Combined, rotated(Side, Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
            }
            Built.put(Side, Combined.optimize());
        }
        return Built;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByFacing.get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Mode) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            User.displayClientMessage(Component.translatable(Kind.flavourKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }
}
