package net.rai.scp_unleashed.dclass.client;

import net.rai.scp_unleashed.dclass.HumanoidPose;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.function.Function;

public final class PuppetPoser {
    public static final String[] Frames = {"root", "waist"};
    public static final String[] Attached = {"head", "right_arm", "left_arm"};
    public static final int[] AttachedParts = {HumanoidPose.Head, HumanoidPose.RightArm, HumanoidPose.LeftArm};
    public static final String[] Grounded = {"right_leg", "left_leg"};
    public static final int[] GroundedParts = {HumanoidPose.RightLeg, HumanoidPose.LeftLeg};
    public static final float Unit = 16.0f;

    private PuppetPoser() {
    }

    public static void apply(Function<String, GeoBone> Lookup, HumanoidPose Pose) {
        for (String Name : Frames) {
            GeoBone Frame = Lookup.apply(Name);
            if (Frame != null) {
                Frame.setRotX(0.0f);
                Frame.setRotY(0.0f);
                Frame.setRotZ(0.0f);
                Frame.setPosX(0.0f);
                Frame.setPosY(0.0f);
                Frame.setPosZ(0.0f);
                Frame.setScaleX(1.0f);
                Frame.setScaleY(1.0f);
                Frame.setScaleZ(1.0f);
            }
        }
        GeoBone Body = Lookup.apply("body");
        if (Body == null) {
            return;
        }
        direct(Body, Pose, HumanoidPose.Body);
        for (int Index = 0; Index < Grounded.length; Index++) {
            GeoBone Leg = Lookup.apply(Grounded[Index]);
            if (Leg != null) {
                direct(Leg, Pose, GroundedParts[Index]);
            }
        }
        Matrix4f Carrier = frame(Body, Pose, HumanoidPose.Body).invert();
        for (int Index = 0; Index < Attached.length; Index++) {
            GeoBone Child = Lookup.apply(Attached[Index]);
            if (Child != null) {
                attached(Child, Carrier, Pose, AttachedParts[Index]);
            }
        }
    }

    private static void direct(GeoBone Bone, HumanoidPose Pose, int Part) {
        Bone.setRotX(-Pose.get(Part, HumanoidPose.TurnX));
        Bone.setRotY(-Pose.get(Part, HumanoidPose.TurnY));
        Bone.setRotZ(Pose.get(Part, HumanoidPose.TurnZ));
        Bone.setPosX(Pose.get(Part, HumanoidPose.OffsetX));
        Bone.setPosY(-Pose.get(Part, HumanoidPose.OffsetY));
        Bone.setPosZ(Pose.get(Part, HumanoidPose.OffsetZ));
        stretch(Bone, Pose, Part);
    }

    private static void attached(GeoBone Bone, Matrix4f Carrier, HumanoidPose Pose, int Part) {
        Vector3f Pivot = pivot(Bone);
        Matrix4f Local = new Matrix4f(Carrier).mul(frame(Bone, Pose, Part));
        Vector3f Turn = Local.getEulerAnglesZYX(new Vector3f());
        Vector3f Moved = Local.transformPosition(new Vector3f(Pivot)).sub(Pivot);
        Bone.setRotX(Turn.x);
        Bone.setRotY(Turn.y);
        Bone.setRotZ(Turn.z);
        Bone.setPosX(-Moved.x * Unit);
        Bone.setPosY(Moved.y * Unit);
        Bone.setPosZ(Moved.z * Unit);
        stretch(Bone, Pose, Part);
    }

    private static void stretch(GeoBone Bone, HumanoidPose Pose, int Part) {
        Bone.setScaleX(Pose.get(Part, HumanoidPose.StretchX));
        Bone.setScaleY(Pose.get(Part, HumanoidPose.StretchY));
        Bone.setScaleZ(Pose.get(Part, HumanoidPose.StretchZ));
    }

    private static Vector3f pivot(GeoBone Bone) {
        return new Vector3f(Bone.getPivotX(), Bone.getPivotY(), Bone.getPivotZ()).div(Unit);
    }

    private static Matrix4f frame(GeoBone Bone, HumanoidPose Pose, int Part) {
        Vector3f Pivot = pivot(Bone);
        Vector3f Shift = new Vector3f(-Pose.get(Part, HumanoidPose.OffsetX), -Pose.get(Part, HumanoidPose.OffsetY), Pose.get(Part, HumanoidPose.OffsetZ)).div(Unit);
        Quaternionf Turn = new Quaternionf().rotationZYX(Pose.get(Part, HumanoidPose.TurnZ), -Pose.get(Part, HumanoidPose.TurnY), -Pose.get(Part, HumanoidPose.TurnX));
        return new Matrix4f().translate(Shift).translate(Pivot).rotate(Turn).translate(-Pivot.x, -Pivot.y, -Pivot.z);
    }
}
