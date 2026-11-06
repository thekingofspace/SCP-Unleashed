package net.rai.scp_unleashed.scp001day.sun;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class Scp001DayLevelField implements Scp001DayRay.Field {
    private final Level World;
    private final int MinY;
    private final int MaxY;
    private int ChunkX = Integer.MIN_VALUE;
    private int ChunkZ = Integer.MIN_VALUE;
    private LevelChunk Chunk;
    private int Lookups;

    public Scp001DayLevelField(Level World) {
        this.World = World;
        this.MinY = World.getMinBuildHeight();
        this.MaxY = World.getMaxBuildHeight();
    }

    public int lookups() {
        return Lookups;
    }

    @Override
    public int cell(int X, int Y, int Z) {
        Lookups++;
        if (Y >= MaxY) {
            return Scp001DayRay.Clear;
        }
        if (Y < MinY) {
            return Scp001DayRay.Solid;
        }
        int Cx = X >> 4;
        int Cz = Z >> 4;
        if (Cx != ChunkX || Cz != ChunkZ) {
            ChunkX = Cx;
            ChunkZ = Cz;
            Chunk = World.getChunkSource().getChunkNow(Cx, Cz);
        }
        if (Chunk == null) {
            return Scp001DayRay.Clear;
        }
        LevelChunkSection Section = Chunk.getSection(Chunk.getSectionIndex(Y));
        if (Section.hasOnlyAir()) {
            return Scp001DayRay.Clear;
        }
        return Scp001DayShade.cell(Section.getBlockState(X & 15, Y & 15, Z & 15));
    }
}
