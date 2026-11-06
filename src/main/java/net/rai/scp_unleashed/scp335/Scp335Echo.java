package net.rai.scp_unleashed.scp335;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rai.scp_unleashed.computer.ComputerScp079;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.net.ChatService;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079Grid;

import java.util.Optional;

public final class Scp335Echo {
    public static final String LineKey = "chat.scp_unleashed.scp335.scp079.";
    public static final int Lines = 4;
    public static final int ChunkRadius = 3;

    private Scp335Echo() {
    }

    public static boolean echo(ServerLevel World, BlockPos Computer, RandomSource Random) {
        ComputerGrid.Reach Reach = ComputerGrid.reach(World, Computer);
        if (Reach.chatServer() == null || !watching(World, Computer)) {
            return false;
        }
        ChatMessage Draft = new ChatMessage(0L, ChatMessage.Sender.Anomaly, ComputerScp079.Prefix + "name.0", LineKey + Random.nextInt(Lines), "", Optional.empty(),
                World.getDayTime(), false);
        return ChatService.post(World, Reach, ChatChannels.Network, Draft) == ChatService.Result.Posted;
    }

    private static boolean watching(ServerLevel World, BlockPos Computer) {
        int ChunkX = Computer.getX() >> 4;
        int ChunkZ = Computer.getZ() >> 4;
        for (int OffsetX = -ChunkRadius; OffsetX <= ChunkRadius; OffsetX++) {
            for (int OffsetZ = -ChunkRadius; OffsetZ <= ChunkRadius; OffsetZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX + OffsetX, ChunkZ + OffsetZ);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Found : Chunk.getBlockEntities().values()) {
                    if (Found instanceof Scp079BlockEntity Machine && Machine.isPowered() && Scp079Grid.reach(World, Machine).Devices().containsKey(Computer)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
