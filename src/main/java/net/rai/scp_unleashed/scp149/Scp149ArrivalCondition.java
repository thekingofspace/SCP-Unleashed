package net.rai.scp_unleashed.scp149;

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
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp149ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp149_whine";
    public static final String ReadyKey = "Ready";
    public static final String TriesKey = "Tries";
    public static final int Attempts = 24;
    public static final int GiveUpTries = 30;
    public static final int CrampedMin = 2;
    public static final int CrampedMax = 4;
    public static final float ViewSpread = 30.0f;
    public static final float PitchSpread = 12.0f;
    public static final MapCodec<Scp149ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp149ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 300).forGetter(Scp149ArrivalCondition::MaxDelay),
            Codec.intRange(3, 24).optionalFieldOf("min_distance", 5).forGetter(Scp149ArrivalCondition::MinDistance),
            Codec.intRange(3, 24).optionalFieldOf("max_distance", 9).forGetter(Scp149ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp149ArrivalCondition::new));

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
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp149.0"),
                Component.translatable("archive.scp_unleashed.notes.scp149.1"),
                Component.translatable("archive.scp_unleashed.notes.scp149.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (Reader.isSpectator() || Reader.isSleeping()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        ServerLevel World = Reader.serverLevel();
        long Now = World.getGameTime();
        if (!State.contains(ReadyKey)) {
            State.putLong(ReadyKey, Now + rollDelay(Reader.getRandom()));
            Case.markDirty();
            return Optional.empty();
        }
        if (Now < State.getLong(ReadyKey) || !Scp149World.get(World.getServer()).room()) {
            return Optional.empty();
        }
        int Tries = State.getInt(TriesKey) + 1;
        State.putInt(TriesKey, Tries);
        Case.markDirty();
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        Optional<Vec3> Spot = inView(World, Reader, Low, High, ViewSpread);
        if (Spot.isEmpty() && Tries >= GiveUpTries) {
            Spot = inView(World, Reader, Low, High, 180.0f);
        }
        if (Spot.isEmpty() && Tries >= GiveUpTries * 2) {
            Spot = inView(World, Reader, CrampedMin, CrampedMax, 180.0f);
        }
        return Spot.map(Found -> new Finding(BlockPos.containing(Found), true));
    }

    public static boolean roomy(ServerLevel World, Vec3 Spot) {
        AABB Box = Scp149Registry.Fly.get().getDimensions().makeBoundingBox(Spot);
        return World.noCollision(Box) && !World.containsAnyLiquid(Box);
    }

    public static Optional<Vec3> inView(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, float Spread) {
        RandomSource Random = Reader.getRandom();
        Vec3 Eye = Reader.getEyePosition();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            float Yaw = Reader.getYRot() + (Random.nextFloat() * 2.0f - 1.0f) * Spread;
            float Pitch = Mth.clamp((Random.nextFloat() * 2.0f - 1.0f) * PitchSpread, -20.0f, 20.0f);
            double Reach = MinDistance + Random.nextDouble() * (MaxDistance - MinDistance);
            Vec3 Point = Eye.add(Vec3.directionFromRotation(Pitch, Yaw).scale(Reach)).subtract(0.0, Scp149Fly.Height * 0.5, 0.0);
            if (!World.isLoaded(BlockPos.containing(Point)) || !roomy(World, Point)) {
                continue;
            }
            HitResult Hit = World.clip(new ClipContext(Eye, Point.add(0.0, Scp149Fly.Height * 0.5, 0.0), ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, Reader));
            if (Hit.getType() == HitResult.Type.MISS) {
                return Optional.of(Point);
            }
        }
        return Optional.empty();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        ServerPlayer Reader = Found.Investigator();
        Vec3 Spot = Vec3.atCenterOf(Found.Pos()).subtract(0.0, Scp149Fly.Height * 0.5, 0.0);
        if (!roomy(World, Spot)) {
            Optional<Vec3> Again = inView(World, Reader, 4, 10, 180.0f);
            if (Again.isEmpty()) {
                return false;
            }
            Spot = Again.get();
        }
        return arrive(World, Spot, Reader).isPresent();
    }

    public static Optional<Scp149Fly> arrive(ServerLevel World, Vec3 Spot, ServerPlayer Reader) {
        if (!Scp149World.get(World.getServer()).room()) {
            return Optional.empty();
        }
        Scp149Fly Fly = Scp149Registry.Fly.get().create(World);
        if (Fly == null) {
            return Optional.empty();
        }
        Vec3 Toward = Reader.position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        Fly.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Fly.setYBodyRot(Yaw);
        Fly.setYHeadRot(Yaw);
        Fly.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        if (!World.addFreshEntity(Fly)) {
            return Optional.empty();
        }
        Fly.setTarget(Reader);
        Vec3 Middle = Spot.add(0.0, Scp149Fly.Height * 0.5, 0.0);
        World.sendParticles(Scp149Registry.Mote.get(), Middle.x, Middle.y, Middle.z, 8, 0.2, 0.2, 0.2, 0.03);
        Scp149Brood.play(World, Middle, List.of(Scp149Registry.BurstWhine, Scp149Registry.Whine), 1.1f);
        return Optional.of(Fly);
    }
}
