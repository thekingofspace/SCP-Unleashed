package net.rai.scp_unleashed.scp280;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.InvestigationTracker;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp280EyesCondition() implements StartCondition {
    public static final String TypeName = "scp280_eyes";
    public static final String UnlockedKey = "Unlocked";
    public static final int NoteCount = 3;
    public static final MapCodec<Scp280EyesCondition> ConditionCodec = MapCodec.unit(Scp280EyesCondition::new);

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public boolean managesReaders() {
        return true;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Scp280Haunt.unlock(Reader);
        Case.getState().putBoolean(UnlockedKey, true);
        Case.markDirty();
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp280.0"),
                Component.translatable("archive.scp_unleashed.notes.scp280.1"),
                Component.translatable("archive.scp_unleashed.notes.scp280.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (!Case.getState().getBoolean(UnlockedKey)) {
            Scp280Haunt.unlock(Reader);
            Case.getState().putBoolean(UnlockedKey, true);
            Case.markDirty();
        }
        Scp280World Data = Scp280World.get(Reader.server);
        if (Data.hasWitnessed(Reader.getUUID()) || Data.isCaptured()) {
            Investigations.get(Reader.server).remove(Reader.getUUID(), Case);
            InvestigationTracker.markResolved(Reader, Case.getDocument(), Case.getId());
        }
        return Optional.empty();
    }
}
