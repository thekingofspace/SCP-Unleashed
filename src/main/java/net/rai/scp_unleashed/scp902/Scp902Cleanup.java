package net.rai.scp_unleashed.scp902;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp902Cleanup {
    public static final int ChunksPerTick = 6;
    public static final int Margin = 4;
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<MinecraftServer, Map<Integer, Integer>> Progress = new ConcurrentHashMap<>();

    private Scp902Cleanup() {
    }

    public static int minX(int Slot) {
        return Scp902Wastes.centre(Slot).getX() + Scp902Wastes.TemplateWestX - Margin;
    }

    public static int maxX(int Slot) {
        return Scp902Wastes.centre(Slot).getX() + Scp902Wastes.TemplateEastX + Margin;
    }

    public static int minZ(int Slot) {
        return Scp902Wastes.centre(Slot).getZ() + Scp902Wastes.TemplateNorthZ - Margin;
    }

    public static int maxZ(int Slot) {
        return Scp902Wastes.centre(Slot).getZ() + Scp902Wastes.TemplateSouthZ + Margin;
    }

    public static AABB area(int Slot) {
        return new AABB(minX(Slot), Scp902Wastes.TemplateY, minZ(Slot), maxX(Slot) + 1, Scp902Wastes.TemplateTopY + 8, maxZ(Slot) + 1);
    }

    public static List<ChunkPos> chunks(int Slot) {
        List<ChunkPos> Found = new ArrayList<>();
        for (int Cx = minX(Slot) >> 4; Cx <= maxX(Slot) >> 4; Cx++) {
            for (int Cz = minZ(Slot) >> 4; Cz <= maxZ(Slot) >> 4; Cz++) {
                Found.add(new ChunkPos(Cx, Cz));
            }
        }
        return Found;
    }

    public static void tick(MinecraftServer Server) {
        Scp902State State = Scp902State.get(Server);
        List<Integer> Waiting = State.dirtySlots();
        if (Waiting.isEmpty()) {
            return;
        }
        ServerLevel World = Scp902Wastes.level(Server);
        if (World == null) {
            return;
        }
        Map<Integer, Integer> Cursor = Progress.computeIfAbsent(Server, Key -> new ConcurrentHashMap<>());
        int Budget = ChunksPerTick;
        for (int Slot : Waiting) {
            if (Budget <= 0) {
                break;
            }
            if (State.occupied(Slot)) {
                continue;
            }
            List<ChunkPos> Chunks = chunks(Slot);
            int Index = Cursor.getOrDefault(Slot, 0);
            while (Budget > 0 && Index < Chunks.size()) {
                clear(World, Slot, Chunks.get(Index));
                Index++;
                Budget--;
            }
            if (Index >= Chunks.size()) {
                finish(World, Slot);
                Cursor.remove(Slot);
                State.cleaned(Slot);
            } else {
                Cursor.put(Slot, Index);
            }
        }
    }

    public static void wipe(ServerLevel World, int Slot) {
        for (ChunkPos Chunk : chunks(Slot)) {
            clear(World, Slot, Chunk);
        }
    }

    public static void clearNow(ServerLevel World, int Slot) {
        wipe(World, Slot);
        finish(World, Slot);
        Progress.computeIfAbsent(World.getServer(), Key -> new ConcurrentHashMap<>()).remove(Slot);
        Scp902State.get(World.getServer()).cleaned(Slot);
    }

    private static void clear(ServerLevel World, int Slot, ChunkPos Chunk) {
        int FromX = Math.max(minX(Slot), Chunk.getMinBlockX());
        int ToX = Math.min(maxX(Slot), Chunk.getMaxBlockX());
        int FromZ = Math.max(minZ(Slot), Chunk.getMinBlockZ());
        int ToZ = Math.min(maxZ(Slot), Chunk.getMaxBlockZ());
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = FromX; X <= ToX; X++) {
            for (int Z = FromZ; Z <= ToZ; Z++) {
                for (int Y = Scp902Wastes.TemplateY; Y <= Scp902Wastes.TemplateTopY; Y++) {
                    Cursor.set(X, Y, Z);
                    BlockState Wanted = Scp902Wastes.flat(Y);
                    if (World.getBlockState(Cursor) != Wanted) {
                        World.setBlock(Cursor, Wanted, Flags);
                    }
                }
            }
        }
    }

    private static void finish(ServerLevel World, int Slot) {
        for (Entity Left : World.getEntitiesOfClass(Entity.class, area(Slot), Found -> !(Found instanceof Player))) {
            Left.discard();
        }
        Vec3 Arrival = Scp902Wastes.arrival(Slot);
        BlockPos Centre = BlockPos.containing(Arrival);
        World.getChunkSource().removeRegionTicket(TicketType.PORTAL, new ChunkPos(Centre), Scp902Wastes.ArrivalTicketRadius, Centre);
    }

    public static void forget(MinecraftServer Server) {
        Progress.remove(Server);
    }
}
