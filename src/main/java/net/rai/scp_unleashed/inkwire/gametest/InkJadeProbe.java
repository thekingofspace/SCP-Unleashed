package net.rai.scp_unleashed.inkwire.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.impl.BlockAccessorImpl;

final class InkJadeProbe {
    private InkJadeProbe() {
    }

    static CompoundTag serverData(ServerLevel World, Player User, BlockPos Pos) {
        BlockEntity Found = World.getBlockEntity(Pos);
        BlockAccessor Accessor = new BlockAccessorImpl.Builder()
                .level(World)
                .player(User)
                .blockState(World.getBlockState(Pos))
                .blockEntity(() -> Found)
                .hit(new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false))
                .serverConnected(true)
                .build();
        CompoundTag Data = new CompoundTag();
        ScpPowerProvider.Instance.appendServerData(Data, Accessor);
        return Data;
    }
}
