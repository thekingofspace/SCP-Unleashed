package net.rai.scp_unleashed.scp031;

public final class Scp031HeadSpring {
    public static final float Absorb = 0.5f;

    private final float Frequency;
    private final float Damping;
    private final float MaxSpeed;
    private float Velocity;

    public Scp031HeadSpring(float Frequency, float Damping, float MaxSpeed) {
        this.Frequency = Frequency;
        this.Damping = Damping;
        this.MaxSpeed = MaxSpeed;
    }

    public static float wrap(float Degrees) {
        float Wrapped = Degrees % 360.0f;
        if (Wrapped >= 180.0f) {
            Wrapped -= 360.0f;
        }
        if (Wrapped < -180.0f) {
            Wrapped += 360.0f;
        }
        return Wrapped;
    }

    public float step(float Current, float Wanted) {
        float Error = wrap(Wanted - Current);
        Velocity += Frequency * Frequency * Error - 2.0f * Damping * Frequency * Velocity;
        Velocity = Math.max(-MaxSpeed, Math.min(MaxSpeed, Velocity));
        return Current + Velocity;
    }

    public void absorb() {
        Velocity *= Absorb;
    }

    public float velocity() {
        return Velocity;
    }

    public void reset() {
        Velocity = 0.0f;
    }
}
