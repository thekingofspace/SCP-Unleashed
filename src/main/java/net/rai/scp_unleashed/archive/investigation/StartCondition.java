package net.rai.scp_unleashed.archive.investigation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public interface StartCondition {
    String getTypeName();

    List<Component> begin(ServerPlayer Reader, Investigation Case);

    Optional<Finding> tick(ServerPlayer Reader, Investigation Case);

    default boolean managesReaders() {
        return false;
    }

    record Finding(BlockPos Pos, boolean Manifested) {
    }
}
