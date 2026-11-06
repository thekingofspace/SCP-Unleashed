package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.block.Scp1730CrackBlock;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Leech;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Tentacle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp1730Cracks {
    public static final int CheckEvery = 40;
    public static final double LeechReach = 14.0;
    public static final double TentacleReach = 9.0;
    public static final int LeechesPerCrack = 2;
    public static final int LeechesPerPlayer = 6;
    public static final int LeechesPerLevel = 40;
    public static final float LeechChance = 0.15f;
    public static final float[] TentacleChance = {0.05f, 0.08f, 0.12f, 0.16f};
    public static final int TentacleRest = 400;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> Index = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TentacleClock = new ConcurrentHashMap<>();

    private Scp1730Cracks() {
    }

    public static void indexed(Level World, BlockPos Pos, boolean Present) {
        if (World.isClientSide) {
            return;
        }
        Set<BlockPos> Found = Index.computeIfAbsent(World.dimension(), Key -> ConcurrentHashMap.newKeySet());
        if (Present) {
            Found.add(Pos.immutable());
        } else {
            Found.remove(Pos);
        }
    }

    public static void clear() {
        Index.clear();
        TentacleClock.clear();
    }

    public static List<BlockPos> near(Level World, Vec3 Point, double Reach) {
        List<BlockPos> Found = new ArrayList<>();
        Set<BlockPos> All = Index.get(World.dimension());
        if (All == null) {
            return Found;
        }
        double Limit = Reach * Reach;
        for (BlockPos Pos : All) {
            if (Pos.distToCenterSqr(Point) <= Limit && World.isLoaded(Pos) && World.getBlockState(Pos).getBlock() instanceof Scp1730CrackBlock) {
                Found.add(Pos);
            }
        }
        return Found;
    }

    public static Direction faceOf(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getBlock() instanceof Scp1730CrackBlock ? State.getValue(Scp1730CrackBlock.Facing) : Direction.NORTH;
    }

    public static boolean suppressed(Level World, BlockPos Pos) {
        return Scp1730Anchors.anchored(World, Pos);
    }

    public static void tick(ServerLevel World, ServerPlayer User, int Zone) {
        if ((World.getGameTime() + User.getId()) % CheckEvery != 0 || User.isCreative() || User.isSpectator() || !User.isAlive()) {
            return;
        }
        List<BlockPos> Cracks = near(World, User.position(), LeechReach);
        if (Cracks.isEmpty()) {
            return;
        }
        int Total = World.getEntitiesOfClass(Scp1730Leech.class, User.getBoundingBox().inflate(24.0)).size();
        for (BlockPos Pos : Cracks) {
            if (suppressed(World, Pos)) {
                continue;
            }
            if (Total < LeechesPerPlayer && World.random.nextFloat() < LeechChance && spawnLeech(World, Pos)) {
                Total++;
            }
        }
        long Now = World.getGameTime();
        if (Now < TentacleClock.getOrDefault(User.getUUID(), 0L)) {
            return;
        }
        float Chance = TentacleChance[Math.max(0, Math.min(TentacleChance.length - 1, Zone))];
        for (BlockPos Pos : Cracks) {
            if (suppressed(World, Pos) || World.random.nextFloat() >= Chance) {
                continue;
            }
            Direction Side = faceOf(World, Pos);
            Vec3 Mouth = Scp1730CrackBlock.mouth(Pos, Side);
            if (Mouth.distanceTo(User.getEyePosition()) > TentacleReach || !World.getEntitiesOfClass(Scp1730Tentacle.class, new AABB(Pos).inflate(2.0)).isEmpty()) {
                continue;
            }
            Vec3 Way = User.getEyePosition().subtract(Mouth);
            if (Way.normalize().dot(new Vec3(Side.getStepX(), 0.0, Side.getStepZ())) < 0.2) {
                continue;
            }
            if (!Scp1730Symbols.clearSight(World, User, User.getEyePosition(), Mouth, Pos, null)) {
                continue;
            }
            Scp1730Tentacle.emerge(World, Pos, Side, User);
            TentacleClock.put(User.getUUID(), Now + TentacleRest + World.random.nextInt(TentacleRest));
            return;
        }
    }

    public static boolean spawnLeech(ServerLevel World, BlockPos Pos) {
        Direction Side = faceOf(World, Pos);
        BlockPos Out = Pos.relative(Side);
        if (World.getEntitiesOfClass(Scp1730Leech.class, new AABB(Out).inflate(3.0)).size() >= LeechesPerCrack) {
            return false;
        }
        if (!World.getBlockState(Out).getCollisionShape(World, Out).isEmpty()) {
            return false;
        }
        if (World.getEntitiesOfClass(Scp1730Leech.class, new AABB(Out).inflate(256.0, 64.0, 256.0)).size() >= LeechesPerLevel) {
            return false;
        }
        Scp1730Leech Leech = Scp1730Registry.LeechType.get().create(World);
        if (Leech == null) {
            return false;
        }
        Vec3 Mouth = Scp1730CrackBlock.mouth(Pos, Side);
        double Floor = Out.getY();
        Leech.moveTo(Mouth.x, Math.max(Floor, Mouth.y - 0.3), Mouth.z, Side.toYRot(), 0.0f);
        Leech.finalizeSpawn(World, World.getCurrentDifficultyAt(Out), MobSpawnType.EVENT, null);
        Leech.setDeltaMovement(Side.getStepX() * 0.25, 0.1, Side.getStepZ() * 0.25);
        Leech.setHome(Pos);
        World.addFreshEntity(Leech);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1730Registry.LeechEmerge.get(), SoundSource.HOSTILE, 0.9f, 0.9f + World.random.nextFloat() * 0.3f);
        return true;
    }
}
