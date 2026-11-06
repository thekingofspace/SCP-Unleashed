package net.rai.scp_unleashed.chunkloader;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Map;

public final class ChunkLoaderTickets {
    public static final ResourceLocation ControllerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chunk_loader");
    public static final TicketController Controller = new TicketController(ControllerId, ChunkLoaderTickets::validate);

    private ChunkLoaderTickets() {
    }

    public static boolean hold(ServerLevel World, BlockPos Owner, long Chunk) {
        return Controller.forceChunk(World, Owner, ChunkPos.getX(Chunk), ChunkPos.getZ(Chunk), true, true);
    }

    public static boolean release(ServerLevel World, BlockPos Owner, long Chunk) {
        return Controller.forceChunk(World, Owner, ChunkPos.getX(Chunk), ChunkPos.getZ(Chunk), false, true);
    }

    static void validate(ServerLevel World, TicketHelper Helper) {
        for (Map.Entry<BlockPos, TicketSet> Entry : Map.copyOf(Helper.getBlockTickets()).entrySet()) {
            BlockPos Owner = Entry.getKey();
            TicketSet Tickets = Entry.getValue();
            if (!(World.getBlockEntity(Owner) instanceof ChunkLoaderBlockEntity Loader)) {
                Helper.removeAllTickets(Owner);
                continue;
            }
            for (long Chunk : new LongOpenHashSet(Tickets.nonTicking())) {
                Helper.removeTicket(Owner, Chunk, false);
            }
            LongSet Kept = new LongOpenHashSet();
            for (long Chunk : new LongOpenHashSet(Tickets.ticking())) {
                if (Loader.wantsOnLoad(Chunk)) {
                    Kept.add(Chunk);
                } else {
                    Helper.removeTicket(Owner, Chunk, true);
                }
            }
            Loader.adoptTickets(Kept);
        }
    }
}
