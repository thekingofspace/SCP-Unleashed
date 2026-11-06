package net.rai.scp_unleashed.scp343;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp343ArrivalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp343_arrival";
    public static final int Tries = 4;
    public static final MapCodec<Scp343ArrivalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp343ArrivalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 240).forGetter(Scp343ArrivalCondition::MaxDelay),
            Codec.intRange(2, 64).optionalFieldOf("min_distance", 6).forGetter(Scp343ArrivalCondition::MinDistance),
            Codec.intRange(2, 64).optionalFieldOf("max_distance", 16).forGetter(Scp343ArrivalCondition::MaxDistance)
    ).apply(Instance, Scp343ArrivalCondition::new));

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
        Case.getState().putLong("Ready", Reader.serverLevel().getGameTime() + rollDelay(Reader.getRandom()));
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        schedule(Reader, Case);
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp343.0"),
                Component.translatable("archive.scp_unleashed.notes.scp343.1"),
                Component.translatable("archive.scp_unleashed.notes.scp343.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (Reader.isSpectator() || Reader.isSleeping()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        if (!State.contains("Ready")) {
            schedule(Reader, Case);
            return Optional.empty();
        }
        ServerLevel World = Reader.serverLevel();
        if (World.getGameTime() < State.getLong("Ready")) {
            return Optional.empty();
        }
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        return Scp343Walls.open(World, Reader.blockPosition(), Low, High, Reader.getRandom(), null, 0.0, Tries).map(Spot -> new Finding(Spot.Stand(), true));
    }
}
