package net.rai.scp_unleashed.scp179.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class VoxelSphere {
    public static final int[][] Normals = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final float[][][] Corners = {
            {{1, 1, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 0}},
            {{0, 1, 1}, {0, 1, 0}, {0, 0, 0}, {0, 0, 1}},
            {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}},
            {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
            {{1, 1, 1}, {0, 1, 1}, {0, 0, 1}, {1, 0, 1}},
            {{0, 1, 0}, {1, 1, 0}, {1, 0, 0}, {0, 0, 0}}
    };

    private final List<int[]> Faces = new ArrayList<>();
    private final float Radius;

    public VoxelSphere(float Radius) {
        this.Radius = Radius;
        int Reach = (int) Math.ceil(Radius) + 1;
        for (int X = -Reach; X <= Reach; X++) {
            for (int Y = -Reach; Y <= Reach; Y++) {
                for (int Z = -Reach; Z <= Reach; Z++) {
                    if (!inside(X, Y, Z)) {
                        continue;
                    }
                    for (int Side = 0; Side < Normals.length; Side++) {
                        if (!inside(X + Normals[Side][0], Y + Normals[Side][1], Z + Normals[Side][2])) {
                            Faces.add(new int[]{X, Y, Z, Side});
                        }
                    }
                }
            }
        }
    }

    public boolean inside(int X, int Y, int Z) {
        return X * X + Y * Y + Z * Z <= Radius * Radius;
    }

    public List<int[]> faces() {
        return Faces;
    }

    public interface Painter {
        float[] paint(int X, int Y, int Z, int Side);
    }

    public void emit(BufferBuilder Builder, Matrix4f Pose, float Cx, float Cy, float Cz, float Voxel, Painter Paint) {
        for (int[] Face : Faces) {
            float[] Look = Paint.paint(Face[0], Face[1], Face[2], Face[3]);
            float U0 = Look[0];
            float V0 = Look[1];
            float U1 = Look[2];
            float V1 = Look[3];
            float Shade = Look[4];
            float[][] Corner = Corners[Face[3]];
            float Bx = Cx + (Face[0] - 0.5f) * Voxel;
            float By = Cy + (Face[1] - 0.5f) * Voxel;
            float Bz = Cz + (Face[2] - 0.5f) * Voxel;
            SkyScene.vertex(Builder, Pose, Bx + Corner[0][0] * Voxel, By + Corner[0][1] * Voxel, Bz + Corner[0][2] * Voxel, U0, V0, Shade, 1.0f);
            SkyScene.vertex(Builder, Pose, Bx + Corner[3][0] * Voxel, By + Corner[3][1] * Voxel, Bz + Corner[3][2] * Voxel, U0, V1, Shade, 1.0f);
            SkyScene.vertex(Builder, Pose, Bx + Corner[2][0] * Voxel, By + Corner[2][1] * Voxel, Bz + Corner[2][2] * Voxel, U1, V1, Shade, 1.0f);
            SkyScene.vertex(Builder, Pose, Bx + Corner[1][0] * Voxel, By + Corner[1][1] * Voxel, Bz + Corner[1][2] * Voxel, U1, V0, Shade, 1.0f);
        }
    }
}
