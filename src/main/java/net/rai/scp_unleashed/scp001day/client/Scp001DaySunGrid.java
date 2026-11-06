package net.rai.scp_unleashed.scp001day.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayGridStore;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayRay;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayShade;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySunUpload;
import net.rai.scp_unleashed.scp001day.sun.SunVector;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

public final class Scp001DaySunGrid {
    public static final int Span = Scp001DayGridStore.Span;
    public static final int Coarse = Scp001DayGridStore.Coarse;
    public static final int CoarseSpan = Scp001DayGridStore.CoarseSpan;
    public static final long BuildBudget = 2000000L;
    public static final Scp001DayGridStore.Memory Native = new Scp001DayGridStore.Memory() {
        @Override
        public ByteBuffer alloc(int Bytes) {
            return MemoryUtil.memAlloc(Bytes);
        }

        @Override
        public ByteBuffer calloc(int Bytes) {
            return MemoryUtil.memCalloc(Bytes);
        }

        @Override
        public void free(ByteBuffer Buffer) {
            MemoryUtil.memFree(Buffer);
        }
    };

    private static final Scp001DayGridStore Store = new Scp001DayGridStore(Native);
    private static int GridTexture;
    private static int CoarseTexture;

    private Scp001DaySunGrid() {
    }

    private record LevelCells(ClientLevel World, int MinY, int MaxY) implements Scp001DayGridStore.Cells {
        @Override
        public boolean column(int X, int Z, int Y0, int[] Out) {
            LevelChunk Chunk = World.getChunkSource().getChunkNow(X >> 4, Z >> 4);
            for (int Ly = 0; Ly < Out.length; Ly++) {
                int Y = Y0 + Ly;
                if (Y >= MaxY || Chunk == null) {
                    Out[Ly] = Scp001DayRay.Clear;
                } else if (Y < MinY) {
                    Out[Ly] = Scp001DayRay.Solid;
                } else {
                    LevelChunkSection Section = Chunk.getSection(Chunk.getSectionIndex(Y));
                    Out[Ly] = Section.hasOnlyAir() ? Scp001DayRay.Clear : Scp001DayShade.cell(Section.getBlockState(X & 15, Y & 15, Z & 15));
                }
            }
            return Chunk != null;
        }

        @Override
        public int cell(int X, int Y, int Z) {
            return Scp001DayShade.cell(World.getBlockState(new BlockPos(X, Y, Z)));
        }
    }

    public static boolean ready() {
        return Store.ready() && GridTexture != 0;
    }

    public static int progress() {
        return Store.progress();
    }

    public static int loaded() {
        return Store.loaded();
    }

    public static int version() {
        return Store.version();
    }

    public static int gridTexture() {
        return GridTexture;
    }

    public static int coarseTexture() {
        return CoarseTexture;
    }

    public static int originX() {
        return Store.originX();
    }

    public static int originY() {
        return Store.originY();
    }

    public static int originZ() {
        return Store.originZ();
    }

    public static boolean holding() {
        return GridTexture != 0 || CoarseTexture != 0 || Store.building() || Store.ready();
    }

    public static void invalidate(int ChunkX, int ChunkZ) {
        Store.invalidate(ChunkX, ChunkZ);
    }

    public static void markDirty(BlockPos Pos) {
        Store.markDirty(Pos.asLong());
    }

    public static void update(ClientLevel World, Vec3 Camera, SunVector Sun) {
        RenderSystem.assertOnRenderThread();
        ensureTextures();
        if (GridTexture == 0 || CoarseTexture == 0) {
            return;
        }
        LevelCells Source = new LevelCells(World, World.getMinBuildHeight(), World.getMaxBuildHeight());
        if (Store.step(World.dimension(), Camera.x, Camera.y, Camera.z, Sun, Source, System.nanoTime() + BuildBudget, Integer.MAX_VALUE)) {
            upload();
        }
    }

    private static void upload() {
        ByteBuffer Live = Store.live();
        ByteBuffer CoarseData = Store.coarse();
        if (Live == null || CoarseData == null) {
            return;
        }
        if (!Scp001DaySunUpload.fits(Scp001DaySunUpload.Grid, Scp001DaySunUpload.Clean, Live.capacity())
                || !Scp001DaySunUpload.fits(Scp001DaySunUpload.Coarse, Scp001DaySunUpload.Clean, CoarseData.capacity())) {
            return;
        }
        Scp001DayGl.unpacked(GL12C.GL_TEXTURE_3D, GridTexture, () -> {
            Live.position(0);
            GL12C.glTexSubImage3D(GL12C.GL_TEXTURE_3D, 0, 0, 0, 0, Span, Span, Span, GL30C.GL_RG_INTEGER, GL11C.GL_UNSIGNED_BYTE, Live);
        });
        Scp001DayGl.unpacked(GL12C.GL_TEXTURE_3D, CoarseTexture, () -> {
            CoarseData.position(0);
            GL12C.glTexSubImage3D(GL12C.GL_TEXTURE_3D, 0, 0, 0, 0, CoarseSpan, CoarseSpan, CoarseSpan, GL30C.GL_RED_INTEGER, GL11C.GL_UNSIGNED_BYTE, CoarseData);
        });
    }

    private static void ensureTextures() {
        if (GridTexture == 0) {
            GridTexture = makeTexture(Span, GL30C.GL_RG8UI, GL30C.GL_RG_INTEGER);
        }
        if (CoarseTexture == 0) {
            CoarseTexture = makeTexture(CoarseSpan, GL30C.GL_R8UI, GL30C.GL_RED_INTEGER);
        }
    }

    private static int makeTexture(int Size, int Internal, int Format) {
        int Id = GL11C.glGenTextures();
        if (Id == 0) {
            return 0;
        }
        Scp001DayGl.unpacked(GL12C.GL_TEXTURE_3D, Id, () -> {
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_WRAP_R, GL12C.GL_CLAMP_TO_EDGE);
            GL12C.glTexImage3D(GL12C.GL_TEXTURE_3D, 0, Internal, Size, Size, Size, 0, Format, GL11C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        });
        return Id;
    }

    public static void release() {
        RenderSystem.assertOnRenderThreadOrInit();
        Store.release();
        if (GridTexture != 0) {
            GL11C.glDeleteTextures(GridTexture);
            GridTexture = 0;
        }
        if (CoarseTexture != 0) {
            GL11C.glDeleteTextures(CoarseTexture);
            CoarseTexture = 0;
        }
    }
}
