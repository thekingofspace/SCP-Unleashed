package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PheromoneCommunicatorBlockEntity extends BlockEntity {
    public static final int SenseTicks = 20;

    public PheromoneCommunicatorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp610Registry.CommunicatorEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PheromoneCommunicatorBlockEntity Device) {
        if (World instanceof ServerLevel Server && (Server.getGameTime() + Pos.asLong()) % SenseTicks == 0) {
            Device.sense(Server, Pos, State);
        }
    }

    public void sense(ServerLevel World, BlockPos Pos, BlockState State) {
        boolean Heard = PheromoneCommunicatorBlock.listening(World, Pos);
        if (State.getValue(PheromoneCommunicatorBlock.Active) != Heard) {
            World.setBlock(Pos, State.setValue(PheromoneCommunicatorBlock.Active, Heard), Block.UPDATE_CLIENTS);
        }
    }
}
