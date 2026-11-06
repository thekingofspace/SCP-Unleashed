package net.rai.scp_unleashed.scp303;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp303ArrivalCondition(int MinDelay, int MaxDelay, int CheckInterval, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp303_arrival";
    public static final String ReadyKey = "Ready";
    public static final String NextKey = "Next";
    public static final long LenientAfter = 1200L;
    public static final MapCodec<Scp303ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp303ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 180).forGetter(Scp303ArrivalCondition::MaxDelay),
            Codec.intRange(1, 600).optionalFieldOf("check_interval", 5).forGetter(Scp303ArrivalCondition::CheckInterval),
            Codec.intRange(6, 64).optionalFieldOf("min_distance", 16).forGetter(Scp303ArrivalCondition::MinDistance),
            Codec.intRange(6, 64).optionalFieldOf("max_distance", 28).forGetter(Scp303ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp303ArrivalCondition::new));

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
        Scp303Unlock.unlock(Reader);
        return List.of(Component.translatable("archive.scp_unleashed.notes.scp303.0"), Component.translatable("archive.scp_unleashed.notes.scp303.1"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (Reader.isSpectator() || !Reader.isAlive() || Reader.isSleeping() || !Scp303Arrival.realm(Reader.serverLevel())) {
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
        if (Now < Ready || Now < State.getLong(NextKey)) {
            return Optional.empty();
        }
        State.putLong(NextKey, Now + CheckInterval * 20L);
        Case.markDirty();
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        return Scp303Spots.hidden(World, Reader, Low, High, Reader.getRandom(), null, true, Now - Ready > LenientAfter)
                .map(Spot -> new Finding(BlockPos.containing(Spot), false));
    }
}
