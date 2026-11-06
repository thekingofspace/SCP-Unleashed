package net.rai.scp_unleashed.scp105;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp105SightingCondition(int MinDelay, int MaxDelay, int CheckInterval, int NightGrace, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp105_sighting";
    public static final MapCodec<Scp105SightingCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp105SightingCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 180).forGetter(Scp105SightingCondition::MaxDelay),
            Codec.intRange(1, 600).optionalFieldOf("check_interval", 5).forGetter(Scp105SightingCondition::CheckInterval),
            Codec.intRange(0, 86400).optionalFieldOf("night_grace", 120).forGetter(Scp105SightingCondition::NightGrace),
            Codec.intRange(8, 96).optionalFieldOf("min_distance", 20).forGetter(Scp105SightingCondition::MinDistance),
            Codec.intRange(8, 96).optionalFieldOf("max_distance", 40).forGetter(Scp105SightingCondition::MaxDistance)
    ).apply(Instance, Scp105SightingCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long readyAt(ServerPlayer Reader) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return Reader.serverLevel().getGameTime() + (Low + Reader.getRandom().nextInt(High - Low + 1)) * 20L;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong("Ready", readyAt(Reader));
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp105.0"),
                Component.translatable("archive.scp_unleashed.notes.scp105.1"),
                Component.translatable("archive.scp_unleashed.notes.scp105.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (Reader.isSpectator() || Reader.isSleeping()) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        long Now = World.getGameTime();
        if (!State.contains("Ready")) {
            State.putLong("Ready", readyAt(Reader));
            Case.markDirty();
            return Optional.empty();
        }
        long Ready = State.getLong("Ready");
        if (Now < Ready || Now < State.getLong("Next")) {
            return Optional.empty();
        }
        State.putLong("Next", Now + CheckInterval * 20L);
        Case.markDirty();
        if (!Scp105Arrival.skyWorld(World)) {
            return Optional.empty();
        }
        if (!World.isDay() && Now < Ready + NightGrace * 20L) {
            return Optional.empty();
        }
        return Scp105Arrival.findSpot(World, Reader, MinDistance, Math.max(MinDistance, MaxDistance), Reader.getRandom()).map(Spot -> new Finding(Spot, false));
    }
}
