package net.rai.scp_unleashed.raid.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class RaidStash {
    private RaidStash() {
    }

    public static boolean holds(ServerLevel World, BlockPos Pos, Predicate<ItemStack> Wanted) {
        if (!World.isLoaded(Pos)) {
            return false;
        }
        BlockEntity Tile = World.getBlockEntity(Pos);
        if (Tile instanceof StorageContainerBlockEntity Locker) {
            ItemStack Inside = Locker.peekContents();
            return !Inside.isEmpty() && Wanted.test(Inside);
        }
        if (Tile == null) {
            return false;
        }
        IItemHandler Handler = World.getCapability(Capabilities.ItemHandler.BLOCK, Pos, null);
        if (Handler == null) {
            return false;
        }
        for (int Slot = 0; Slot < Handler.getSlots(); Slot++) {
            ItemStack Inside = Handler.getStackInSlot(Slot);
            if (!Inside.isEmpty() && Wanted.test(Inside)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack takeBy(Entity Raider, BlockPos Pos, Predicate<ItemStack> Wanted) {
        if (!(Raider.level() instanceof ServerLevel World) || !RaidAccess.touch(Raider, Pos)) {
            return ItemStack.EMPTY;
        }
        return take(World, Pos, Wanted);
    }

    public static ItemStack take(ServerLevel World, BlockPos Pos, Predicate<ItemStack> Wanted) {
        if (!World.isLoaded(Pos)) {
            return ItemStack.EMPTY;
        }
        BlockEntity Tile = World.getBlockEntity(Pos);
        if (Tile instanceof StorageContainerBlockEntity Locker) {
            ItemStack Inside = Locker.peekContents();
            return !Inside.isEmpty() && Wanted.test(Inside) ? Locker.release() : ItemStack.EMPTY;
        }
        if (Tile == null) {
            return ItemStack.EMPTY;
        }
        IItemHandler Handler = World.getCapability(Capabilities.ItemHandler.BLOCK, Pos, null);
        if (Handler == null) {
            return ItemStack.EMPTY;
        }
        for (int Slot = 0; Slot < Handler.getSlots(); Slot++) {
            ItemStack Inside = Handler.getStackInSlot(Slot);
            if (!Inside.isEmpty() && Wanted.test(Inside)) {
                ItemStack Taken = Handler.extractItem(Slot, Inside.getCount(), false);
                if (!Taken.isEmpty()) {
                    return Taken;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static List<BlockPos> nearby(ServerLevel World, BlockPos Centre, int Radius) {
        List<BlockPos> Found = new ArrayList<>();
        int MinX = (Centre.getX() - Radius) >> 4;
        int MaxX = (Centre.getX() + Radius) >> 4;
        int MinZ = (Centre.getZ() - Radius) >> 4;
        int MaxZ = (Centre.getZ() + Radius) >> 4;
        double Square = (double) Radius * Radius;
        for (int ChunkX = MinX; ChunkX <= MaxX; ChunkX++) {
            for (int ChunkZ = MinZ; ChunkZ <= MaxZ; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Tile : Chunk.getBlockEntities().values()) {
                    BlockPos Pos = Tile.getBlockPos();
                    if (Pos.distSqr(Centre) <= Square) {
                        Found.add(Pos);
                    }
                }
            }
        }
        return Found;
    }
}
