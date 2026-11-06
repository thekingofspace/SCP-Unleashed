package net.rai.scp_unleashed.scp001gate;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class Scp001GateBlade {
    public static final String Bone = "sword";
    public static final float PivotX = -12.5f;
    public static final float PivotY = 44.5f;
    public static final float PivotZ = 0.0f;
    public static final float BladeX = -12.5f;
    public static final float BladeZ = 0.0f;
    public static final float GuardY = 37.0f;
    public static final float TipY = -34.5f;
    public static final float HalfWidth = 2.6f;
    public static final float HalfDepth = 0.8f;
    public static final float ModelLift = 0.01f;

    private Scp001GateBlade() {
    }

    public static Matrix4f placement(float Facing, float Scale) {
        return new Matrix4f().scale(Scale).rotateY((180.0f - Facing) * Mth.DEG_TO_RAD).translate(0.0f, ModelLift, 0.0f);
    }

    public static Matrix4f pose(Matrix4fc Placement, Matrix4fc BoneAtPivot) {
        return new Matrix4f(Placement).mul(BoneAtPivot);
    }

    public static Vector3f local(float Share, float Across, float Through) {
        float Clamped = Mth.clamp(Share, 0.0f, 1.0f);
        float Y = GuardY + (TipY - GuardY) * Clamped;
        return new Vector3f(-(BladeX - PivotX + Mth.clamp(Across, -1.0f, 1.0f) * HalfWidth) / 16.0f, (Y - PivotY) / 16.0f,
                (BladeZ - PivotZ + Mth.clamp(Through, -1.0f, 1.0f) * HalfDepth) / 16.0f);
    }

    public static Vec3 point(Matrix4fc Pose, float Share, float Across, float Through) {
        Vector3f At = Pose.transformPosition(local(Share, Across, Through), new Vector3f());
        return new Vec3(At.x, At.y, At.z);
    }

    public static Vec3 guard(Matrix4fc Pose) {
        return point(Pose, 0.0f, 0.0f, 0.0f);
    }

    public static Vec3 tip(Matrix4fc Pose) {
        return point(Pose, 1.0f, 0.0f, 0.0f);
    }

    public static float length(float Scale) {
        return (GuardY - TipY) / 16.0f * Scale;
    }
}
