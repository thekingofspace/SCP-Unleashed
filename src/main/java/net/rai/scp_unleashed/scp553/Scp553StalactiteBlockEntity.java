package net.rai.scp_unleashed.scp553;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Scp553StalactiteBlockEntity extends BlockEntity {
    public static final int StageTicks = 600;
    public static final int HatchTicks = StageTicks * (Scp553StalactiteBlock.MaxStage + 1);
    public static final int CheckInterval = 20;
    public static final int HoldInterval = 200;
    private int Growth;
    private int Wait;
    private boolean Stalled;

    public Scp553StalactiteBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp553Registry.StalactiteEntity.get(), Pos, State);
    }

    public int growth() {
        return Growth;
    }

    public boolean stalled() {
        return Stalled;
    }

    public static int stageFor(int Growth) {
        return Math.min(Scp553StalactiteBlock.MaxStage, Math.max(0, Growth / StageTicks));
    }

    public void setGrowth(int Value) {
        Growth = Math.max(0, Math.min(HatchTicks, Value));
        setChanged();
        if (level instanceof ServerLevel World) {
            restage(World);
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp553StalactiteBlockEntity Stalactite) {
        if (World instanceof ServerLevel Server) {
            Stalactite.tick(Server);
        }
    }

    private void tick(ServerLevel World) {
        if (--Wait > 0) {
            return;
        }
        Wait = CheckInterval;
        boolean Dark = Scp553Colony.dark(World, worldPosition);
        if (Stalled == Dark) {
            Stalled = !Dark;
            setChanged();
        }
        if (Stalled) {
            return;
        }
        if (Growth < HatchTicks) {
            Growth = Math.min(HatchTicks, Growth + CheckInterval);
            setChanged();
            restage(World);
            return;
        }
        if (!hatch(World)) {
            Wait = HoldInterval;
        }
    }

    private void restage(ServerLevel World) {
        BlockState State = getBlockState();
        if (!State.is(Scp553Registry.Stalactite.get())) {
            return;
        }
        int Wanted = stageFor(Growth);
        if (State.getValue(Scp553StalactiteBlock.Stage) == Wanted) {
            return;
        }
        World.setBlock(worldPosition, State.setValue(Scp553StalactiteBlock.Stage, Wanted), Block.UPDATE_ALL);
        Vec3 Tip = tip();
        World.sendParticles(Scp553Registry.Glint.get(), Tip.x, Tip.y, Tip.z, 4 + Wanted * 2, 0.12, 0.15, 0.12, 0.01);
        Scp553Colony.play(World, Tip, Scp553Registry.GrowLayers, 0.9f + Wanted * 0.08f, 0.7f);
    }

    public Vec3 tip() {
        double Bottom = Scp553StalactiteBlock.shapeFor(getBlockState().getValue(Scp553StalactiteBlock.Stage)).min(net.minecraft.core.Direction.Axis.Y);
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + Bottom, worldPosition.getZ() + 0.5);
    }

    public boolean ripe() {
        return Growth >= HatchTicks;
    }

    @Nullable
    public Scp553Butterfly forceHatch(ServerLevel World) {
        Growth = HatchTicks;
        return release(World);
    }

    public boolean hatch(ServerLevel World) {
        if (!Scp553Colony.roomToHatch(World, worldPosition)) {
            return false;
        }
        return release(World) != null;
    }

    @Nullable
    private Scp553Butterfly release(ServerLevel World) {
        Scp553Butterfly Born = Scp553Registry.Butterfly.get().create(World);
        if (Born == null) {
            return null;
        }
        BlockState State = getBlockState();
        BlockPos Pos = worldPosition;
        Vec3 Spot = new Vec3(Pos.getX() + 0.5, Pos.getY() + 0.3, Pos.getZ() + 0.5);
        float Yaw = World.getRandom().nextFloat() * 360.0f;
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Born.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Born.setYBodyRot(Yaw);
        Born.setYHeadRot(Yaw);
        Born.finalizeSpawn(World, World.getCurrentDifficultyAt(Pos), MobSpawnType.BREEDING, null);
        Born.newborn(Pos);
        Born.setDeltaMovement(0.0, -0.05, 0.0);
        if (!World.addFreshEntity(Born)) {
            return null;
        }
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Spot.x, Spot.y + 0.3, Spot.z, 24, 0.2, 0.3, 0.2, 0.1);
        World.sendParticles(Scp553Registry.Glint.get(), Spot.x, Spot.y + 0.2, Spot.z, 16, 0.25, 0.25, 0.25, 0.04);
        Scp553Colony.play(World, Spot, Scp553Registry.HatchLayers, 1.0f, 1.0f);
        return Born;
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.putInt("Growth", Growth);
        Saved.putBoolean("Stalled", Stalled);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Growth = Saved.getInt("Growth");
        Stalled = Saved.getBoolean("Stalled");
    }
}
