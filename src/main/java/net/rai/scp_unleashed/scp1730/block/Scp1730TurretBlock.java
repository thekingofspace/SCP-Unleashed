package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.guns.GunStruckBlock;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import org.jetbrains.annotations.Nullable;

public class Scp1730TurretBlock extends BaseEntityBlock implements GunStruckBlock {
    public static final MapCodec<Scp1730TurretBlock> TurretCodec = simpleCodec(Scp1730TurretBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final float ArrowDamage = 6.0f;
    public static final float TridentDamage = 12.0f;
    private static final VoxelShape Hanging = Block.box(3, 4, 3, 13, 16, 13);

    public Scp1730TurretBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return TurretCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection());
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return World.getBlockState(Pos.above()).isFaceSturdy(World, Pos.above(), Direction.DOWN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Hanging;
    }

    @Override
    public void onGunHit(ServerLevel World, BlockPos Pos, BlockState State, BlockHitResult Hit, Entity User, float Damage) {
        if (World.getBlockEntity(Pos) instanceof Scp1730TurretBlockEntity Turret) {
            Turret.damage(World, Damage, Hit.getLocation());
        }
    }

    @Override
    protected void onProjectileHit(Level World, BlockState State, BlockHitResult Hit, Projectile Shot) {
        if (World instanceof ServerLevel Server && Shot instanceof AbstractArrow && World.getBlockEntity(Hit.getBlockPos()) instanceof Scp1730TurretBlockEntity Turret) {
            Turret.damage(Server, Shot instanceof ThrownTrident ? TridentDamage : ArrowDamage, Hit.getLocation());
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp1730TurretBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, Scp1730Registry.TurretEntity.get(), World.isClientSide ? Scp1730TurretBlockEntity::clientTick : Scp1730TurretBlockEntity::serverTick);
    }
}
