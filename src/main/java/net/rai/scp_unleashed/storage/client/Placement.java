package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public record Placement(float X, float Y, float Z, float Yaw, float Pitch, float Roll, float ScaleX, float ScaleY, float ScaleZ) {
    public static final Placement Origin = new Placement(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);

    public static Placement at(float X, float Y, float Z) {
        return new Placement(X, Y, Z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    }

    public Placement withPosition(float NewX, float NewY, float NewZ) {
        return new Placement(NewX, NewY, NewZ, Yaw, Pitch, Roll, ScaleX, ScaleY, ScaleZ);
    }

    public Placement moved(float Dx, float Dy, float Dz) {
        return new Placement(X + Dx, Y + Dy, Z + Dz, Yaw, Pitch, Roll, ScaleX, ScaleY, ScaleZ);
    }

    public Placement withRotation(float NewYaw, float NewPitch, float NewRoll) {
        return new Placement(X, Y, Z, NewYaw, NewPitch, NewRoll, ScaleX, ScaleY, ScaleZ);
    }

    public Placement withYaw(float NewYaw) {
        return new Placement(X, Y, Z, NewYaw, Pitch, Roll, ScaleX, ScaleY, ScaleZ);
    }

    public Placement withScale(float Scale) {
        return new Placement(X, Y, Z, Yaw, Pitch, Roll, Scale, Scale, Scale);
    }

    public Placement withScale(float NewX, float NewY, float NewZ) {
        return new Placement(X, Y, Z, Yaw, Pitch, Roll, NewX, NewY, NewZ);
    }

    public void apply(PoseStack Pose) {
        Pose.translate(X / 16.0f, Y / 16.0f, Z / 16.0f);
        if (Yaw != 0.0f) {
            Pose.mulPose(Axis.YP.rotationDegrees(Yaw));
        }
        if (Pitch != 0.0f) {
            Pose.mulPose(Axis.XP.rotationDegrees(Pitch));
        }
        if (Roll != 0.0f) {
            Pose.mulPose(Axis.ZP.rotationDegrees(Roll));
        }
        if (ScaleX != 1.0f || ScaleY != 1.0f || ScaleZ != 1.0f) {
            Pose.scale(ScaleX, ScaleY, ScaleZ);
        }
    }
}
