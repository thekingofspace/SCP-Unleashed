package net.rai.scp_unleashed.scp027;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp027SightingCondition(int MinDelay, int MaxDelay, int CheckInterval, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp027_sighting";
    public static final String ReadyKey = "Ready";
    public static final String NextKey = "Next";
    public static final MapCodec<Scp027SightingCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp027SightingCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 180).forGetter(Scp027SightingCondition::MaxDelay),
            Codec.intRange(1, 600).optionalFieldOf("check_interval", 5).forGetter(Scp027SightingCondition::CheckInterval),
            Codec.intRange(8, 96).optionalFieldOf("min_distance", 16).forGetter(Scp027SightingCondition::MinDistance),
            Codec.intRange(8, 96).optionalFieldOf("max_distance", 32).forGetter(Scp027SightingCondition::MaxDistance)
    ).apply(Instance, Scp027SightingCondition::new));

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
        Case.getState().putLong(ReadyKey, readyAt(Reader));
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp027.0"),
                Component.translatable("archive.scp_unleashed.notes.scp027.1"),
                Component.translatable("archive.scp_unleashed.notes.scp027.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ServerLevel World = Reader.serverLevel();
        if (Reader.isSpectator() || Reader.isSleeping() || !AnomalyRealm.allows(World)) {
            return Optional.empty();
        }
        CompoundTag State = Case.getState();
        long Now = World.getGameTime();
        if (!State.contains(ReadyKey)) {
            State.putLong(ReadyKey, readyAt(Reader));
            Case.markDirty();
            return Optional.empty();
        }
        if (Now < State.getLong(ReadyKey) || Now < State.getLong(NextKey)) {
            return Optional.empty();
        }
        State.putLong(NextKey, Now + CheckInterval * 20L);
        Case.markDirty();
        return Scp027Arrival.findSpot(World, Reader, MinDistance, Math.max(MinDistance, MaxDistance), Reader.getRandom()).map(Spot -> new Finding(Spot, false));
    }
}
