package net.rai.scp_unleashed.scp2521;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record Scp2521KnownCondition() implements StartCondition {
    public static final String TypeName = "scp2521_known";
    public static final MapCodec<Scp2521KnownCondition> ConditionCodec = MapCodec.unit(new Scp2521KnownCondition());

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Scp2521Rules.learn(Reader);
        return List.of();
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        if (!Scp2521State.get(Reader.server).taken(Reader.getUUID()) || Scp2521Trips.active(Reader.getUUID()) || Scp2521Void.inside(Reader)) {
            return Optional.empty();
        }
        return Optional.of(new Finding(Reader.blockPosition(), true));
    }

    @Override
    public boolean managesReaders() {
        return true;
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        return true;
    }
}
