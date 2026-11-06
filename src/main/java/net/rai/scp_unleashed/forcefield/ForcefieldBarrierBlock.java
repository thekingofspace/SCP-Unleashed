package net.rai.scp_unleashed.forcefield;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.guns.GunStruckBlock;
import org.jetbrains.annotations.Nullable;

public class ForcefieldBarrierBlock extends Block implements GunStruckBlock {
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;
    public static final double Thickness = 2.0;
    public static final float GunScale = 1.0f;
    public static final float ArrowScale = 1.0f;
    public static final float LooseProjectile = 1.5f;
    private static final VoxelShape SheetX = Block.box(0.0, 0.0, 8.0 - Thickness / 2.0, 16.0, 16.0, 8.0 + Thickness / 2.0);
    private static final VoxelShape SheetZ = Block.box(8.0 - Thickness / 2.0, 0.0, 0.0, 8.0 + Thickness / 2.0, 16.0, 16.0);

    public ForcefieldBarrierBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Axis, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Axis);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Axis) == Direction.Axis.X ? SheetX : SheetZ;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter World, BlockPos Pos) {
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
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public PathType getBlockPathType(BlockState State, BlockGetter World, BlockPos Pos, @Nullable Mob Walker) {
        return PathType.BLOCKED;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState State, HitResult Target, LevelReader World, BlockPos Pos, Player User) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void attack(BlockState State, Level World, BlockPos Pos, Player User) {
        if (World instanceof ServerLevel Server && User instanceof ServerPlayer Miner) {
            ForcefieldMining.strike(Server, Miner, Pos);
        }
    }

    @Override
    protected void onProjectileHit(Level World, BlockState State, BlockHitResult Hit, Projectile Shot) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        double Speed = Shot.getDeltaMovement().length();
        float Amount = Shot instanceof AbstractArrow Arrow ? (float) (Arrow.getBaseDamage() * Speed) * ArrowScale : LooseProjectile + (float) Speed;
        strike(Server, Hit.getBlockPos(), Hit.getType() == HitResult.Type.MISS ? Vec3.atCenterOf(Hit.getBlockPos()) : Hit.getLocation(), Amount);
    }

    @Override
    public void onGunHit(ServerLevel World, BlockPos Pos, BlockState State, BlockHitResult Hit, Entity User, float Damage) {
        strike(World, Pos, Hit.getLocation(), Damage * GunScale);
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        ForcefieldEmitterBlockEntity Owner = owner(World, Pos);
        if (Owner == null || !Owner.getPhase().raised()) {
            World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void strike(ServerLevel World, BlockPos Pos, Vec3 Point, float Amount) {
        ForcefieldEmitterBlockEntity Owner = owner(World, Pos);
        if (Owner != null) {
            Owner.damage(World, Amount, Point);
        }
    }

    @Nullable
    public static ForcefieldEmitterBlockEntity owner(Level World, BlockPos Pos) {
        BlockState Cell = World.getBlockState(Pos);
        if (!(Cell.getBlock() instanceof ForcefieldBarrierBlock)) {
            return null;
        }
        for (int Step = 1; Step <= ForcefieldEmitterBlockEntity.MaxGap; Step++) {
            BlockPos Below = Pos.below(Step);
            BlockState State = World.getBlockState(Below);
            if (State.getBlock() instanceof ForcefieldBarrierBlock) {
                continue;
            }
            if (ForcefieldEmitterBlock.isBottom(State) && State.getValue(ForcefieldEmitterBlock.Axis) == Cell.getValue(Axis)
                    && World.getBlockEntity(Below) instanceof ForcefieldEmitterBlockEntity Emitter) {
                return Emitter;
            }
            return null;
        }
        return null;
    }
}
