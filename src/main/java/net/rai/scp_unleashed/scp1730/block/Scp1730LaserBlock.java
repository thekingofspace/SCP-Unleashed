package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.guns.GunStruckBlock;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Scp1730LaserBlock extends BaseEntityBlock implements GunStruckBlock {
    public static final MapCodec<Scp1730LaserBlock> LaserCodec = simpleCodec(Scp1730LaserBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    public static final BooleanProperty Broken = BooleanProperty.create("broken");
    private static final Map<Direction, VoxelShape> Boxes = new EnumMap<>(Direction.class);

    static {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Boxes.put(Side, Shapes.or(turned(Side, 4, 4, 10.5, 12, 13, 16), turned(Side, 7, 0, 15, 9, 4, 16)));
        }
    }

    public Scp1730LaserBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Powered, true).setValue(Broken, false));
    }

    public static VoxelShape turned(Direction Side, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Side) {
            case SOUTH -> Block.box(16 - X1, Y0, 16 - Z1, 16 - X0, Y1, 16 - Z0);
            case EAST -> Block.box(16 - Z1, Y0, X0, 16 - Z0, Y1, X1);
            case WEST -> Block.box(Z0, Y0, 16 - X1, Z1, Y1, 16 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return LaserCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Powered, Broken);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Face = Context.getClickedFace().getAxis().isHorizontal() ? Context.getClickedFace() : Context.getHorizontalDirection();
        return defaultBlockState().setValue(Facing, Face);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Boxes.get(State.getValue(Facing));
    }

    @Override
    public void onGunHit(ServerLevel World, BlockPos Pos, BlockState State, BlockHitResult Hit, Entity User, float Damage) {
        if (World.getBlockEntity(Pos) instanceof Scp1730LaserBlockEntity Laser) {
            Laser.damage(World, Damage, Hit.getLocation());
        }
    }

    @Override
    protected void onProjectileHit(Level World, BlockState State, BlockHitResult Hit, Projectile Shot) {
        if (World instanceof ServerLevel Server && Shot instanceof AbstractArrow && World.getBlockEntity(Hit.getBlockPos()) instanceof Scp1730LaserBlockEntity Laser) {
            Laser.disable(Server);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Broken) || Random.nextInt(9) != 0) {
            return;
        }
        Direction Side = State.getValue(Facing);
        double X = Pos.getX() + 0.5 - Side.getStepX() * 0.15;
        double Y = Pos.getY() + Scp1730LaserBlockEntity.LensHeight;
        double Z = Pos.getZ() + 0.5 - Side.getStepZ() * 0.15;
        World.addParticle(ParticleTypes.ELECTRIC_SPARK, X, Y, Z, Side.getStepX() * 0.05 + (Random.nextDouble() - 0.5) * 0.08, -0.04,
                Side.getStepZ() * 0.05 + (Random.nextDouble() - 0.5) * 0.08);
        if (Random.nextInt(3) == 0) {
            World.addParticle(ParticleTypes.SMOKE, X, Y + 0.1, Z, 0.0, 0.02, 0.0);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp1730LaserBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, Scp1730Registry.LaserEntity.get(), World.isClientSide ? Scp1730LaserBlockEntity::clientTick : Scp1730LaserBlockEntity::serverTick);
    }
}
