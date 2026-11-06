package net.rai.scp_unleashed.scp131.client;

public final class Scp131Pose {
    public static final int Joints = 3;
    public float IrisX;
    public float IrisY;
    public float Pupil = 1.0f;
    public float LidTop = Scp131Face.LidRest;
    public float LidBottom;
    public float Socket = 1.0f;
    public float BodyPitch;
    public float BodyRoll;
    public float Squash;
    public float Lift;
    public final float[] TipPitch = new float[Joints];
    public final float[] TipRoll = new float[Joints];
    public float Wheel;

    public void reset() {
        IrisX = 0.0f;
        IrisY = 0.0f;
        Pupil = 1.0f;
        LidTop = Scp131Face.LidRest;
        LidBottom = 0.0f;
        Socket = 1.0f;
        BodyPitch = 0.0f;
        BodyRoll = 0.0f;
        Squash = 0.0f;
        Lift = 0.0f;
        for (int Joint = 0; Joint < Joints; Joint++) {
            TipPitch[Joint] = 0.0f;
            TipRoll[Joint] = 0.0f;
        }
        Wheel = 0.0f;
    }
}
