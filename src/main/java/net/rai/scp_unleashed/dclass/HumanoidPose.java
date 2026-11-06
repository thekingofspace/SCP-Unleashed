package net.rai.scp_unleashed.dclass;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.UseAnim;

public final class HumanoidPose {
    public static final int Head = 0;
    public static final int Body = 1;
    public static final int RightArm = 2;
    public static final int LeftArm = 3;
    public static final int RightLeg = 4;
    public static final int LeftLeg = 5;
    public static final int PartCount = 6;
    public static final int OffsetX = 0;
    public static final int OffsetY = 1;
    public static final int OffsetZ = 2;
    public static final int TurnX = 3;
    public static final int TurnY = 4;
    public static final int TurnZ = 5;
    public static final int StretchX = 6;
    public static final int StretchY = 7;
    public static final int StretchZ = 8;
    public static final int Fields = 9;

    public boolean Using;
    public InteractionHand UseHand = InteractionHand.MAIN_HAND;
    public UseAnim UseKind = UseAnim.NONE;
    public int UseTicks;
    public boolean Sitting;
    public boolean FallFlying;
    public boolean Sleeping;
    public boolean Spinning;
    public boolean Copied;
    public final float[] Parts = new float[PartCount * Fields];

    public void copy(int Part, float X, float Y, float Z, float RotX, float RotY, float RotZ, float ScaleX, float ScaleY, float ScaleZ) {
        int Base = Part * Fields;
        Parts[Base + OffsetX] = X;
        Parts[Base + OffsetY] = Y;
        Parts[Base + OffsetZ] = Z;
        Parts[Base + TurnX] = RotX;
        Parts[Base + TurnY] = RotY;
        Parts[Base + TurnZ] = RotZ;
        Parts[Base + StretchX] = ScaleX;
        Parts[Base + StretchY] = ScaleY;
        Parts[Base + StretchZ] = ScaleZ;
    }

    public float get(int Part, int Field) {
        return Parts[Part * Fields + Field];
    }
}
