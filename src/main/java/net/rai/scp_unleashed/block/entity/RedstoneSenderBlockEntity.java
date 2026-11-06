package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;

public class RedstoneSenderBlockEntity extends RedstonePortBlockEntity {
    private int Input;
    private int LastSent = -1;
    private boolean Started;

    public RedstoneSenderBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.RedstoneSender.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, RedstoneSenderBlockEntity Sender) {
        if (World instanceof ServerLevel ServerWorld) {
            Sender.tickServer(ServerWorld);
        }
    }

    public void readInput() {
        if (level != null) {
            Input = level.getBestNeighborSignal(worldPosition);
        }
    }

    private void tickServer(ServerLevel World) {
        if (!Started) {
            Started = true;
            readInput();
        }
        boolean Powered = isPowered();
        int Signal = Powered ? Input : 0;
        if (Signal != LastSent || (Signal > 0 && isHeartbeat(World.getGameTime()))) {
            sendSignal(World, Port, Signal);
            LastSent = Signal;
        }
        showState(Powered, Signal, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void portChanged(int OldPort) {
        if (level instanceof ServerLevel World && LastSent > 0) {
            sendSignal(World, OldPort, 0);
        }
        LastSent = -1;
    }

    @Override
    public int getSignal() {
        return Input;
    }

    @Override
    public String getKindName() {
        return "sender";
    }
}
