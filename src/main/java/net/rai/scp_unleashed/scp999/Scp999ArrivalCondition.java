package net.rai.scp_unleashed.scp999;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Scp999ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp999_arrival";
    public static final String ReadyKey = "Ready";
    public static final int Attempts = 36;
    public static final int Vertical = 6;
    public static final int SeenGrace = 1200;
    public static final int ReachGrace = 2400;
    public static final double ViewCone = 0.45;
    public static final List<String> NoteKeys = List.of("archive.scp_unleashed.notes.scp999.0", "archive.scp_unleashed.notes.scp999.1");
    public static final MapCodec<Scp999ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp999ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 150).forGetter(Scp999ArrivalCondition::MaxDelay),
            Codec.intRange(6, 64).optionalFieldOf("min_distance", 12).forGetter(Scp999ArrivalCondition::MinDistance),
            Codec.intRange(6, 64).optionalFieldOf("max_distance", 24).forGetter(Scp999ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp999ArrivalCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long rollDelay(RandomSource Random) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return (Low + (long) Random.nextInt(High - Low + 1)) * 20L;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong(ReadyKey, Reader.serverLevel().getGameTime() + rollDelay(Reader.getRandom()));
        Case.markDirty();
        return NoteKeys.stream().<Component>map(Component::translatable).toList();
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (Reader.isSpectator() || Reader.isSleeping()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        long Now = World.getGameTime();
        if (!State.contains(ReadyKey)) {
            State.putLong(ReadyKey, Now + rollDelay(Reader.getRandom()));
            Case.markDirty();
            return Optional.empty();
        }
        long Ready = State.getLong(ReadyKey);
        if (Now < Ready) {
            return Optional.empty();
        }
        long Waited = Now - Ready;
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        return findSpot(World, Reader, Low, High, Waited < SeenGrace, Waited < ReachGrace, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static Optional<BlockPos> findSpot(ServerLevel World, ServerPlayer Reader, int Low, int High, boolean Hidden, boolean Reachable, RandomSource Random) {
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = Low + Random.nextDouble() * (High - Low);
            BlockPos Column = BlockPos.containing(Reader.getX() + Math.cos(Angle) * Radius, Reader.getY(), Reader.getZ() + Math.sin(Angle) * Radius);
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Spot = Column.above(Dy);
                if (!suitable(World, Spot)) {
                    continue;
                }
                if (Hidden && seen(World, Reader, Spot)) {
                    break;
                }
                if (Reachable && !reaches(World, Spot, Reader)) {
                    break;
                }
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static boolean suitable(ServerLevel World, BlockPos Spot) {
        if (!LeadPlanner.isDryStandable(World, Spot)) {
            return false;
        }
        AABB Room = new AABB(Spot).inflate(0.1, 0.0, 0.1);
        return World.noCollision(Room.move(0.0, 0.05, 0.0)) && !World.containsAnyLiquid(Room.inflate(1.0, 0.5, 1.0));
    }

    public static boolean seen(ServerLevel World, ServerPlayer Reader, BlockPos Spot) {
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Target = Vec3.atBottomCenterOf(Spot).add(0.0, Scp999Blob.EyeHeight, 0.0);
        Vec3 Toward = Target.subtract(Eye).normalize();
        if (Toward.dot(Reader.getViewVector(1.0f)) < ViewCone) {
            return false;
        }
        HitResult Hit = World.clip(new ClipContext(Eye, Target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader));
        return Hit.getType() == HitResult.Type.MISS;
    }

    public static boolean reaches(ServerLevel World, BlockPos Spot, ServerPlayer Reader) {
        Scp999Blob Probe = Scp999Registry.Blob.get().create(World);
        if (Probe == null) {
            return false;
        }
        Probe.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, 0.0f, 0.0f);
        Path Route = Probe.getNavigation().createPath(Reader, 2);
        Probe.discard();
        return Route != null && Route.canReach();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerPlayer Reader = Found.Investigator();
        return place(Found.World(), Vec3.atBottomCenterOf(Found.Pos()), Reader.getUUID(), Reader.position()).isPresent();
    }

    public static Optional<Scp999Blob> place(ServerLevel World, Vec3 Spot, UUID Sought, Vec3 Toward) {
        Scp999Blob Blob = Scp999Registry.Blob.get().create(World);
        if (Blob == null) {
            return Optional.empty();
        }
        float Yaw = (float) (Mth.atan2(Toward.z - Spot.z, Toward.x - Spot.x) * Mth.RAD_TO_DEG) - 90.0f;
        Blob.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Blob.setYHeadRot(Yaw);
        Blob.setYBodyRot(Yaw);
        if (!World.noCollision(Blob)) {
            return Optional.empty();
        }
        Blob.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Blob.seek(Sought);
        if (!World.addFreshEntity(Blob)) {
            return Optional.empty();
        }
        return Optional.of(Blob);
    }
}
