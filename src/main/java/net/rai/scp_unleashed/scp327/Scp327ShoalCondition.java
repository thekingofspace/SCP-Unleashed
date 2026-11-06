package net.rai.scp_unleashed.scp327;

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

public record Scp327ShoalCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance, int CheckInterval) implements StartCondition {
    public static final String TypeName = "scp327_shoal";
    public static final String ReadyKey = "Ready";
    public static final String NextKey = "Next";
    public static final String TriesKey = "Tries";
    public static final List<String> NoteKeys = List.of("archive.scp_unleashed.notes.scp327.0", "archive.scp_unleashed.notes.scp327.1", "archive.scp_unleashed.notes.scp327.2");
    public static final MapCodec<Scp327ShoalCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 30).forGetter(Scp327ShoalCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 90).forGetter(Scp327ShoalCondition::MaxDelay),
            Codec.intRange(4, 128).optionalFieldOf("min_distance", 24).forGetter(Scp327ShoalCondition::MinDistance),
            Codec.intRange(4, 128).optionalFieldOf("max_distance", 96).forGetter(Scp327ShoalCondition::MaxDistance),
            Codec.intRange(1, 600).optionalFieldOf("check_interval", 5).forGetter(Scp327ShoalCondition::CheckInterval)
    ).apply(Instance, Scp327ShoalCondition::new));

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
        long Now = World.getGameTime();
        if (Now < State.getLong(ReadyKey) || Now < State.getLong(NextKey)) {
            return Optional.empty();
        }
        State.putLong(NextKey, Now + CheckInterval * 20L);
        Case.markDirty();
        Optional<Scp327Waters.Body> Water = Scp327Shoal.findWater(World, Reader.position(), MinDistance, MaxDistance, Reader.getRandom());
        if (Water.isEmpty()) {
            State.putInt(TriesKey, State.getInt(TriesKey) + 1);
            return Optional.empty();
        }
        return Water.map(Found -> new Finding(Found.Centre(), true));
    }
}
