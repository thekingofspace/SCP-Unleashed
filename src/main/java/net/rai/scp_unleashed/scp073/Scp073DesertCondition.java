package net.rai.scp_unleashed.scp073;

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

public record Scp073DesertCondition(int MinDelay, int CheckInterval, float Chance, int MinDesert, int MaxDesert, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp073_desert";
    public static final long StepCap = 40L;
    public static final MapCodec<Scp073DesertCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 10).forGetter(Scp073DesertCondition::MinDelay),
            Codec.intRange(1, 600).optionalFieldOf("check_interval", 5).forGetter(Scp073DesertCondition::CheckInterval),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 0.1f).forGetter(Scp073DesertCondition::Chance),
            Codec.intRange(0, 3600).optionalFieldOf("min_desert", 20).forGetter(Scp073DesertCondition::MinDesert),
            Codec.intRange(1, 7200).optionalFieldOf("max_desert", 150).forGetter(Scp073DesertCondition::MaxDesert),
            Codec.intRange(8, 96).optionalFieldOf("min_distance", 24).forGetter(Scp073DesertCondition::MinDistance),
            Codec.intRange(8, 96).optionalFieldOf("max_distance", 48).forGetter(Scp073DesertCondition::MaxDistance)
    ).apply(Instance, Scp073DesertCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong("Ready", Reader.serverLevel().getGameTime() + MinDelay * 20L);
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp073.0"),
                Component.translatable("archive.scp_unleashed.notes.scp073.1"),
                Component.translatable("archive.scp_unleashed.notes.scp073.2"));
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
            State.putLong("Ready", Now + MinDelay * 20L);
            Case.markDirty();
            return Optional.empty();
        }
        long Last = State.getLong("Seen");
        State.putLong("Seen", Now);
        if (Now < State.getLong("Ready") || !Scp073Cain.isDesert(World, Reader.blockPosition())) {
            return Optional.empty();
        }
        long Step = Last > 0L ? Math.max(0L, Math.min(StepCap, Now - Last)) : 20L;
        long Desert = State.getLong("Desert") + Step;
        State.putLong("Desert", Desert);
        Case.markDirty();
        if (Desert < MinDesert * 20L || Now < State.getLong("Next")) {
            return Optional.empty();
        }
        State.putLong("Next", Now + CheckInterval * 20L);
        boolean Due = Desert >= MaxDesert * 20L || Reader.getRandom().nextFloat() < Chance;
        if (!Due) {
            return Optional.empty();
        }
        return Scp073Arrival.findSpot(World, Reader, MinDistance, Math.max(MinDistance, MaxDistance), Reader.getRandom()).map(Spot -> new Finding(Spot, false));
    }
}
