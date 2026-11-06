package net.rai.scp_unleashed.computer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.computer.data.ChatMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class ChatServerBlockEntity extends ComputerDeviceEntity {
    public static final int Draw = 6;
    public static final int BufferSize = 30;
    public static final long BufferTicks = 12000L;

    private final ArrayDeque<Buffered> Buffer = new ArrayDeque<>();

    public ChatServerBlockEntity(BlockPos Pos, BlockState State) {
        super(ComputerRegistry.ChatServerEntity.get(), Pos, State);
    }

    @Override
    public int getDraw() {
        return Draw;
    }

    @Override
    protected void powerChanged(ServerLevel World, boolean Running) {
        if (!Running) {
            Buffer.clear();
        }
    }

    @Override
    protected void tickDevice(ServerLevel World, long Now) {
        if (Math.floorMod(Now + worldPosition.asLong(), 100L) == 0L) {
            prune(Now);
        }
    }

    public void buffer(ChatMessage Message, long Now) {
        for (Buffered Held : Buffer) {
            if (Held.Message().Id() == Message.Id()) {
                return;
            }
        }
        Buffer.addLast(new Buffered(Message, Now));
        while (Buffer.size() > BufferSize) {
            Buffer.removeFirst();
        }
    }

    public List<ChatMessage> recent(long Now) {
        prune(Now);
        List<ChatMessage> Found = new ArrayList<>(Buffer.size());
        for (Buffered Held : Buffer) {
            Found.add(Held.Message());
        }
        return Found;
    }

    public long lastId(long Now) {
        prune(Now);
        return Buffer.isEmpty() ? 0L : Buffer.getLast().Message().Id();
    }

    private void prune(long Now) {
        while (!Buffer.isEmpty() && Now - Buffer.getFirst().At() > BufferTicks) {
            Buffer.removeFirst();
        }
    }

    private record Buffered(ChatMessage Message, long At) {
    }
}
