package net.rai.scp_unleashed.z0003;

import net.minecraft.world.entity.Entity;

public final class Z0003Stride {
    public static final float Gain = 4.0f;
    public static final float Response = 0.4f;
    public static final double Teleport = 4.0;

    private double LastX;
    private double LastZ;
    private boolean Known;
    private float Speed;
    private float Position;

    public void step(Entity Walker) {
        double X = Walker.getX();
        double Z = Walker.getZ();
        if (!Known) {
            LastX = X;
            LastZ = Z;
            Known = true;
            return;
        }
        double Moved = Math.sqrt((X - LastX) * (X - LastX) + (Z - LastZ) * (Z - LastZ));
        LastX = X;
        LastZ = Z;
        if (Moved > Teleport) {
            Moved = 0.0;
        }
        float Target = Math.min((float) Moved * Gain, 1.0f);
        Speed += (Target - Speed) * Response;
        Position += Speed;
        if (Position > 4096.0f) {
            Position -= 4096.0f;
        }
    }

    public float speed() {
        return Speed;
    }

    public float position() {
        return Position;
    }
}
