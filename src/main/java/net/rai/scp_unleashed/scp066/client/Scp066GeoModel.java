package net.rai.scp_unleashed.scp066.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp066.Scp066Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

public class Scp066GeoModel extends GeoModel<Scp066Entity> {
    public static final String JitterBone = "jitter";
    public static final String SpinBone = "spin";
    public static final String[] SpinTails = {"tail_b", "tail_c"};
    public static final float TailTilt = 25.0f;
    public static final float TailHide = 0.05f;
    public static final String EyePrefix = "eye_";
    public static final float ShakeLimit = 1.6f;
    public static final float LookYaw = 0.6f;
    public static final float LookPitch = 0.4f;
    public static final float BlinkSquash = 0.2f;
    private static final ResourceLocation ModelPath = location("geo/entity/scp066/scp066.geo.json");
    private static final ResourceLocation AnimationPath = location("animations/entity/scp066/scp066.animation.json");
    private static final Map<String, ResourceLocation> Textures = new HashMap<>();

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static String stage(Scp066Entity Heap) {
        byte Mood = Heap.mood();
        if (Mood == Scp066Entity.Calm) {
            return "calm";
        }
        if (Mood == Scp066Entity.Hostile) {
            return "hostile";
        }
        int Ticks = Heap.changeTicks();
        if (Ticks < 5) {
            return "grey_1";
        }
        if (Ticks < 12) {
            return "grey_2";
        }
        if (Ticks < Scp066Entity.EyesEnd) {
            return "grey";
        }
        if (Ticks < Scp066Entity.EyesEnd + 5) {
            return "red_1";
        }
        if (Ticks < Scp066Entity.EyesEnd + 10) {
            return "red_2";
        }
        return "hostile";
    }

    public static float shake(Scp066Entity Heap, float Partial) {
        if (!Heap.isChanging()) {
            return 0.0f;
        }
        float Ticks = Heap.changeTicks() + Partial;
        if (Ticks < Scp066Entity.GreyTicks) {
            return 0.25f * Ticks / Scp066Entity.GreyTicks;
        }
        if (Ticks < Scp066Entity.ShakeEnd) {
            float Rise = (Ticks - Scp066Entity.GreyTicks) / (Scp066Entity.ShakeEnd - Scp066Entity.GreyTicks);
            return 0.6f + (ShakeLimit - 0.6f) * Rise;
        }
        if (Ticks < Scp066Entity.EyesEnd) {
            return 0.3f;
        }
        return 0.5f * Math.max(0.0f, 1.0f - (Ticks - Scp066Entity.EyesEnd) / (Scp066Entity.ChangeTicks - Scp066Entity.EyesEnd));
    }

    public static float puff(Scp066Entity Heap, float Partial) {
        if (!Heap.isChanging()) {
            return 1.0f;
        }
        float Share = Mth.clamp((Heap.changeTicks() + Partial - Scp066Entity.EyesEnd) / 10.0f, 0.0f, 1.0f);
        return 1.0f + 0.12f * Mth.sin(Share * Mth.PI);
    }

    public static float eyeGrowth(Scp066Entity Heap, int Index, float Partial) {
        if (Heap.isHostile()) {
            return 1.0f;
        }
        if (!Heap.isChanging()) {
            return 0.0f;
        }
        float Share = Mth.clamp((Heap.changeTicks() + Partial - Scp066Entity.ShakeEnd - Index * Scp066Entity.EyeSpacing) / 3.0f, 0.0f, 1.0f);
        return Share + 0.3f * Mth.sin(Share * Mth.PI);
    }

    public static float hashUnit(int First, int Second) {
        int Value = (First * 73856093) ^ (Second * 19349663) ^ 0x5bd1e995;
        Value = ((Value >>> 15) ^ Value) * 0x2C1B3C6D;
        Value = ((Value >>> 12) ^ Value) * 0x297A2D39;
        Value = (Value >>> 15) ^ Value;
        return (Value & 0xFFFF) / 65535.0f;
    }

    public static float[] look(float Time, int Index) {
        int Period = 16 + Index * 5;
        float Shifted = Time + Index * 17;
        int Slot = Mth.floor(Shifted / Period);
        float Share = Math.min(1.0f, (Shifted - Slot * Period) / 2.0f);
        float YawBefore = (hashUnit(Slot - 1, Index) - 0.5f) * LookYaw * 2.0f;
        float PitchBefore = (hashUnit(Slot - 1, Index + 31) - 0.5f) * LookPitch * 2.0f;
        float YawAfter = (hashUnit(Slot, Index) - 0.5f) * LookYaw * 2.0f;
        float PitchAfter = (hashUnit(Slot, Index + 31) - 0.5f) * LookPitch * 2.0f;
        return new float[]{Mth.lerp(Share, YawBefore, YawAfter), Mth.lerp(Share, PitchBefore, PitchAfter)};
    }

    public static boolean blinking(int Ticks, int Index) {
        return Math.floorMod(Ticks + Index * 23, 70 + Index * 9) < 3;
    }

    @Override
    public ResourceLocation getModelResource(Scp066Entity Heap) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp066Entity Heap) {
        return Textures.computeIfAbsent(stage(Heap), Name -> location("textures/entity/scp066/" + Name + ".png"));
    }

    @Override
    public ResourceLocation getAnimationResource(Scp066Entity Heap) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp066Entity Heap, long InstanceId, AnimationState<Scp066Entity> Animated) {
        float Partial = Animated.getPartialTick();
        float Time = Heap.tickCount + Partial;
        GeoBone Jitter = getAnimationProcessor().getBone(JitterBone);
        if (Jitter != null) {
            float Amount = shake(Heap, Partial);
            Jitter.setPosX(Amount * Mth.sin(Time * 5.1f + 1.3f) * Mth.cos(Time * 2.3f));
            Jitter.setPosY(Amount * 0.6f * Mth.sin(Time * 7.7f));
            Jitter.setPosZ(Amount * Mth.sin(Time * 6.3f + 0.4f) * Mth.cos(Time * 1.7f + 0.9f));
            Jitter.setRotZ(Amount * 0.11f * Mth.sin(Time * 4.4f + 0.7f));
            Jitter.setRotX(Amount * 0.09f * Mth.sin(Time * 5.9f));
            float Swell = puff(Heap, Partial);
            Jitter.setScaleX(Swell);
            Jitter.setScaleY(Swell);
            Jitter.setScaleZ(Swell);
        }
        for (int Index = 0; Index < Scp066Entity.EyeCount; Index++) {
            GeoBone Eye = getAnimationProcessor().getBone(EyePrefix + Index);
            if (Eye == null) {
                continue;
            }
            float Grown = eyeGrowth(Heap, Index, Partial);
            Eye.setHidden(Grown <= 0.0f);
            if (Grown <= 0.0f) {
                continue;
            }
            float[] Gaze = look(Time + Heap.getId() * 7, Index);
            Eye.setRotX(Eye.getInitialSnapshot().getRotX() + Gaze[1]);
            Eye.setRotY(Eye.getInitialSnapshot().getRotY() + Gaze[0]);
            float Squash = Heap.isHostile() && blinking(Heap.tickCount + Heap.getId() * 7, Index) ? BlinkSquash : 1.0f;
            Eye.setScaleX(Grown);
            Eye.setScaleY(Grown * Squash);
            Eye.setScaleZ(Grown);
        }
        GeoBone Spin = getAnimationProcessor().getBone(SpinBone);
        if (Spin == null) {
            return;
        }
        Matrix3f Turn = new Matrix3f().set(Heap.roll(Partial, Mth.rotLerp(Partial, Heap.yBodyRotO, Heap.yBodyRot)));
        float Sine = Mth.clamp(-Turn.m02, -1.0f, 1.0f);
        if (Math.abs(Sine) > 0.9999f) {
            Spin.setRotX(0.0f);
            Spin.setRotY((float) Math.asin(Sine));
            Spin.setRotZ((float) Mth.atan2(-Turn.m10, Turn.m11));
        } else {
            Spin.setRotX((float) Mth.atan2(Turn.m12, Turn.m22));
            Spin.setRotY((float) Math.asin(Sine));
            Spin.setRotZ((float) Mth.atan2(Turn.m01, Turn.m00));
        }
        float Tilt = (float) Math.toDegrees(Math.acos(Mth.clamp(Turn.m11, -1.0f, 1.0f)));
        float Wound = Math.max(0.0f, 1.0f - Tilt / TailTilt);
        for (String Name : SpinTails) {
            GeoBone Tail = getAnimationProcessor().getBone(Name);
            if (Tail != null) {
                float Size = Math.max(0.01f, Wound);
                Tail.setHidden(Wound < TailHide);
                Tail.setScaleX(Size);
                Tail.setScaleY(Size);
                Tail.setScaleZ(Size);
            }
        }
        Spin.setPosX(0.0f);
        Spin.setPosZ(0.0f);
        Spin.setPosY(-lowest(Spin, new Matrix4f(), true, Float.MAX_VALUE, new Matrix4f(), new Vector3f()) * 16.0f);
    }

    public static float lowest(GeoBone Bone, Matrix4f Parent, boolean First, float Floor, Matrix4f Shape, Vector3f Point) {
        if (Bone.isHidden()) {
            return Floor;
        }
        Matrix4f Local = new Matrix4f(Parent);
        if (!First) {
            Local.translate(-Bone.getPosX() / 16.0f, Bone.getPosY() / 16.0f, Bone.getPosZ() / 16.0f);
        }
        Local.translate(Bone.getPivotX() / 16.0f, Bone.getPivotY() / 16.0f, Bone.getPivotZ() / 16.0f)
                .rotateZYX(Bone.getRotZ(), Bone.getRotY(), Bone.getRotX())
                .scale(Bone.getScaleX(), Bone.getScaleY(), Bone.getScaleZ())
                .translate(-Bone.getPivotX() / 16.0f, -Bone.getPivotY() / 16.0f, -Bone.getPivotZ() / 16.0f);
        float Result = Floor;
        for (GeoCube Cube : Bone.getCubes()) {
            Vec3 Pivot = Cube.pivot();
            Vec3 Twist = Cube.rotation();
            Shape.set(Local)
                    .translate((float) Pivot.x / 16.0f, (float) Pivot.y / 16.0f, (float) Pivot.z / 16.0f)
                    .rotateZYX((float) Twist.z, (float) Twist.y, (float) Twist.x)
                    .translate((float) -Pivot.x / 16.0f, (float) -Pivot.y / 16.0f, (float) -Pivot.z / 16.0f);
            for (GeoQuad Quad : Cube.quads()) {
                if (Quad == null) {
                    continue;
                }
                for (GeoVertex Vertex : Quad.vertices()) {
                    Shape.transformPosition(Point.set(Vertex.position()));
                    Result = Math.min(Result, Point.y);
                }
            }
        }
        for (GeoBone Child : Bone.getChildBones()) {
            Result = lowest(Child, Local, false, Result, Shape, Point);
        }
        return Result;
    }
}
