package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.NetworkObjectBlock;
import net.rai.scp_unleashed.block.RedstonePortBlock;
import net.rai.scp_unleashed.wire.WireChannels;

public abstract class RedstonePortBlockEntity extends NetworkObjectBlockEntity {
    public static final int Heartbeat = 10;
    public static final String SignalKey = "Signal";

    protected RedstonePortBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public abstract int getSignal();

    protected void showState(boolean Powered, int Signal, int Flags) {
        BlockState State = getBlockState();
        if (level == null || !(State.getBlock() instanceof RedstonePortBlock)) {
            return;
        }
        if (State.getValue(NetworkObjectBlock.Active) != Powered || State.getValue(RedstonePortBlock.Power) != Signal) {
            level.setBlock(worldPosition, State.setValue(NetworkObjectBlock.Active, Powered).setValue(RedstonePortBlock.Power, Signal), Flags);
        }
    }

    protected void sendSignal(ServerLevel World, int OnPort, int Signal) {
        CompoundTag Payload = new CompoundTag();
        Payload.putInt(SignalKey, Signal);
        send(World, WireChannels.redstonePort(OnPort), Payload);
    }

    protected boolean isHeartbeat(long Now) {
        return Math.floorMod(Now + worldPosition.asLong(), Heartbeat) == 0;
    }
}
