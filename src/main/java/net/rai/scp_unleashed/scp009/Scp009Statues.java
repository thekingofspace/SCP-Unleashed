package net.rai.scp_unleashed.scp009;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public final class Scp009Statues {
    public static final int MaxPerChunk = 6;
    public static final float MaxWidth = 4.5f;
    public static final float MaxHeight = 6.0f;

    private Scp009Statues() {
    }

    public static boolean fits(LivingEntity Dead) {
        return Dead.getBbWidth() <= MaxWidth && Dead.getBbHeight() <= MaxHeight;
    }

    public static void raise(LivingEntity Dead) {
        if (!(Dead.level() instanceof ServerLevel World) || !fits(Dead)) {
            return;
        }
        Scp009Statue Statue = Scp009Statue.of(World, Dead);
        makeRoom(World, Statue.chunkPosition());
        if (World.addFreshEntity(Statue)) {
            Dead.setInvisible(true);
        }
    }

    public static int inChunk(ServerLevel World, ChunkPos Chunk) {
        return column(World, Chunk).size();
    }

    private static List<Scp009Statue> column(ServerLevel World, ChunkPos Chunk) {
        AABB Column = new AABB(Chunk.getMinBlockX(), World.getMinBuildHeight(), Chunk.getMinBlockZ(), Chunk.getMaxBlockX() + 1, World.getMaxBuildHeight(), Chunk.getMaxBlockZ() + 1);
        return World.getEntitiesOfClass(Scp009Statue.class, Column, Scp009Statue::isAlive);
    }

    private static void makeRoom(ServerLevel World, ChunkPos Chunk) {
        List<Scp009Statue> Existing = column(World, Chunk);
        if (Existing.size() < MaxPerChunk) {
            return;
        }
        Existing.sort(Comparator.comparingLong(Scp009Statue::createdAt));
        int Excess = Existing.size() - MaxPerChunk + 1;
        for (int Index = 0; Index < Excess; Index++) {
            Existing.get(Index).crumble();
        }
    }
}
