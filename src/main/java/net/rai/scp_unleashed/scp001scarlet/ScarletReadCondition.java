package net.rai.scp_unleashed.scp001scarlet;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;

import java.util.List;
import java.util.Optional;

public record ScarletReadCondition() implements StartCondition {
    public static final String TypeName = "scp001_scarlet_read";
    public static final MapCodec<ScarletReadCondition> ConditionCodec = MapCodec.unit(new ScarletReadCondition());

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        ScarletMeter.read(Reader);
        return List.of(Component.translatable("archive.scp_unleashed.notes.scp001_scarlet.0"), Component.translatable("archive.scp_unleashed.notes.scp001_scarlet.1"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ScarletState State = ScarletState.get(Reader.server);
        ScarletState.Phase Phase = State.phase();
        if (!State.thronePlaced() || Phase == ScarletState.Phase.Asleep || Phase == ScarletState.Phase.Stirring || Phase == ScarletState.Phase.Freed) {
            return Optional.empty();
        }
        return Optional.of(new Finding(State.throne(), false));
    }

    public static boolean surface(AnomalySpawners.Discovery Found) {
        return ScarletState.get(Found.World().getServer()).thronePlaced();
    }
}
