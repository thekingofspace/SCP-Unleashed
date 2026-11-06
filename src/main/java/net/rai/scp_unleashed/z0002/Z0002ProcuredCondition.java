package net.rai.scp_unleashed.z0002;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Z0002ProcuredCondition() implements StartCondition {
    public static final String TypeName = "z0002_procured";
    public static final String NoteKey = "archive.scp_unleashed.notes.procured";
    public static final Z0002ProcuredCondition Instance = new Z0002ProcuredCondition();
    public static final MapCodec<Z0002ProcuredCondition> ConditionCodec = MapCodec.unit(() -> Instance);

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        return List.of(Component.translatable(NoteKey));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        return Optional.empty();
    }
}
