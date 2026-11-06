package net.rai.scp_unleashed.storage;

import net.minecraft.util.Mth;

public final class DoorSwing {
    public static final float OpenAngle = 90.0f;
    public static final float Stiffness = 0.065f;
    public static final float Damping = 0.33f;
    public static final float Push = 1.7f;
    public static final float MaxSpeed = 18.0f;
    public static final float StopBounce = 0.25f;
    public static final float SlamBounce = 0.18f;
    public static final float SlamSpeed = 6.0f;
    public static final float StopSpeed = 0.8f;
    public static final float RestSpeed = 1.0f;
    public static final float Snap = 0.15f;

    private float Angle;
    private float Previous;
    private float Velocity;
    private boolean Primed;

    public enum Event {
        None,
        Stopped,
        Slammed,
        Settled
    }

    public void prime(boolean Open) {
        Angle = Open ? OpenAngle : 0.0f;
        Previous = Angle;
        Velocity = 0.0f;
        Primed = true;
    }

    public boolean isPrimed() {
        return Primed;
    }

    public Event step(boolean Open) {
        if (!Primed) {
            prime(Open);
            return Event.None;
        }
        Previous = Angle;
        return Open ? opening() : closing();
    }

    private Event opening() {
        if (Angle >= OpenAngle && Math.abs(Velocity) < Snap) {
            Angle = OpenAngle;
            Velocity = 0.0f;
            return Event.None;
        }
        Velocity += (OpenAngle - Angle) * Stiffness - Velocity * Damping;
        Angle += Velocity;
        if (Angle >= OpenAngle) {
            Angle = OpenAngle;
            if (Velocity > 0.0f) {
                float Impact = Velocity;
                Velocity = -Velocity * StopBounce;
                return Impact >= StopSpeed ? Event.Stopped : Event.None;
            }
        }
        if (Math.abs(OpenAngle - Angle) < Snap && Math.abs(Velocity) < Snap) {
            Angle = OpenAngle;
            Velocity = 0.0f;
        }
        return Event.None;
    }

    private Event closing() {
        if (Angle <= 0.0f && Velocity <= 0.0f) {
            Angle = 0.0f;
            Velocity = 0.0f;
            return Event.None;
        }
        Velocity = Math.max(Velocity - Push, -MaxSpeed);
        Angle += Velocity;
        if (Angle <= 0.0f) {
            Angle = 0.0f;
            float Impact = -Velocity;
            Velocity = Impact * SlamBounce;
            if (Velocity < RestSpeed) {
                Velocity = 0.0f;
            }
            return Impact >= SlamSpeed ? Event.Slammed : Event.Settled;
        }
        return Event.None;
    }

    public float angle(float PartialTick) {
        return Mth.lerp(PartialTick, Previous, Angle);
    }

    public float getAngle() {
        return Angle;
    }

    public boolean isShut() {
        return Angle <= 0.0f && Previous <= 0.0f && Velocity == 0.0f;
    }

    public boolean isMoving() {
        return Velocity != 0.0f || Angle != Previous;
    }
}
