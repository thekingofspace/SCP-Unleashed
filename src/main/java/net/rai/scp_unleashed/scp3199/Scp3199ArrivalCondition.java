package net.rai.scp_unleashed.scp3199;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Scp3199ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp3199_arrival";
    public static final String ReadyKey = "Ready";
    public static final String CuedKey = "Cued";
    public static final int CueLead = 100;
    public static final int Attempts = 48;
    public static final int BehindAttempts = 32;
    public static final double BehindArc = 75.0;
    public static final int Vertical = 4;
    public static final double CueReach = 6.0;
    public static final MapCodec<Scp3199ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 30).forGetter(Scp3199ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 90).forGetter(Scp3199ArrivalCondition::MaxDelay),
            Codec.intRange(3, 48).optionalFieldOf("min_distance", 6).forGetter(Scp3199ArrivalCondition::MinDistance),
            Codec.intRange(3, 48).optionalFieldOf("max_distance", 14).forGetter(Scp3199ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp3199ArrivalCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long rollDelay(RandomSource Random) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return (Low + (long) Random.nextInt(High - Low + 1)) * 20L;
    }

    private void schedule(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong(ReadyKey, Reader.serverLevel().getGameTime() + rollDelay(Reader.getRandom()));
        Case.getState().remove(CuedKey);
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        schedule(Reader, Case);
        return List.of(Component.translatable("archive.scp_unleashed.notes.scp3199.0"), Component.translatable("archive.scp_unleashed.notes.scp3199.1"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        if (!State.contains(ReadyKey)) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        ServerPlayer Victim = victim(Reader);
        if (Victim == null) {
            return Optional.empty();
        }
        ServerLevel World = Victim.serverLevel();
        long Now = Reader.serverLevel().getGameTime();
        long Ready = State.getLong(ReadyKey);
        if (Now < Ready - CueLead) {
            return Optional.empty();
        }
        if (!State.getBoolean(CuedKey)) {
            State.putBoolean(CuedKey, true);
            Case.markDirty();
            cue(Victim);
        }
        if (Now < Ready) {
            return Optional.empty();
        }
        return hiddenSpot(World, Victim, MinDistance, MaxDistance, Victim.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static boolean valid(@Nullable ServerPlayer Player) {
        return Player != null && Player.isAlive() && !Player.isRemoved() && !Player.isSpectator() && !Player.isCreative() && AnomalyRealm.allows(Player.serverLevel());
    }

    @Nullable
    public static ServerPlayer victim(ServerPlayer Reader) {
        if (valid(Reader)) {
            return Reader;
        }
        if (!AnomalyRealm.allows(Reader.serverLevel())) {
            return null;
        }
        return nearest(Reader.serverLevel(), Reader.position());
    }

    @Nullable
    private static ServerPlayer nearest(ServerLevel World, Vec3 From) {
        ServerPlayer Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (ServerPlayer Candidate : World.players()) {
            if (!valid(Candidate)) {
                continue;
            }
            double Distance = Candidate.position().distanceToSqr(From);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    private static void cue(ServerPlayer Victim) {
        float Yaw = Victim.getYRot() + 180.0f + (Victim.getRandom().nextFloat() * 2.0f - 1.0f) * 50.0f;
        Vec3 Behind = Victim.getEyePosition().add(-Mth.sin(Yaw * Mth.DEG_TO_RAD) * CueReach, 0.0, Mth.cos(Yaw * Mth.DEG_TO_RAD) * CueReach);
        LeadPlanner.playTo(Victim, Scp3199Registry.ArriveFar, Behind, 0.8f, 0.9f + Victim.getRandom().nextFloat() * 0.15f);
    }

    public static AABB roomAt(BlockPos Pos) {
        return Scp3199Registry.Scp3199Type.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Pos));
    }

    public static boolean fits(ServerLevel World, BlockPos Pos) {
        return LeadPlanner.isDryStandable(World, Pos) && World.noCollision(roomAt(Pos)) && World.getBlockState(Pos.above(2)).getCollisionShape(World, Pos.above(2)).isEmpty();
    }

    public static boolean seenBy(ServerLevel World, ServerPlayer Victim, BlockPos Pos) {
        AABB Room = roomAt(Pos);
        Vec3 Eye = Victim.getEyePosition();
        Vec3 Look = Victim.getViewVector(1.0f);
        List<Vec3> Points = new ArrayList<>();
        Points.add(Room.getCenter());
        Points.add(new Vec3(Room.getCenter().x, Room.maxY - 0.1, Room.getCenter().z));
        Points.add(new Vec3(Room.getCenter().x, Room.minY + 0.2, Room.getCenter().z));
        for (Vec3 Point : Points) {
            Vec3 Toward = Point.subtract(Eye);
            if (Toward.lengthSqr() < 1.0e-4 || Toward.normalize().dot(Look) < 0.2) {
                continue;
            }
            if (World.clip(new ClipContext(Eye, Point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Victim)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    public static Optional<BlockPos> hiddenSpot(ServerLevel World, ServerPlayer Victim, int MinDistance, int MaxDistance, RandomSource Random) {
        float Facing = Victim.getYRot();
        BlockPos Centre = Victim.blockPosition();
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Yaw = Attempt < BehindAttempts ? Facing + 180.0 + (Random.nextDouble() * 2.0 - 1.0) * BehindArc : Random.nextDouble() * 360.0;
            double Radius = MinDistance + Random.nextDouble() * Math.max(0, MaxDistance - MinDistance);
            double Dx = -Mth.sin((float) Math.toRadians(Yaw)) * Radius;
            double Dz = Mth.cos((float) Math.toRadians(Yaw)) * Radius;
            BlockPos Column = Centre.offset((int) Math.round(Dx), 0, (int) Math.round(Dz));
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Candidate = Column.above(Dy);
                if (fits(World, Candidate)) {
                    if (!seenBy(World, Victim, Candidate)) {
                        return Optional.of(Candidate);
                    }
                    if (Fallback == null) {
                        Fallback = Candidate;
                    }
                    break;
                }
            }
        }
        return Optional.ofNullable(Fallback);
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Pos = Found.Pos();
        if (!fits(World, Pos)) {
            return false;
        }
        Scp3199 Creature = Scp3199Registry.Scp3199Type.get().create(World);
        if (Creature == null) {
            return false;
        }
        ServerPlayer Victim = victim(Found.Investigator());
        Vec3 Toward = Victim != null ? Victim.position() : Found.Investigator().position();
        Creature.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, 0.0f, 0.0f);
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(Pos), MobSpawnType.EVENT, null);
        Creature.faceTowards(Toward);
        if (!World.addFreshEntity(Creature)) {
            return false;
        }
        Scp3199Sounds.play(World, Creature.getEyePosition(), List.of(Scp3199Registry.Arrive), 1.0f, 1.0f);
        return true;
    }
}
