package net.rai.scp_unleashed.scp131.client;

import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public final class Scp131Rig {
    public static final String Root = "root";
    public static final String Body = "body";
    public static final String Wheel = "wheel";
    public static final String Socket = "socket";
    public static final String Iris = "iris";
    public static final String Pupil = "pupil";
    public static final String LidTop = "lid_top";
    public static final String LidBottom = "lid_bottom";
    public static final String Lash = "lash";
    public static final String[] Tips = {"tip1", "tip2", "tip3"};
    public static final float HiddenBelow = 0.02f;
    public static final float SquashSpread = 0.6f;
    public static final float WheelUnits = 2.24f;
    public static final float LowerReach = 2.0f;
    public static final float LidMeet = 1.02f;
    public static final float LowerShoulder = 0.5f;
    public static final float TopLidReach = 1.0127f;
    private static final float[][] Hull = {{6.5f, 2.5f}, {5.5f, 4.5f}, {4.5f, 5.5f}, {2.5f, 6.5f}};

    private Scp131Rig() {
    }

    private static GeoBone bone(GeoModel<?> Model, String Name) {
        return Model.getBone(Name).orElse(null);
    }

    private static void rotate(GeoBone Target, float GeoX, float GeoY, float GeoZ) {
        if (Target == null) {
            return;
        }
        BoneSnapshot Rest = Target.getInitialSnapshot();
        Target.setRotX(Rest.getRotX() - GeoX * Mth.DEG_TO_RAD);
        Target.setRotY(Rest.getRotY() - GeoY * Mth.DEG_TO_RAD);
        Target.setRotZ(Rest.getRotZ() + GeoZ * Mth.DEG_TO_RAD);
    }

    private static void move(GeoBone Target, float X, float Y, float Z) {
        if (Target == null) {
            return;
        }
        BoneSnapshot Rest = Target.getInitialSnapshot();
        Target.setPosX(Rest.getOffsetX() + X);
        Target.setPosY(Rest.getOffsetY() + Y);
        Target.setPosZ(Rest.getOffsetZ() + Z);
    }

    private static void scale(GeoBone Target, float X, float Y, float Z) {
        if (Target == null) {
            return;
        }
        Target.setScaleX(X);
        Target.setScaleY(Y);
        Target.setScaleZ(Z);
    }

    private static boolean shutter(GeoBone Target, float Share) {
        boolean Hidden = Share < HiddenBelow;
        if (Target == null) {
            return Hidden;
        }
        Target.setHidden(Hidden);
        scale(Target, 1.0f, Hidden ? 1.0f : Math.min(1.0f, Share), 1.0f);
        return Hidden;
    }

    public static float topLid(float Top, float Lower) {
        return Lower > 0.0f ? Math.min(Top, (LidMeet - Lower * LowerShoulder) * TopLidReach) : Top;
    }

    public static float[] tiltOffset(float Pitch, float Roll) {
        double TurnX = Math.toRadians(-Pitch);
        double TurnZ = Math.toRadians(-Roll);
        double CosX = Math.cos(TurnX);
        double SinX = Math.sin(TurnX);
        double CosZ = Math.cos(TurnZ);
        double SinZ = Math.sin(TurnZ);
        double UpX = -SinZ * CosX;
        double UpZ = SinX;
        if (Math.abs(UpX) + Math.abs(UpZ) < 1.0e-9) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        float[] Best = Hull[0];
        double Reach = Double.NEGATIVE_INFINITY;
        for (float[] Corner : Hull) {
            double Support = Corner[0] * Math.abs(UpX) + Corner[1] * Math.abs(UpZ);
            if (Support > Reach) {
                Reach = Support;
                Best = Corner;
            }
        }
        double PivotX = Math.copySign(Best[0], UpX);
        double PivotZ = Math.copySign(Best[1], UpZ);
        double MovedX = PivotX - (CosZ * PivotX + SinZ * SinX * PivotZ);
        double MovedY = -(SinZ * PivotX - CosZ * SinX * PivotZ);
        double MovedZ = PivotZ - CosX * PivotZ;
        return new float[]{(float) -MovedX, (float) MovedY, (float) MovedZ};
    }

    public static void apply(GeoModel<?> Model, Scp131Pose Pose) {
        GeoBone Whole = bone(Model, Root);
        float[] Offset = tiltOffset(Pose.BodyPitch, Pose.BodyRoll);
        rotate(Whole, Pose.BodyPitch, 0.0f, -Pose.BodyRoll);
        move(Whole, Offset[0], Offset[1] + Pose.Lift, Offset[2]);
        scale(bone(Model, Body), 1.0f + Pose.Squash * SquashSpread, 1.0f - Pose.Squash, 1.0f + Pose.Squash * SquashSpread);
        rotate(bone(Model, Wheel), Pose.Wheel, 0.0f, 0.0f);
        GeoBone Eye = bone(Model, Socket);
        scale(Eye, Pose.Socket, Pose.Socket, Pose.Socket);
        move(bone(Model, Iris), Pose.IrisX, Pose.IrisY, 0.0f);
        scale(bone(Model, Pupil), Pose.Pupil, Pose.Pupil, 1.0f);
        float Top = topLid(Pose.LidTop, Pose.LidBottom);
        boolean Open = shutter(bone(Model, LidTop), Top);
        shutter(bone(Model, LidBottom), Math.min(1.0f, Pose.LidBottom * LowerReach));
        GeoBone Line = bone(Model, Lash);
        if (Line != null) {
            Line.setHidden(Open);
        }
        move(Line, 0.0f, Scp131Face.lashOffset(Top), 0.0f);
        for (int Joint = 0; Joint < Tips.length; Joint++) {
            rotate(bone(Model, Tips[Joint]), -Pose.TipPitch[Joint], 0.0f, Pose.TipRoll[Joint]);
        }
    }
}
