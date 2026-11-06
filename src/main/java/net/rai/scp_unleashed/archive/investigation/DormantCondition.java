package net.rai.scp_unleashed.archive.investigation;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public record DormantCondition(String TypeName) implements StartCondition {
    public static MapCodec<DormantCondition> codecFor(String Name) {
        return MapCodec.unit(() -> new DormantCondition(Name));
    }

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        return List.of();
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        return Optional.empty();
    }
}
