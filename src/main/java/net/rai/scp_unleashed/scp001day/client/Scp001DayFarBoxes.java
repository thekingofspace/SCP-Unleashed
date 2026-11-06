package net.rai.scp_unleashed.scp001day.client;

import java.nio.FloatBuffer;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySunUpload;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

public final class Scp001DayFarBoxes {
    public static final int Span = Scp001DaySunUpload.FarSpan;
    private static int Texture;
    private static FloatBuffer Data;
    private static int KnownCellX = Integer.MIN_VALUE;
    private static int KnownCellZ = Integer.MIN_VALUE;
    private static int KnownOriginX = Integer.MIN_VALUE;
    private static int KnownOriginY = Integer.MIN_VALUE;
    private static int KnownOriginZ = Integer.MIN_VALUE;
    private static int FirstLocalX;
    private static int FirstLocalZ;

    private Scp001DayFarBoxes() {
    }

    public static int texture() {
        return Texture;
    }

    public static int firstX() {
        return FirstLocalX;
    }

    public static int firstZ() {
        return FirstLocalZ;
    }

    public static float shiftX(int OriginX) {
        return Math.floorMod(OriginX, Scp001DayPlan.Cell);
    }

    public static float shiftZ(int OriginZ) {
        return Math.floorMod(OriginZ, Scp001DayPlan.Cell);
    }

    public static void update(double CameraX, double CameraZ, int OriginX, int OriginY, int OriginZ) {
        int CellX = Math.floorDiv((int) Math.floor(CameraX), Scp001DayPlan.Cell);
        int CellZ = Math.floorDiv((int) Math.floor(CameraZ), Scp001DayPlan.Cell);
        if (Texture != 0 && CellX == KnownCellX && CellZ == KnownCellZ && OriginX == KnownOriginX && OriginY == KnownOriginY && OriginZ == KnownOriginZ) {
            return;
        }
        KnownCellX = CellX;
        KnownCellZ = CellZ;
        KnownOriginX = OriginX;
        KnownOriginY = OriginY;
        KnownOriginZ = OriginZ;
        int FirstX = CellX - Span / 2;
        int FirstZ = CellZ - Span / 2;
        FirstLocalX = FirstX - Math.floorDiv(OriginX, Scp001DayPlan.Cell);
        FirstLocalZ = FirstZ - Math.floorDiv(OriginZ, Scp001DayPlan.Cell);
        if (Data == null) {
            Data = MemoryUtil.memAllocFloat(Span * Span * 8);
        }
        Data.clear();
        for (int Iz = 0; Iz < Span; Iz++) {
            for (int Ix = 0; Ix < Span; Ix++) {
                Scp001DayPlan.Tower Here = Scp001DayPlan.tower(FirstX + Ix, FirstZ + Iz);
                Data.put(Here.MinX() - OriginX).put(Here.MinZ() - OriginZ).put(Here.MaxX() + 1 - OriginX).put(Here.MaxZ() + 1 - OriginZ);
                Data.put(Scp001DayPlan.StreetY - OriginY).put(Here.solidTop() + 1 - OriginY).put(0.0f).put(0.0f);
            }
        }
        Data.flip();
        if (!Scp001DaySunUpload.fits(Scp001DaySunUpload.Far, Scp001DaySunUpload.Clean, (long) Data.remaining() * Float.BYTES)) {
            return;
        }
        boolean Fresh = Texture == 0;
        if (Fresh) {
            Texture = GL11C.glGenTextures();
            if (Texture == 0) {
                return;
            }
        }
        FloatBuffer Upload = Data;
        Scp001DayGl.unpacked(GL11C.GL_TEXTURE_2D, Texture, () -> {
            if (Fresh) {
                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
            }
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA32F, Span * 2, Span, 0, GL11C.GL_RGBA, GL11C.GL_FLOAT, Upload);
        });
    }

    public static void release() {
        if (Texture != 0) {
            GL11C.glDeleteTextures(Texture);
            Texture = 0;
        }
        if (Data != null) {
            MemoryUtil.memFree(Data);
            Data = null;
        }
        KnownCellX = Integer.MIN_VALUE;
    }
}
