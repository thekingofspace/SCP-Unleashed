package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.NetworkObjectBlock;
import net.rai.scp_unleashed.block.RedstonePortBlock;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedstoneReceiverBlockEntity extends RedstonePortBlockEntity {
    public static final int Timeout = 30;

    private final Map<BlockPos, Heard> Sources = new HashMap<>();
    private long StartedAt = -1;

    public RedstoneReceiverBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.RedstoneReceiver.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, RedstoneReceiverBlockEntity Receiver) {
        Receiver.tickServer(World.getGameTime());
    }

    private void tickServer(long Now) {
        if (StartedAt < 0) {
            StartedAt = Now;
        }
        Sources.values().removeIf(Entry -> Now - Entry.Seen() > Timeout);
        refreshOutput();
    }

    @Override
    public Collection<String> getSubscriptions() {
        return List.of(getChannel());
    }

    public String getChannel() {
        return WireChannels.redstonePort(Port);
    }

    @Override
    public void receiveMessage(WireMessage Message) {
        if (level == null || !Message.Channel().equals(getChannel())) {
            return;
        }
        int Signal = Mth.clamp(Message.Payload().getInt(SignalKey), 0, 15);
        if (Signal > 0) {
            Sources.put(Message.Source(), new Heard(Signal, level.getGameTime()));
        } else {
            Sources.remove(Message.Source());
        }
        refreshOutput();
    }

    private void refreshOutput() {
        if (level == null || StartedAt < 0) {
            return;
        }
        boolean Powered = isPowered();
        int Signal = 0;
        if (Powered) {
            for (Heard Entry : Sources.values()) {
                Signal = Math.max(Signal, Entry.Signal());
            }
        }
        BlockState State = getBlockState();
        if (level.getGameTime() - StartedAt <= Timeout && State.getBlock() instanceof RedstonePortBlock) {
            Powered |= State.getValue(NetworkObjectBlock.Active);
            Signal = Math.max(Signal, State.getValue(RedstonePortBlock.Power));
        }
        showState(Powered, Signal, Block.UPDATE_ALL);
    }

    @Override
    protected void portChanged(int OldPort) {
        Sources.clear();
        if (level != null) {
            StartedAt = level.getGameTime() - Timeout - 1;
        }
        refreshOutput();
    }

    @Override
    public int getSignal() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof RedstonePortBlock ? State.getValue(RedstonePortBlock.Power) : 0;
    }

    @Override
    public String getKindName() {
        return "receiver";
    }

    private record Heard(int Signal, long Seen) {
    }
}
