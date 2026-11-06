package net.rai.scp_unleashed.scp009;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class Scp009Breaches {
    public static final int DiscoveryInterval = 20;
    public static final double DiscoveryRadius = 16.0;
    public static final int Offset = 7;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> Known = new HashMap<>();

    private Scp009Breaches() {
    }

    public static boolean isBreached(Level World, BlockPos Pos) {
        return World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof Container Box && Box.hasAnyMatching(Scp009CrystalItem::is);
    }

    public static void discover(ServerPlayer User) {
        if (Math.floorMod(User.tickCount + User.getId(), DiscoveryInterval) != 0 || User.isSpectator()) {
            return;
        }
        ServerLevel World = User.serverLevel();
        Vec3 Centre = User.position();
        int Reach = (int) Math.ceil(DiscoveryRadius);
        double ReachSquared = DiscoveryRadius * DiscoveryRadius;
        int MinX = SectionPos.blockToSectionCoord(Centre.x - Reach);
        int MaxX = SectionPos.blockToSectionCoord(Centre.x + Reach);
        int MinZ = SectionPos.blockToSectionCoord(Centre.z - Reach);
        int MaxZ = SectionPos.blockToSectionCoord(Centre.z + Reach);
        Set<BlockPos> Found = null;
        for (int ChunkX = MinX; ChunkX <= MaxX; ChunkX++) {
            for (int ChunkZ = MinZ; ChunkZ <= MaxZ; ChunkZ++) {
                if (!World.hasChunk(ChunkX, ChunkZ)) {
                    continue;
                }
                LevelChunk Chunk = World.getChunk(ChunkX, ChunkZ);
                for (BlockEntity Candidate : Chunk.getBlockEntities().values()) {
                    if (!(Candidate instanceof Container Box)) {
                        continue;
                    }
                    BlockPos Own = Candidate.getBlockPos();
                    if (Vec3.atCenterOf(Own).distanceToSqr(Centre) > ReachSquared || !Box.hasAnyMatching(Scp009CrystalItem::is)) {
                        continue;
                    }
                    if (Found == null) {
                        Found = Known.computeIfAbsent(World.dimension(), Key -> new HashSet<>());
                    }
                    BlockPos Fixed = Own.immutable();
                    if (Found.add(Fixed)) {
                        Scp009Exposure.expose(World, Vec3.atCenterOf(Fixed), Fixed::equals);
                    }
                }
            }
        }
    }

    public static void tick(ServerLevel World) {
        Set<BlockPos> Breaches = Known.get(World.dimension());
        if (Breaches == null || Breaches.isEmpty() || Math.floorMod(World.getGameTime() + Offset, (long) Scp009Exposure.Interval) != 0L) {
            return;
        }
        Iterator<BlockPos> Each = Breaches.iterator();
        while (Each.hasNext()) {
            BlockPos Pos = Each.next();
            if (!isBreached(World, Pos)) {
                Each.remove();
                continue;
            }
            Scp009Exposure.expose(World, Vec3.atCenterOf(Pos), Pos::equals);
        }
    }

    public static int count() {
        int Total = 0;
        for (Set<BlockPos> Breaches : Known.values()) {
            Total += Breaches.size();
        }
        return Total;
    }

    public static void clear() {
        Known.clear();
    }
}
