package net.rai.scp_unleashed.scp163;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp163CrashCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp163_crash";
    public static final MapCodec<Scp163CrashCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp163CrashCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 290).forGetter(Scp163CrashCondition::MaxDelay),
            Codec.intRange(12, 256).optionalFieldOf("min_distance", 28).forGetter(Scp163CrashCondition::MinDistance),
            Codec.intRange(12, 256).optionalFieldOf("max_distance", 60).forGetter(Scp163CrashCondition::MaxDistance)
    ).apply(Instance, Scp163CrashCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long rollDelay(ServerPlayer Reader) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return (Low + (long) Reader.getRandom().nextInt(High - Low + 1)) * 20L;
    }

    private void scheduleFor(ServerPlayer Reader, Investigation Case) {
        long Now = Reader.server.overworld().getGameTime();
        Scp163Crashes.get(Reader.server).schedule(Case.getId(), Reader.getUUID(), Case.getDocument(), Now + rollDelay(Reader), MinDistance, MaxDistance);
        Case.getState().putBoolean("Scheduled", true);
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        scheduleFor(Reader, Case);
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp163_crash"),
                Component.translatable("archive.scp_unleashed.notes.scp163_crash.unknown"),
                Component.translatable("archive.scp_unleashed.notes.scp163_crash.advice"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        Optional<Scp163Crashes.Crash> Entry = Scp163Crashes.get(Reader.server).forCase(Case.getId());
        if (Entry.isEmpty()) {
            CompoundTag State = Case.getState();
            if (!State.getBoolean("Rescheduled")) {
                State.putBoolean("Rescheduled", true);
                scheduleFor(Reader, Case);
            }
            return Optional.empty();
        }
        if (Entry.get().isLanded()) {
            return Reader.level().dimension().location().toString().equals(Entry.get().getDimension()) ? Optional.of(new Finding(Entry.get().getSite(), true)) : Optional.empty();
        }
        return Optional.empty();
    }
}
