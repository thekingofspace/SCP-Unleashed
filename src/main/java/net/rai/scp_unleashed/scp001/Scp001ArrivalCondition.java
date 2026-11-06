package net.rai.scp_unleashed.scp001;

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
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp001ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp001_apocalypse";
    public static final String ReadyKey = "Ready";
    public static final int Attempts = 36;
    public static final int Vertical = 4;
    public static final double FrontArc = 70.0;
    public static final List<String> NoteKeys = List.of("archive.scp_unleashed.notes.scp001.0", "archive.scp_unleashed.notes.scp001.1");
    public static final MapCodec<Scp001ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 20).forGetter(Scp001ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 60).forGetter(Scp001ArrivalCondition::MaxDelay),
            Codec.intRange(4, 48).optionalFieldOf("min_distance", 10).forGetter(Scp001ArrivalCondition::MinDistance),
            Codec.intRange(4, 48).optionalFieldOf("max_distance", 18).forGetter(Scp001ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp001ArrivalCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private void schedule(ServerPlayer Reader, Investigation Case) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        long Delay = (Low + (long) Reader.getRandom().nextInt(High - Low + 1)) * 20L;
        Case.getState().putLong(ReadyKey, Reader.serverLevel().getGameTime() + Delay);
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        schedule(Reader, Case);
        return NoteKeys.stream().<Component>map(Component::translatable).toList();
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
        return arrivalSpot(World, Reader, MinDistance, MaxDistance, Reader.getRandom()).map(Pos -> new Finding(Pos, true));
    }

    public static Optional<BlockPos> arrivalSpot(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, RandomSource Random) {
        Scp001Prototype Probe = Scp001Registry.Prototype.get().create(World);
        if (Probe == null) {
            return Optional.empty();
        }
        float Facing = Reader.getYRot();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Yaw = Attempt < Attempts / 2 ? Facing + (Random.nextDouble() * 2.0 - 1.0) * FrontArc : Random.nextDouble() * 360.0;
            double Radius = MinDistance + Random.nextDouble() * Math.max(0, MaxDistance - MinDistance);
            BlockPos Column = BlockPos.containing(Reader.getX() - Mth.sin((float) Math.toRadians(Yaw)) * Radius, Reader.getY(), Reader.getZ() + Mth.cos((float) Math.toRadians(Yaw)) * Radius);
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Candidate = Column.above(Dy);
                if (Scp001Warp.fits(World, Probe, Candidate)) {
                    return Optional.of(Candidate);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Scp001Prototype Creature = Scp001Registry.Prototype.get().create(World);
        if (Creature == null || !Scp001Warp.fits(World, Creature, Found.Pos())) {
            return false;
        }
        Vec3 Bottom = Vec3.atBottomCenterOf(Found.Pos());
        Vec3 Toward = Found.Investigator().position().subtract(Bottom);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        if (!Scp001Returns.emerge(World, Creature, Found.Pos(), Yaw)) {
            return false;
        }
        Creature.setTarget(Found.Investigator());
        return true;
    }
}
