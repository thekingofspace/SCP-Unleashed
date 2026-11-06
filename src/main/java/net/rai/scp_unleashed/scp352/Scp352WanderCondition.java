package net.rai.scp_unleashed.scp352;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Scp352WanderCondition(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp352_wander";
    public static final String ReadyKey = "Ready";
    public static final String NextKey = "Next";
    public static final String TriesKey = "Tries";
    public static final int CheckTicks = 40;
    public static final int StrictTries = 6;
    public static final int LenientTries = 14;
    public static final long HandOffDelay = 600L;
    public static final List<String> NoteKeys = List.of("archive.scp_unleashed.notes.scp352.0", "archive.scp_unleashed.notes.scp352.1");
    public static final MapCodec<Scp352WanderCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(Scp352WanderCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 180).forGetter(Scp352WanderCondition::MaxDelay),
            Codec.intRange(16, 160).optionalFieldOf("min_distance", 40).forGetter(Scp352WanderCondition::MinDistance),
            Codec.intRange(16, 160).optionalFieldOf("max_distance", 80).forGetter(Scp352WanderCondition::MaxDistance)
    ).apply(Instance, Scp352WanderCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private void schedule(ServerPlayer Reader, Investigation Case) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        long Delay = (Low + (long) Reader.getRandom().nextInt(High - Low + 1)) * 20L;
        CompoundTag State = Case.getState();
        State.putLong(ReadyKey, Reader.serverLevel().getGameTime() + Delay);
        State.putInt(TriesKey, 0);
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
        State.putLong(NextKey, Now + CheckTicks);
        int Tries = State.getInt(TriesKey);
        State.putInt(TriesKey, Tries + 1);
        Case.markDirty();
        int Mode = Tries < StrictTries ? Scp352Arrival.Strict : Tries < LenientTries ? Scp352Arrival.Lenient : Scp352Arrival.Forced;
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        return Scp352Arrival.findSite(World, Reader, Low, High, Mode, Reader.getRandom()).map(Pos -> new Finding(Pos, false));
    }

    public static void handOff(MinecraftServer Server, UUID CaseId, UUID From, UUID To) {
        Investigations Data = Investigations.get(Server);
        for (Investigation Case : Data.of(To)) {
            if (!Case.getId().equals(CaseId) || !Case.getDocument().equals(Scp352Registry.DocumentId)) {
                continue;
            }
            CompoundTag State = Case.getState();
            long Now = Server.overworld().getGameTime();
            State.putLong(ReadyKey, Math.max(State.getLong(ReadyKey), Now + HandOffDelay));
            State.putInt(TriesKey, 0);
            State.remove(NextKey);
            Case.markDirty();
            Case.consumeDirty();
            Data.setDirty();
        }
    }
}
