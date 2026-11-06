package net.rai.scp_unleashed.scp015;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class Scp015Colony {
    static final int MaxFronts = 4;
    static final int StartDelay = 100;
    static final int StartJitter = 60;
    static final int GrowInterval = 60;
    static final int GrowJitter = 40;
    static final int SpawnInterval = 40;
    static final int BreakInterval = 80;
    static final int RegrowDelay = 600;
    static final int DormantInterval = 200;
    static final int DormantBatch = 64;
    static final int QueueLimit = 32768;
    static final int Margin = 8;
    static final float BranchChance = 0.16f;
    static final float CreakChance = 0.5f;
    static final float GroanChance = 0.08f;

    final int Id;
    private final List<Scp015Front> Fronts = new ArrayList<>();
    private final LongLinkedOpenHashSet Pending = new LongLinkedOpenHashSet();
    private final LongLinkedOpenHashSet Boundary = new LongLinkedOpenHashSet();
    private final LongLinkedOpenHashSet Dormant = new LongLinkedOpenHashSet();
    private final Long2LongLinkedOpenHashMap Waking = new Long2LongLinkedOpenHashMap();
    private long NextSpawn;
    private long NextBreak;
    private long NextDormant;
    private int MinX;
    private int MinY;
    private int MinZ;
    private int MaxX;
    private int MaxY;
    private int MaxZ;

    Scp015Colony(int Id, BlockPos Origin) {
        this.Id = Id;
        MinX = MaxX = Origin.getX();
        MinY = MaxY = Origin.getY();
        MinZ = MaxZ = Origin.getZ();
    }

    static boolean growable(ServerLevel World, BlockPos Pos) {
        return !World.isOutsideBuildHeight(Pos) && World.getWorldBorder().isWithinBounds(Pos) && World.shouldTickBlocksAt(Pos);
    }

    static boolean occupied(ServerLevel World, BlockPos Pos) {
        return !World.getEntitiesOfClass(LivingEntity.class, new AABB(Pos), Living -> Living.isAlive() && !Living.isSpectator()).isEmpty();
    }

    static boolean breakable(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.isAir() || Scp015PipeBlock.isPipe(State) || Scp015Growth.isFree(State) || State.is(Scp015Registry.Resistant)) {
            return false;
        }
        return State.getDestroySpeed(World, Pos) >= 0.0f;
    }

    boolean contains(BlockPos Pos) {
        return Pos.getX() >= MinX - Margin && Pos.getX() <= MaxX + Margin && Pos.getY() >= MinY - Margin && Pos.getY() <= MaxY + Margin
                && Pos.getZ() >= MinZ - Margin && Pos.getZ() <= MaxZ + Margin;
    }

    double distanceTo(BlockPos Pos) {
        double X = Math.max(0, Math.max(MinX - Pos.getX(), Pos.getX() - MaxX));
        double Y = Math.max(0, Math.max(MinY - Pos.getY(), Pos.getY() - MaxY));
        double Z = Math.max(0, Math.max(MinZ - Pos.getZ(), Pos.getZ() - MaxZ));
        return X * X + Y * Y + Z * Z;
    }

    private void include(BlockPos Pos) {
        MinX = Math.min(MinX, Pos.getX());
        MinY = Math.min(MinY, Pos.getY());
        MinZ = Math.min(MinZ, Pos.getZ());
        MaxX = Math.max(MaxX, Pos.getX());
        MaxY = Math.max(MaxY, Pos.getY());
        MaxZ = Math.max(MaxZ, Pos.getZ());
    }

    boolean isIdle() {
        return Fronts.isEmpty() && Pending.isEmpty() && Boundary.isEmpty() && Dormant.isEmpty() && Waking.isEmpty();
    }

    int frontCount() {
        return Fronts.size();
    }

    void seed(BlockPos Pos, long StartAt, RandomSource Random) {
        include(Pos);
        if (Fronts.size() < MaxFronts) {
            Fronts.add(new Scp015Front(Pos.asLong(), null, Scp015Front.rollRun(Random), StartAt));
        } else {
            queue(Pending, Pos.asLong());
        }
    }

    void wake(BlockPos Pos, long WakeAt) {
        include(Pos);
        Waking.putIfAbsent(Pos.asLong(), WakeAt);
        while (Waking.size() > QueueLimit) {
            Waking.removeFirstLong();
        }
    }

    private static void queue(LongLinkedOpenHashSet Queue, long Pos) {
        Queue.addAndMoveToLast(Pos);
        while (Queue.size() > QueueLimit) {
            Queue.removeFirstLong();
        }
    }

    boolean tick(ServerLevel World, Scp015Growth Growth, int Limit, Scp015Growth.Budget Spend) {
        long Now = World.getGameTime();
        boolean Changed = false;
        while (!Waking.isEmpty() && Waking.get(Waking.firstLongKey()) <= Now) {
            queue(Pending, Waking.firstLongKey());
            Waking.removeFirstLong();
            Changed = true;
        }
        if (!Dormant.isEmpty() && Now >= NextDormant) {
            NextDormant = Now + DormantInterval;
            int Batch = Math.min(DormantBatch, Dormant.size());
            for (int Index = 0; Index < Batch; Index++) {
                long Pos = Dormant.removeFirstLong();
                if (World.shouldTickBlocksAt(BlockPos.of(Pos))) {
                    queue(Pending, Pos);
                } else {
                    Dormant.add(Pos);
                }
            }
            Changed = true;
        }
        Changed |= growFronts(World, Growth, Limit, Spend, Now);
        if (Fronts.size() < MaxFronts && Now >= NextSpawn) {
            while (!Pending.isEmpty() && Spend.Checks > 0) {
                Spend.Checks--;
                Changed = true;
                if (examine(World, Pending.removeLastLong(), Now)) {
                    NextSpawn = Now + SpawnInterval;
                    break;
                }
            }
        }
        if (Fronts.isEmpty() && Pending.isEmpty() && Waking.isEmpty() && !Boundary.isEmpty() && Now >= NextBreak && Spend.Growth > 0
                && Growth.getSegments() < Limit) {
            while (!Boundary.isEmpty() && Spend.Checks > 0) {
                Spend.Checks--;
                Changed = true;
                long Pos = Boundary.removeLastLong();
                int Outcome = breakFrom(World, Pos, Now);
                if (Outcome > 0) {
                    Spend.Growth--;
                    NextBreak = Now + BreakInterval;
                    break;
                }
                if (Outcome == 0) {
                    break;
                }
            }
        }
        return Changed;
    }

    private boolean growFronts(ServerLevel World, Scp015Growth Growth, int Limit, Scp015Growth.Budget Spend, long Now) {
        boolean Changed = false;
        List<Scp015Front> Branches = new ArrayList<>();
        Iterator<Scp015Front> Cursor = Fronts.iterator();
        while (Cursor.hasNext()) {
            Scp015Front Front = Cursor.next();
            if (Now < Front.NextTick) {
                continue;
            }
            if (Spend.Growth <= 0) {
                break;
            }
            Front.NextTick = Now + GrowInterval + World.random.nextInt(GrowJitter);
            Changed = true;
            BlockPos At = BlockPos.of(Front.Pos);
            if (!World.shouldTickBlocksAt(At)) {
                Cursor.remove();
                queue(Dormant, Front.Pos);
                continue;
            }
            if (!Scp015PipeBlock.isPipe(World.getBlockState(At))) {
                Cursor.remove();
                continue;
            }
            if (Growth.getSegments() >= Limit) {
                continue;
            }
            Direction Side = Front.choose(World, At);
            if (Side == null) {
                Cursor.remove();
                queue(Pending, Front.Pos);
                continue;
            }
            BlockPos Target = At.relative(Side);
            Spend.Growth--;
            if (!extend(World, At, Target, Side)) {
                continue;
            }
            queue(Pending, Front.Pos);
            Front.advance(Target, Side, World.random);
            if (Fronts.size() + Branches.size() < MaxFronts && World.random.nextFloat() < BranchChance) {
                Branches.add(new Scp015Front(At.asLong(), null, Scp015Front.rollRun(World.random), Now + GrowInterval / 2 + World.random.nextInt(GrowJitter)));
            }
        }
        Fronts.addAll(Branches);
        return Changed;
    }

    private boolean extend(ServerLevel World, BlockPos From, BlockPos Target, Direction Side) {
        BlockState Existing = World.getBlockState(Target);
        if (!Existing.isAir() && !(Existing.getBlock() instanceof LiquidBlock)) {
            World.destroyBlock(Target, false);
        }
        BlockState Grown = Scp015Registry.Pipe.get().grownState(Side.getOpposite());
        if (!World.setBlock(Target, Grown, Block.UPDATE_ALL)) {
            return false;
        }
        BlockState Parent = World.getBlockState(From);
        if (Scp015PipeBlock.isPipe(Parent)) {
            World.setBlock(From, Parent.setValue(Scp015PipeBlock.connection(Side), true), Block.UPDATE_ALL);
        }
        include(Target);
        RandomSource Random = World.random;
        float Pitch = 0.85f + Random.nextFloat() * 0.3f;
        World.playSound(null, Target, Scp015Registry.Clank.get(), SoundSource.BLOCKS, 0.55f, Pitch);
        if (Random.nextFloat() < CreakChance) {
            World.playSound(null, Target, Scp015Registry.Creak.get(), SoundSource.BLOCKS, 0.6f, Pitch);
        }
        if (Random.nextFloat() < GroanChance) {
            World.playSound(null, Target, Scp015Registry.Groan.get(), SoundSource.BLOCKS, 0.8f, 0.85f + Random.nextFloat() * 0.2f);
            World.playSound(null, Target, Scp015Registry.GroanCreak.get(), SoundSource.BLOCKS, 0.55f, 0.85f + Random.nextFloat() * 0.2f);
        }
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Grown), Target.getX() + 0.5 - Side.getStepX() * 0.45, Target.getY() + 0.5 - Side.getStepY() * 0.45,
                Target.getZ() + 0.5 - Side.getStepZ() * 0.45, 5, 0.15, 0.15, 0.15, 0.02);
        return true;
    }

    private boolean examine(ServerLevel World, long Pos, long Now) {
        BlockPos At = BlockPos.of(Pos);
        if (!World.shouldTickBlocksAt(At)) {
            queue(Dormant, Pos);
            return false;
        }
        if (!Scp015PipeBlock.isPipe(World.getBlockState(At))) {
            return false;
        }
        boolean Free = false;
        boolean Breakable = false;
        boolean Waiting = false;
        for (Direction Side : Direction.values()) {
            BlockPos Target = At.relative(Side);
            if (World.isOutsideBuildHeight(Target) || !World.getWorldBorder().isWithinBounds(Target)) {
                continue;
            }
            if (!World.shouldTickBlocksAt(Target)) {
                Waiting = true;
                continue;
            }
            BlockState TargetState = World.getBlockState(Target);
            if (Scp015PipeBlock.isPipe(TargetState)) {
                continue;
            }
            if (Scp015Growth.isFree(TargetState)) {
                if (occupied(World, Target)) {
                    Waiting = true;
                } else {
                    Free = true;
                }
            } else if (breakable(World, Target, TargetState)) {
                Breakable = true;
            }
        }
        if (Waiting) {
            queue(Dormant, Pos);
        }
        if (Breakable) {
            queue(Boundary, Pos);
        }
        if (Free) {
            Fronts.add(new Scp015Front(Pos, null, Scp015Front.rollRun(World.random), Now + 10 + World.random.nextInt(GrowJitter)));
            return true;
        }
        return false;
    }

    private int breakFrom(ServerLevel World, long Pos, long Now) {
        BlockPos At = BlockPos.of(Pos);
        if (!World.shouldTickBlocksAt(At)) {
            queue(Dormant, Pos);
            return -1;
        }
        BlockState Here = World.getBlockState(At);
        if (!Scp015PipeBlock.isPipe(Here)) {
            return -1;
        }
        RandomSource Random = World.random;
        Direction Best = null;
        double BestScore = Double.NEGATIVE_INFINITY;
        for (Direction Side : Direction.values()) {
            BlockPos Target = At.relative(Side);
            if (!growable(World, Target)) {
                continue;
            }
            BlockState TargetState = World.getBlockState(Target);
            if (Scp015Growth.isFree(TargetState) && !occupied(World, Target)) {
                queue(Pending, Pos);
                return 0;
            }
            if (!breakable(World, Target, TargetState) || occupied(World, Target)) {
                continue;
            }
            double Score = Random.nextDouble() * 0.6;
            if (Here.getValue(Scp015PipeBlock.connection(Side.getOpposite()))) {
                Score += 2.5;
            }
            Score += Side.getAxis() != Direction.Axis.Y ? 1.0 : (Side == Direction.UP ? 0.4 : 0.0);
            Score -= Math.min(TargetState.getDestroySpeed(World, Target), 30.0f) * 0.1;
            if (Score > BestScore) {
                BestScore = Score;
                Best = Side;
            }
        }
        if (Best == null) {
            return -1;
        }
        BlockPos Target = At.relative(Best);
        World.playSound(null, Target, Scp015Registry.Strain.get(), SoundSource.BLOCKS, 0.9f, 0.85f + Random.nextFloat() * 0.2f);
        World.playSound(null, Target, Scp015Registry.StrainClang.get(), SoundSource.BLOCKS, 0.6f, 0.85f + Random.nextFloat() * 0.2f);
        World.destroyBlock(Target, true);
        queue(Boundary, Pos);
        BlockState After = World.getBlockState(Target);
        if (Scp015Growth.isFree(After) && extend(World, At, Target, Best)) {
            Scp015Front Front = new Scp015Front(Target.asLong(), Best, Scp015Front.rollRun(Random), Now + 10 + Random.nextInt(GrowJitter));
            Front.Run = 1;
            Fronts.add(Front);
        }
        return 1;
    }

    CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putInt("Id", Id);
        Saved.putIntArray("Box", new int[]{MinX, MinY, MinZ, MaxX, MaxY, MaxZ});
        Saved.putLong("NextSpawn", NextSpawn);
        Saved.putLong("NextBreak", NextBreak);
        Saved.putLong("NextDormant", NextDormant);
        ListTag FrontList = new ListTag();
        for (Scp015Front Front : Fronts) {
            FrontList.add(Front.save());
        }
        Saved.put("Fronts", FrontList);
        Saved.putLongArray("Pending", Pending.toLongArray());
        Saved.putLongArray("Boundary", Boundary.toLongArray());
        Saved.putLongArray("Dormant", Dormant.toLongArray());
        Saved.putLongArray("WakingPos", Waking.keySet().toLongArray());
        Saved.putLongArray("WakingAt", Waking.values().toLongArray());
        return Saved;
    }

    static Scp015Colony load(CompoundTag Saved) {
        int[] Box = Saved.getIntArray("Box");
        Scp015Colony Colony = new Scp015Colony(Saved.getInt("Id"), BlockPos.ZERO);
        if (Box.length == 6) {
            Colony.MinX = Box[0];
            Colony.MinY = Box[1];
            Colony.MinZ = Box[2];
            Colony.MaxX = Box[3];
            Colony.MaxY = Box[4];
            Colony.MaxZ = Box[5];
        }
        Colony.NextSpawn = Saved.getLong("NextSpawn");
        Colony.NextBreak = Saved.getLong("NextBreak");
        Colony.NextDormant = Saved.getLong("NextDormant");
        ListTag FrontList = Saved.getList("Fronts", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < FrontList.size() && Colony.Fronts.size() < MaxFronts; Index++) {
            Colony.Fronts.add(Scp015Front.load(FrontList.getCompound(Index)));
        }
        for (long Pos : Saved.getLongArray("Pending")) {
            Colony.Pending.add(Pos);
        }
        for (long Pos : Saved.getLongArray("Boundary")) {
            Colony.Boundary.add(Pos);
        }
        for (long Pos : Saved.getLongArray("Dormant")) {
            Colony.Dormant.add(Pos);
        }
        long[] WakingPos = Saved.getLongArray("WakingPos");
        long[] WakingAt = Saved.getLongArray("WakingAt");
        for (int Index = 0; Index < Math.min(WakingPos.length, WakingAt.length); Index++) {
            Colony.Waking.put(WakingPos[Index], WakingAt[Index]);
        }
        return Colony;
    }
}
