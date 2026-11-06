package net.rai.scp_unleashed.scp001scarlet.throne;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

public class ScarletAnchorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ScarletAnchorBlock> AnchorCodec = simpleCodec(ScarletAnchorBlock::new);
    public static final BooleanProperty Broken = BooleanProperty.create("broken");
    public static final double RingLift = 0.45;
    public static final DustParticleOptions Ember = new DustParticleOptions(new Vector3f(0.85f, 0.12f, 0.08f), 0.7f);
    private static final Map<Direction, VoxelShape> Outlines = new EnumMap<>(Direction.class);

    static {
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            Outlines.put(Facing, Shapes.or(rotate(Facing, 3, 4, 14, 13, 16, 16), rotate(Facing, 7, 8, 7, 9, 14, 14), rotate(Facing, 5, 0, 7, 11, 12, 9)));
        }
    }

    public ScarletAnchorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Broken, false));
    }

    private static VoxelShape rotate(Direction Facing, double X1, double Y1, double Z1, double X2, double Y2, double Z2) {
        return switch (Facing) {
            case SOUTH -> Block.box(16 - X2, Y1, 16 - Z2, 16 - X1, Y2, 16 - Z1);
            case WEST -> Block.box(Z1, Y1, 16 - X2, Z2, Y2, 16 - X1);
            case EAST -> Block.box(16 - Z2, Y1, X1, 16 - Z1, Y2, X2);
            default -> Block.box(X1, Y1, Z1, X2, Y2, Z2);
        };
    }

    public static Vec3 ring(BlockPos Pos, Direction Facing) {
        return Vec3.atCenterOf(Pos).add(0.0, RingLift - 0.5, 0.0).add(Facing.getStepX() * 0.02, 0.0, Facing.getStepZ() * 0.02);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return AnchorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Broken);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Broken) || Random.nextInt(10) != 0) {
            return;
        }
        Vec3 At = ring(Pos, State.getValue(FACING));
        World.addParticle(Ember, At.x + (Random.nextDouble() - 0.5) * 0.3, At.y - 0.2, At.z + (Random.nextDouble() - 0.5) * 0.3, 0.0, -0.01, 0.0);
    }
}
