package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKing;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ScarletKingGeoModel extends SharedGeoModel<ScarletKing> {
    public static final ResourceLocation ModelPath = ScarletKingRegistry.location("geo/entity/scarlet_king/scarlet_king.geo.json");
    public static final ResourceLocation TexturePath = ScarletKingRegistry.location("textures/entity/scarlet_king/scarlet_king.png");
    public static final ResourceLocation AnimationPath = ScarletKingRegistry.location("animations/entity/scarlet_king/scarlet_king.animation.json");
    public static final String[] EyePrefixes = {"eye", "reye", "seye"};
    public static final String[][] Chains = {{"chain_a1", "chain_a1_1", "chain_a1_2", "chain_a1_tip"}, {"chain_a2", "chain_a2_1", "chain_a2_tip"},
            {"chain_b1", "chain_b1_1", "chain_b1_2", "chain_b1_tip"}, {"chain_b2", "chain_b2_1", "chain_b2_tip"}};
    public static final float PupilReach = 1.1f;
    public static final float PupilLift = 0.5f;
    public static final float OpenLid = 0.03f;
    public static final float ModelScale = 3.0f;
    public static final float Floor = 0.15f / 16.0f;
    public static final float Stiffness = 30.0f;
    public static final float Damping = 3.3f;
    public static final float MaxSwing = 75.0f * Mth.DEG_TO_RAD;
    public static final float SubStep = 1.0f / 60.0f;
    public static final float SnapDelta = 0.25f;
    public static final float GutterSpread = 23.0f;
    public static final float GutterTicks = 12.0f;
    private static final Vector3f Down = new Vector3f(0.0f, -1.0f, 0.0f);
    private final Map<ScarletKing, Sway> Swaying = new WeakHashMap<>();

    private static final class Sway {
        final Map<String, Vector3f> Tips = new HashMap<>();
        final Map<String, Vector3f> Speeds = new HashMap<>();
        float Last = Float.NaN;
    }

    @Override
    public ResourceLocation getModelResource(ScarletKing Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(ScarletKing Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(ScarletKing Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(ScarletKing Owner, long InstanceId, AnimationState<ScarletKing> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        Entity Watched = Owner.level().getEntity(Owner.targetId());
        if (Watched == null) {
            Watched = Minecraft.getInstance().player;
        }
        float Across = 0.0f;
        float Lift = 0.0f;
        boolean Dead = Owner.isDeadOrDying();
        if (Watched != null && !Dead) {
            Vec3 Delta = Watched.getEyePosition().subtract(Owner.position().add(0.0, 13.0, 0.0));
            Vec3 Forward = Owner.forward();
            double Right = Delta.x * -Forward.z + Delta.z * Forward.x;
            double Ahead = Delta.x * Forward.x + Delta.z * Forward.z;
            Across = (float) Mth.clamp(Math.atan2(Right, Math.max(1.0, Ahead)) / (Math.PI / 3.0), -1.0, 1.0) * PupilReach;
            Lift = (float) Mth.clamp(Delta.y / 20.0, -1.0, 1.0) * PupilLift;
        }
        float Time = Owner.tickCount + Owner.renderPartial();
        float Dying = Owner.deathTime + Owner.renderPartial();
        int Index = 0;
        for (String Prefix : EyePrefixes) {
            for (int Number = 0; Number < 12; Number++) {
                GeoBone Pupil = getAnimationProcessor().getBone(Prefix + Number + "_pupil");
                GeoBone Lid = getAnimationProcessor().getBone(Prefix + Number + "_lid");
                if (Pupil == null && Lid == null) {
                    continue;
                }
                Index++;
                if (Pupil != null) {
                    Pupil.setPosX(Pupil.getInitialSnapshot().getOffsetX() - Across);
                    Pupil.setPosY(Pupil.getInitialSnapshot().getOffsetY() + Lift);
                }
                if (Lid != null) {
                    float Close;
                    if (Dead) {
                        Close = gutter(Dying, Index);
                    } else {
                        int Period = 90 + Index * 37 % 70;
                        float Phase = (Time + Index * 53) % Period;
                        Close = Phase < 2.0f ? Phase / 2.0f : Phase < 4.0f ? (4.0f - Phase) / 2.0f : 0.0f;
                    }
                    Lid.setScaleY(Math.max(OpenLid, Close));
                }
            }
        }
        emerge(Owner);
        hang(Owner);
    }

    private void emerge(ScarletKing Owner) {
        GeoBone Root = getAnimationProcessor().getBone("root");
        if (Root == null || Root.getInitialSnapshot() == null) {
            return;
        }
        int Action = Owner.isDeadOrDying() ? ScarletKing.Dying : Owner.action();
        if (Action == ScarletKing.Manifest) {
            Root.setPosX(Root.getInitialSnapshot().getOffsetX());
            Root.setPosY(Root.getInitialSnapshot().getOffsetY() + ScarletKing.emergeDepth(Owner.actionAge(Owner.renderPartial()) / 20.0f));
            Root.setPosZ(Root.getInitialSnapshot().getOffsetZ());
        } else if (!ScarletKing.movesRoot(Action)) {
            Root.setPosX(Root.getInitialSnapshot().getOffsetX());
            Root.setPosY(Root.getInitialSnapshot().getOffsetY());
            Root.setPosZ(Root.getInitialSnapshot().getOffsetZ());
        }
    }

    public static float gutter(float Dying, int Index) {
        float Start = ScarletKing.CrownCrackTick + Index * 7 % GutterSpread;
        if (Dying < Start) {
            return 0.0f;
        }
        float Share = Mth.clamp((Dying - Start) / GutterTicks, 0.0f, 1.0f);
        if (Share >= 1.0f) {
            return 1.0f;
        }
        boolean Flicker = Mth.sin(Dying * 2.7f + Index * 1.9f) > 0.55f;
        return Flicker ? 1.0f : Share * 0.6f;
    }

    private void hang(ScarletKing Owner) {
        float Partial = Owner.renderPartial();
        float Now = Owner.tickCount + Partial;
        Sway State = Swaying.computeIfAbsent(Owner, Key -> new Sway());
        float Delta = Float.isNaN(State.Last) ? -1.0f : (Now - State.Last) / 20.0f;
        if (Delta != 0.0f) {
            State.Last = Now;
        }
        float Yaw = Mth.rotLerp(Partial, Owner.yBodyRotO, Owner.yBodyRot);
        Matrix3f Turn = new Matrix3f().rotationY((180.0f - Yaw) * Mth.DEG_TO_RAD);
        Matrix3f Back = new Matrix3f(Turn).transpose();
        Vector3f Offset = new Vector3f((float) Mth.lerp(Partial, Owner.xo, Owner.getX()), (float) Mth.lerp(Partial, Owner.yo, Owner.getY()),
                (float) Mth.lerp(Partial, Owner.zo, Owner.getZ()));
        float FloorWorld = Offset.y + Floor * ModelScale;
        Map<GeoBone, Matrix4f> Cache = new HashMap<>();
        for (String[] Chain : Chains) {
            for (int Level = 0; Level < Chain.length - 1; Level++) {
                GeoBone Bone = getAnimationProcessor().getBone(Chain[Level]);
                GeoBone Child = getAnimationProcessor().getBone(Chain[Level + 1]);
                if (Bone == null || Child == null) {
                    break;
                }
                Matrix4f Parent = matrix(Bone.getParent(), Cache);
                Vector3f Pivot = Parent.transformPosition(new Vector3f(Bone.getPivotX() / 16.0f, Bone.getPivotY() / 16.0f, Bone.getPivotZ() / 16.0f));
                float Length = (Bone.getPivotY() - Child.getPivotY()) / 16.0f;
                boolean Grounded = Pivot.y - Floor < Length;
                Vector3f Aim = aim(Pivot, Length);
                Vector3f World = Turn.transform(new Vector3f(Pivot).mul(ModelScale)).add(Offset);
                float Reach = Length * ModelScale;
                Vector3f Goal = Turn.transform(new Vector3f(Aim)).mul(Reach).add(World);
                String Key = Chain[Level];
                Vector3f Tip = State.Tips.get(Key);
                Vector3f Speed = State.Speeds.get(Key);
                if (Tip == null || Speed == null || Delta < 0.0f || Delta > SnapDelta) {
                    Tip = new Vector3f(Goal);
                    Speed = new Vector3f();
                } else if (Delta > 0.0f) {
                    int Steps = Math.max(1, Mth.ceil(Delta / SubStep));
                    float Small = Delta / Steps;
                    for (int Step = 0; Step < Steps; Step++) {
                        Vector3f Pull = new Vector3f(Goal).sub(Tip).mul(Stiffness).sub(new Vector3f(Speed).mul(Damping));
                        Speed.add(Pull.mul(Small));
                        Tip.add(new Vector3f(Speed).mul(Small));
                    }
                }
                if (Tip.y < FloorWorld) {
                    Tip.y = FloorWorld;
                    Speed.y = Math.max(0.0f, Speed.y);
                }
                Vector3f Way = new Vector3f(Tip).sub(World);
                if (Way.lengthSquared() < 1.0E-12f) {
                    Way.set(Turn.transform(new Vector3f(Aim)));
                }
                Way.normalize();
                Vector3f Local = Back.transform(new Vector3f(Way));
                float Swing = (float) Math.acos(Mth.clamp(-Local.y, -1.0f, 1.0f));
                if (Swing > MaxSwing && !Grounded) {
                    float Flat = (float) Math.sqrt(Local.x * Local.x + Local.z * Local.z);
                    float Fx = Flat > 1.0E-6f ? Local.x / Flat : 1.0f;
                    float Fz = Flat > 1.0E-6f ? Local.z / Flat : 0.0f;
                    Local.set(Fx * Mth.sin(MaxSwing), -Mth.cos(MaxSwing), Fz * Mth.sin(MaxSwing));
                    Way.set(Turn.transform(new Vector3f(Local)));
                }
                State.Tips.put(Key, new Vector3f(Way).mul(Reach).add(World));
                State.Speeds.put(Key, Speed);
                Matrix3f Rotation = orthonormal(Parent).transpose().mul(align(Local));
                Bone.setRotX((float) Math.atan2(Rotation.m12(), Rotation.m22()));
                Bone.setRotY((float) Math.asin(Mth.clamp(-Rotation.m02(), -1.0f, 1.0f)));
                Bone.setRotZ((float) Math.atan2(Rotation.m01(), Rotation.m00()));
                Cache.remove(Bone);
            }
        }
    }

    private static Vector3f aim(Vector3f Pivot, float Length) {
        float Room = Pivot.y - Floor;
        if (Room >= Length) {
            return new Vector3f(Down);
        }
        float Size = (float) Math.sqrt(Pivot.x * Pivot.x + Pivot.z * Pivot.z);
        float Ox = Size > 1.0E-6f ? Pivot.x / Size : 1.0f;
        float Oz = Size > 1.0E-6f ? Pivot.z / Size : 0.0f;
        float Lean = Room > 0.0f ? (float) Math.acos(Mth.clamp(Room / Length, 0.0f, 1.0f)) : Mth.HALF_PI;
        return new Vector3f(Ox * Mth.sin(Lean), -Mth.cos(Lean), Oz * Mth.sin(Lean));
    }

    private static Matrix3f align(Vector3f Aim) {
        Vector3f Axis = new Vector3f(Down).cross(Aim);
        float Sine = Axis.length();
        float Cosine = Down.dot(Aim);
        if (Sine < 1.0E-7f) {
            return Cosine > 0.0f ? new Matrix3f() : new Matrix3f().scaling(1.0f, -1.0f, -1.0f);
        }
        return new Matrix3f().rotation((float) Math.atan2(Sine, Cosine), Axis.div(Sine));
    }

    private static Matrix3f orthonormal(Matrix4f Source) {
        Matrix3f Frame = new Matrix3f();
        Source.get3x3(Frame);
        Vector3f Column = new Vector3f();
        for (int Index = 0; Index < 3; Index++) {
            Frame.getColumn(Index, Column);
            float Size = Column.length();
            if (Size > 1.0E-6f) {
                Frame.setColumn(Index, Column.div(Size));
            } else {
                Frame.setColumn(Index, Index == 0 ? 1.0f : 0.0f, Index == 1 ? 1.0f : 0.0f, Index == 2 ? 1.0f : 0.0f);
            }
        }
        return Frame;
    }

    private static Matrix4f matrix(GeoBone Bone, Map<GeoBone, Matrix4f> Cache) {
        if (Bone == null) {
            return new Matrix4f();
        }
        Matrix4f Known = Cache.get(Bone);
        if (Known != null) {
            return new Matrix4f(Known);
        }
        Matrix4f Made = matrix(Bone.getParent(), Cache);
        float Px = Bone.getPivotX() / 16.0f;
        float Py = Bone.getPivotY() / 16.0f;
        float Pz = Bone.getPivotZ() / 16.0f;
        Made.translate(-Bone.getPosX() / 16.0f, Bone.getPosY() / 16.0f, Bone.getPosZ() / 16.0f).translate(Px, Py, Pz).rotateZ(Bone.getRotZ()).rotateY(Bone.getRotY())
                .rotateX(Bone.getRotX()).scale(Bone.getScaleX(), Bone.getScaleY(), Bone.getScaleZ()).translate(-Px, -Py, -Pz);
        Cache.put(Bone, new Matrix4f(Made));
        return Made;
    }
}
