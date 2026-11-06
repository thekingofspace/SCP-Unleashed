package net.rai.scp_unleashed.scp409;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Scp409CaseBlock extends BaseEntityBlock {
    public static final MapCodec<Scp409CaseBlock> BlockCodec = simpleCodec(Scp409CaseBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final float Hardness = 3.0f;
    public static final float Resistance = 1200.0f;
    public static final int OpenGlow = 4;
    public static final float OpenAngle = 95.0f;
    public static final double HingeY = 6.0;
    public static final double HingeZ = 11.0;
    public static final double Reach = 0.5;
    public static final double[] Cavity = {3.0, 2.0, 6.0, 13.0, 16.0, 10.0};
    public static final double[] Outer = {1.0, 0.0, 4.0, 15.0, 8.0, 12.0};
    private static final List<double[]> ClosedBoxes = List.of(
            new double[]{1.0, 0.0, 4.0, 15.0, 2.0, 12.0},
            new double[]{2.0, 2.0, 5.0, 14.0, 8.0, 11.0},
            new double[]{7.0, 3.0, 4.0, 9.0, 8.0, 5.0},
            new double[]{1.0, 3.0, 7.0, 2.0, 4.0, 9.0},
            new double[]{14.0, 3.0, 7.0, 15.0, 4.0, 9.0},
            new double[]{4.0, 4.0, 11.0, 6.0, 7.0, 12.0},
            new double[]{10.0, 4.0, 11.0, 12.0, 7.0, 12.0});
    private static final List<double[]> OpenBoxes = List.of(
            new double[]{1.0, 0.0, 4.0, 15.0, 2.0, 12.0},
            new double[]{2.0, 2.0, 5.0, 14.0, 6.0, 11.0},
            new double[]{2.0, 5.8, 11.0, 14.0, 12.0, 13.6},
            new double[]{7.0, 3.0, 4.0, 9.0, 4.0, 5.0},
            new double[]{1.0, 3.0, 7.0, 2.0, 4.0, 9.0},
            new double[]{14.0, 3.0, 7.0, 15.0, 4.0, 9.0},
            new double[]{4.0, 4.0, 11.0, 6.0, 6.0, 12.0},
            new double[]{10.0, 4.0, 11.0, 12.0, 6.0, 12.0});
    private static final Map<Direction, VoxelShape> ClosedShapes = shapes(ClosedBoxes);
    private static final Map<Direction, VoxelShape> OpenShapes = shapes(OpenBoxes);

    public Scp409CaseBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    public static int glow(BlockState State) {
        return State.getValue(Open) ? OpenGlow : 0;
    }

    public static boolean isOpen(BlockState State) {
        return State.hasProperty(Open) && State.getValue(Open);
    }

    private static Map<Direction, VoxelShape> shapes(List<double[]> Boxes) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape Joined = Shapes.empty();
            for (double[] Box : Boxes) {
                Joined = Shapes.or(Joined, rotated(Box, Side));
            }
            Made.put(Side, Joined.optimize());
        }
        return Made;
    }

    public static VoxelShape rotated(double[] Box, Direction Side) {
        double MinX = Box[0];
        double MinY = Box[1];
        double MinZ = Box[2];
        double MaxX = Box[3];
        double MaxY = Box[4];
        double MaxZ = Box[5];
        return switch (Side) {
            case SOUTH -> box(16.0 - MaxX, MinY, 16.0 - MaxZ, 16.0 - MinX, MaxY, 16.0 - MinZ);
            case EAST -> box(16.0 - MaxZ, MinY, MinX, 16.0 - MinZ, MaxY, MaxX);
            case WEST -> box(MinZ, MinY, 16.0 - MaxX, MaxZ, MaxY, 16.0 - MinX);
            default -> box(MinX, MinY, MinZ, MaxX, MaxY, MaxZ);
        };
    }

    public static Vec3 toLocal(Vec3 Inside, Direction Side) {
        double X = Inside.x * 16.0;
        double Y = Inside.y * 16.0;
        double Z = Inside.z * 16.0;
        return switch (Side) {
            case SOUTH -> new Vec3(16.0 - X, Y, 16.0 - Z);
            case EAST -> new Vec3(Z, Y, 16.0 - X);
            case WEST -> new Vec3(16.0 - Z, Y, X);
            default -> new Vec3(X, Y, Z);
        };
    }

    public static AABB reachBox(BlockState State, BlockPos Pos) {
        return rotated(Outer, State.getValue(Facing)).bounds().move(Pos).inflate(Reach);
    }

    public static boolean reachesCrystal(BlockState State, BlockPos Pos, Vec3 Hit) {
        Vec3 Local = toLocal(Hit.subtract(Pos.getX(), Pos.getY(), Pos.getZ()), State.getValue(Facing));
        double Slack = 0.01;
        return Local.x > Cavity[0] - Slack && Local.x < Cavity[3] + Slack && Local.z > Cavity[2] - Slack && Local.z < Cavity[5] + Slack && Local.y > Cavity[1];
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Open, false);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Open) ? OpenShapes : ClosedShapes).get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (State.getValue(Open) && reachesCrystal(State, Pos, Hit.getLocation())) {
            if (!World.isClientSide) {
                Scp409Exposure.touchCrystal(User);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!World.isClientSide) {
            toggle(World, Pos, State, User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void toggle(Level World, BlockPos Pos, BlockState State, @Nullable Entity User) {
        boolean Opening = !State.getValue(Open);
        World.setBlock(Pos, State.setValue(Open, Opening), Block.UPDATE_ALL);
        World.gameEvent(User, Opening ? GameEvent.CONTAINER_OPEN : GameEvent.CONTAINER_CLOSE, Pos);
        if (World instanceof ServerLevel Server) {
            float Pitch = 0.95f + Server.random.nextFloat() * 0.1f;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Opening ? Scp409Registry.OpenLayers : Scp409Registry.CloseLayers) {
                Server.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, 1.0f, Pitch);
            }
        }
    }

    @Override
    protected void attack(BlockState State, Level World, BlockPos Pos, Player User) {
        if (!World.isClientSide && State.getValue(Open)) {
            Scp409Exposure.touchCrystal(User);
        }
        super.attack(State, World, Pos, User);
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Visitor) {
        if (!World.isClientSide && State.getValue(Open) && Visitor instanceof LivingEntity && Visitor.getBoundingBox().intersects(reachBox(State, Pos))) {
            Scp409Exposure.touchCrystal(Visitor);
        }
    }

    @Override
    public void stepOn(Level World, BlockPos Pos, BlockState State, Entity Visitor) {
        if (!World.isClientSide && State.getValue(Open)) {
            Scp409Exposure.touchCrystal(Visitor);
        }
        super.stepOn(World, Pos, State, Visitor);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp409CaseBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, Scp409Registry.CaseEntity.get(), Scp409CaseBlockEntity::clientTick)
                : createTickerHelper(Type, Scp409Registry.CaseEntity.get(), Scp409CaseBlockEntity::serverTick);
    }
}
