package net.rai.scp_unleashed.scp439;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp439ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp439_arrival";
    public static final String ReadyKey = "Ready";
    public static final long LenientAfter = 2400L;
    public static final int Vertical = 8;
    public static final MapCodec<Scp439ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp439ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 240).forGetter(Scp439ArrivalCondition::MaxDelay),
            Codec.intRange(8, 96).optionalFieldOf("min_distance", 24).forGetter(Scp439ArrivalCondition::MinDistance),
            Codec.intRange(8, 96).optionalFieldOf("max_distance", 48).forGetter(Scp439ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp439ArrivalCondition::new));

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
                Component.translatable("archive.scp_unleashed.notes.scp439.0"),
                Component.translatable("archive.scp_unleashed.notes.scp439.1"),
                Component.translatable("archive.scp_unleashed.notes.scp439.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (!ReaderTargets.isReady(Reader) || Reader.isSleeping()) {
            return Optional.empty();
        }
        ServerLevel World = Reader.serverLevel();
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
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        Optional<BlockPos> Found = LeadPlanner.standableNear(World, Reader.blockPosition(), Low, High, Vertical, Reader.getRandom());
        if (Found.isEmpty() && Now - Ready > LenientAfter) {
            Found = LeadPlanner.standableNear(World, Reader.blockPosition(), 6, Low, Vertical, Reader.getRandom());
        }
        return Found.map(Pos -> new Finding(Pos, true));
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        BlockPos Pos = Found.Pos();
        Vec3 Spot = Vec3.atBottomCenterOf(Pos);
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) Math.toDegrees(Math.atan2(-Toward.x, Toward.z));
        return Scp439Hive.release(Found.World(), Spot, Yaw, new CompoundTag()) != null;
    }
}
