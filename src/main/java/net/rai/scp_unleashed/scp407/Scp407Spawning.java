package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class Scp407Spawning {
    public static final long SproutDelay = 900L;
    public static final long SproutInterval = 160L;
    public static final int SproutCap = 8;
    public static final long TitanTick = 2976L;
    public static final long WandererDelay = 100L;
    public static final long RetryTicks = 40L;
    public static final int TitanHeadroom = 3;
    public static final int SproutReach = 4;
    public static final int WandererMin = 3;
    public static final int WandererMax = 10;
    public static final int FloorDrop = Scp407Enclosure.MaxHeight;
    public static final int BloomTries = 3;

    private Scp407Spawning() {
    }

    public static boolean open(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getFluidState().isEmpty() && State.getCollisionShape(World, Pos).isEmpty();
    }

    public static boolean standable(ServerLevel World, BlockPos Pos, int Headroom) {
        if (!World.isLoaded(Pos) || !Scp407Growth.sturdy(World, Pos, Direction.DOWN)) {
            return false;
        }
        for (int Rise = 0; Rise < Headroom; Rise++) {
            if (!open(World, Pos.above(Rise))) {
                return false;
            }
        }
        return true;
    }

    public static int alive(ServerLevel World, Scp407Session Session, Class<? extends Entity> Kind) {
        int Count = 0;
        Iterator<UUID> Ids = Session.Creatures.iterator();
        while (Ids.hasNext()) {
            Entity Found = World.getEntity(Ids.next());
            if (Found == null || !Found.isAlive()) {
                Ids.remove();
                continue;
            }
            if (Kind.isInstance(Found)) {
                Count++;
            }
        }
        return Count;
    }

    public static void sprouts(ServerLevel World, Scp407Session Session, long Position) {
        if (Position < Session.ActTick + SproutDelay || !World.isLoaded(Session.Origin)) {
            return;
        }
        long Now = World.getGameTime();
        if (Session.NextSprout < 0L) {
            Session.NextSprout = Now;
        }
        if (Now < Session.NextSprout) {
            return;
        }
        Session.NextSprout = Now + SproutInterval;
        if (alive(World, Session, Scp407Sprout.class) >= SproutCap) {
            return;
        }
        RandomSource Dice = Session.random();
        List<BlockPos> Blooms = Scp407Growth.livingBlooms(World, Session);
        for (int Attempt = 0; Attempt < BloomTries && !Blooms.isEmpty(); Attempt++) {
            BlockPos Bloom = Blooms.remove(Dice.nextInt(Blooms.size()));
            BlockPos Spot = floorNear(World, Session, Bloom, SproutReach, 1);
            if (Spot != null) {
                hatch(World, Session, Spot, Bloom);
                return;
            }
        }
    }

    public static LongOpenHashSet heldCells(Scp407Session Session) {
        LongOpenHashSet Held = new LongOpenHashSet();
        for (Scp407Session.Listener Entry : Session.Listeners.values()) {
            if (Entry.Anchor != null) {
                Held.add(Entry.Anchor.asLong());
                Held.add(Entry.Anchor.above().asLong());
            }
        }
        return Held;
    }

    public static boolean vacant(ServerLevel World, BlockPos Pos) {
        return World.getEntitiesOfClass(LivingEntity.class, new AABB(Pos), LivingEntity::isAlive).isEmpty();
    }

    @Nullable
    public static Scp407Sprout hatch(ServerLevel World, Scp407Session Session, BlockPos Spot, @Nullable BlockPos Bloom) {
        Scp407Sprout Sprout = Scp407Registry.Sprout.get().create(World);
        if (Sprout == null) {
            return null;
        }
        RandomSource Dice = Session.random();
        Sprout.getRandom().setSeed(Dice.nextLong());
        Sprout.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Dice.nextFloat() * 360.0f, 0.0f);
        Sprout.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.EVENT, null);
        Sprout.bindSession(Session.Id);
        Sprout.beginHatch();
        if (!World.addFreshEntity(Sprout)) {
            return null;
        }
        Session.Creatures.add(Sprout.getUUID());
        Session.Sprouts++;
        BlockPos Burst = Bloom != null ? Bloom : Spot;
        World.sendParticles(Scp407Registry.Spore.get(), Burst.getX() + 0.5, Burst.getY() + 0.5, Burst.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.02);
        World.sendParticles(Scp407Registry.Leaf.get(), Spot.getX() + 0.5, Spot.getY() + 0.4, Spot.getZ() + 0.5, 6, 0.25, 0.2, 0.25, 0.03);
        World.playSound(null, Spot, Scp407Registry.SporePuff.get(), SoundSource.NEUTRAL, 0.7f, 1.2f);
        return Sprout;
    }

    @Nullable
    public static BlockPos floorNear(ServerLevel World, Scp407Session Session, BlockPos Center, int Reach, int Headroom) {
        List<int[]> Columns = new ArrayList<>();
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                Columns.add(new int[]{Dx, Dz});
            }
        }
        Util.shuffle(Columns, Session.random());
        Columns.sort(Comparator.comparingInt(Column -> Column[0] * Column[0] + Column[1] * Column[1]));
        LongOpenHashSet Held = heldCells(Session);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int[] Column : Columns) {
            for (int Rise = Reach; Rise >= -FloorDrop; Rise--) {
                Cursor.set(Center.getX() + Column[0], Center.getY() + Rise, Center.getZ() + Column[1]);
                long Key = Cursor.asLong();
                if (Session.Region.contains(Key) && !Held.contains(Key) && standable(World, Cursor, Headroom) && vacant(World, Cursor)) {
                    return Cursor.immutable();
                }
            }
        }
        return null;
    }

    public static void titan(ServerLevel World, Scp407Session Session, long Position) {
        if (Session.TitanSpawned || !World.isLoaded(Session.Origin)) {
            return;
        }
        long Due = Session.isGenesis() ? TitanTick : Session.ActTick + WandererDelay;
        if (Position < Due || (World.getGameTime() + Session.Id) % RetryTicks != 0L) {
            return;
        }
        BlockPos Spot = Session.isGenesis() ? roomSpot(World, Session) : groundSpot(World, Session);
        if (Spot != null) {
            raise(World, Session, Spot);
        }
    }

    @Nullable
    public static Scp407Titan raise(ServerLevel World, Scp407Session Session, BlockPos Spot) {
        Scp407Titan Titan = Scp407Registry.Titan.get().create(World);
        if (Titan == null) {
            return null;
        }
        Titan.getRandom().setSeed(Session.random().nextLong());
        double Dx = Session.Origin.getX() + 0.5 - (Spot.getX() + 0.5);
        double Dz = Session.Origin.getZ() + 0.5 - (Spot.getZ() + 0.5);
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        Titan.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Yaw, 0.0f);
        Titan.setYHeadRot(Yaw);
        Titan.setYBodyRot(Yaw);
        Titan.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.EVENT, null);
        Titan.bindSession(Session.Id);
        Titan.setHome(Session.Origin, Session.isGenesis());
        Titan.beginEmerge();
        if (!World.addFreshEntity(Titan)) {
            return null;
        }
        Session.Titan = Titan.getUUID();
        Session.TitanSpawned = true;
        Session.Creatures.add(Titan.getUUID());
        return Titan;
    }

    @Nullable
    private static BlockPos roomSpot(ServerLevel World, Scp407Session Session) {
        RandomSource Random = Session.random();
        long[] Cells = Session.Region.toLongArray();
        if (Cells.length == 0) {
            return null;
        }
        BlockPos Best = null;
        int BestScore = Integer.MIN_VALUE;
        for (int Attempt = 0; Attempt < 120; Attempt++) {
            BlockPos Pos = BlockPos.of(Cells[Random.nextInt(Cells.length)]);
            if (!standable(World, Pos, TitanHeadroom)) {
                continue;
            }
            int Score = 0;
            LongIterator Near = Session.Grown.iterator();
            int Seen = 0;
            while (Near.hasNext() && Seen < 400) {
                Seen++;
                if (BlockPos.of(Near.nextLong()).distManhattan(Pos) <= 3) {
                    Score++;
                }
            }
            int Distance = Pos.distManhattan(Session.Origin);
            Score -= Math.abs(Distance - 6);
            if (Score > BestScore) {
                Best = Pos;
                BestScore = Score;
            }
        }
        return Best;
    }

    @Nullable
    private static BlockPos groundSpot(ServerLevel World, Scp407Session Session) {
        RandomSource Random = Session.random();
        for (int Attempt = 0; Attempt < 64; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            int Distance = WandererMin + Random.nextInt(WandererMax - WandererMin + 1);
            int X = Session.Origin.getX() + (int) Math.round(Math.cos(Angle) * Distance);
            int Z = Session.Origin.getZ() + (int) Math.round(Math.sin(Angle) * Distance);
            for (int Y = Session.Origin.getY() + 4; Y >= Session.Origin.getY() - 8; Y--) {
                BlockPos Pos = new BlockPos(X, Y, Z);
                if (standable(World, Pos, TitanHeadroom)) {
                    return Pos;
                }
            }
        }
        return null;
    }
}
