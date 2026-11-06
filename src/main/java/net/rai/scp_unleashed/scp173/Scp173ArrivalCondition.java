package net.rai.scp_unleashed.scp173;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp173ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp173_arrival";
    public static final String ReadyKey = "Ready";
    public static final int Attempts = 40;
    public static final int BehindAttempts = 28;
    public static final double BehindArc = 70.0;
    public static final int Vertical = 4;
    public static final MapCodec<Scp173ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp173ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 240).forGetter(Scp173ArrivalCondition::MaxDelay),
            Codec.intRange(4, 48).optionalFieldOf("min_distance", 8).forGetter(Scp173ArrivalCondition::MinDistance),
            Codec.intRange(4, 48).optionalFieldOf("max_distance", 20).forGetter(Scp173ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp173ArrivalCondition::new));

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
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        schedule(Reader, Case);
        return List.of(Component.translatable("archive.scp_unleashed.notes.scp173.0"), Component.translatable("archive.scp_unleashed.notes.scp173.1"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (Reader.isSpectator() || !Reader.isAlive()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        if (!State.contains(ReadyKey)) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        ServerLevel World = Reader.serverLevel();
        if (World.getGameTime() < State.getLong(ReadyKey)) {
            return Optional.empty();
        }
        return hiddenSpot(World, Reader, MinDistance, MaxDistance, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static AABB roomAt(BlockPos Pos) {
        return Scp173Registry.Statue.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Pos));
    }

    public static boolean fits(ServerLevel World, BlockPos Pos) {
        return LeadPlanner.isDryStandable(World, Pos) && World.noCollision(roomAt(Pos));
    }

    public static Optional<BlockPos> hiddenSpot(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, RandomSource Random) {
        float Facing = Reader.getYRot();
        BlockPos Centre = Reader.blockPosition();
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
                    if (!Scp173Sight.anyoneCouldSee(World, roomAt(Candidate))) {
                        return Optional.of(Candidate);
                    }
                    break;
                }
            }
        }
        return Optional.empty();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Pos = Found.Pos();
        if (!fits(World, Pos) || Scp173Sight.anyoneCouldSee(World, roomAt(Pos))) {
            return false;
        }
        Scp173Statue Statue = Scp173Registry.Statue.get().create(World);
        if (Statue == null) {
            return false;
        }
        Statue.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, 0.0f, 0.0f);
        Statue.finalizeSpawn(World, World.getCurrentDifficultyAt(Pos), MobSpawnType.EVENT, null);
        Statue.faceTowards(Found.Investigator().position());
        Statue.pickNewPose();
        Statue.hunt().markMoved();
        return World.addFreshEntity(Statue);
    }
}
