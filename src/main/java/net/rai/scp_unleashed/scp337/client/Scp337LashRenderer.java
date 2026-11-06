package net.rai.scp_unleashed.scp337.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp337.Scp337Lash;
import net.rai.scp_unleashed.scp337.Scp337Mass;
import net.rai.scp_unleashed.scp337.Scp337Quarry;
import net.rai.scp_unleashed.scp337.Scp337Registry;

import java.util.List;

public final class Scp337LashRenderer {
    public static final ResourceLocation Texture = Scp337Registry.location("textures/entity/scp337/lash.png");
    private static final Vec3 Up = new Vec3(0.0, 1.0, 0.0);

    private Scp337LashRenderer() {
    }

    public static void render(Scp337Mass Mass, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        List<Scp337Lash> Lashes = Mass.clientLashes();
        if (Lashes.isEmpty() || Mass.isDeadOrDying()) {
            return;
        }
        Level World = Mass.level();
        Vec3 Base = new Vec3(Mth.lerp(Partial, Mass.xo, Mass.getX()), Mth.lerp(Partial, Mass.yo, Mass.getY()), Mth.lerp(Partial, Mass.zo, Mass.getZ()));
        double Now = Mass.serverTime(Partial);
        for (Scp337Lash Lash : Lashes) {
            double Time = Now - Lash.Start();
            if (Time <= 0.0 || Time >= Scp337Lash.Ticks) {
                continue;
            }
            Vec3 End = Lash.Aim();
            Entity Found = World.getEntity(Lash.Target());
            if (Lash.Real() && Found instanceof LivingEntity Target && Target.isAlive()) {
                Vec3 Body = Scp337Quarry.bodyPoint(Target);
                Vec3 Moved = new Vec3(Mth.lerp(Partial, Target.xo, Target.getX()), Body.y - Target.getY() + Mth.lerp(Partial, Target.yo, Target.getY()), Mth.lerp(Partial, Target.zo, Target.getZ()));
                End = Moved.distanceTo(End) < 3.0 ? Moved : End;
            }
            Vec3 From = Scp337Lash.origin(Base, End, Lash.Seed());
            HitResult Hit = World.clip(new ClipContext(From, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Mass));
            if (Hit.getType() != HitResult.Type.MISS) {
                End = Hit.getLocation();
            }
            Vec3 Middle = From.add(End).scale(0.5);
            int Here = LevelRenderer.getLightColor(World, BlockPos.containing(Middle));
            int Light = LightTexture.pack(Math.max(LightTexture.block(Here), LightTexture.block(PackedLight)), Math.max(LightTexture.sky(Here), LightTexture.sky(PackedLight)));
            tube(Buffers.getBuffer(RenderType.entityCutoutNoCull(Texture)), Pose, Base, From, End, Time, Lash.Seed(), Light);
        }
    }

    private static void tube(VertexConsumer Buffer, PoseStack Pose, Vec3 Base, Vec3 From, Vec3 End, double Time, long Seed, int Light) {
        int Count = Scp337Lash.Segments;
        Vec3[] Points = new Vec3[Count + 1];
        for (int Index = 0; Index <= Count; Index++) {
            Points[Index] = Scp337Lash.point(From, End, Time, (double) Index / Count, Seed).subtract(Base);
        }
        Vec3[][] Frame = frames(Points, End.subtract(From));
        strand(Buffer, Pose, Points, Frame, 1.0, Light);
        for (int Strand = 1; Strand <= Scp337Lash.Frays; Strand++) {
            Vec3[] Shifted = new Vec3[Count + 1];
            for (int Index = 0; Index <= Count; Index++) {
                double Along = (double) Index / Count;
                double Angle = Scp337Lash.frayAngle(Strand, Along, Seed);
                double Reach = Scp337Lash.frayReach(Along);
                Shifted[Index] = Points[Index].add(Frame[0][Index].scale(Math.cos(Angle) * Reach)).add(Frame[1][Index].scale(Math.sin(Angle) * Reach));
            }
            strand(Buffer, Pose, Shifted, frames(Shifted, End.subtract(From)), Scp337Lash.FrayShare, Light);
        }
    }

    private static Vec3[][] frames(Vec3[] Points, Vec3 Fallback) {
        int Count = Points.length - 1;
        Vec3[] Sides = new Vec3[Count + 1];
        Vec3[] Lifts = new Vec3[Count + 1];
        for (int Index = 0; Index <= Count; Index++) {
            Vec3 Along = Points[Math.min(Count, Index + 1)].subtract(Points[Math.max(0, Index - 1)]);
            if (Along.lengthSqr() < 1.0e-8) {
                Along = Fallback;
            }
            Along = Along.normalize();
            Vec3 Side = Along.cross(Up);
            if (Side.lengthSqr() < 1.0e-6) {
                Side = new Vec3(1.0, 0.0, 0.0);
            }
            Side = Side.normalize();
            Sides[Index] = Side;
            Lifts[Index] = Side.cross(Along).normalize();
        }
        return new Vec3[][]{Sides, Lifts};
    }

    private static void strand(VertexConsumer Buffer, PoseStack Pose, Vec3[] Points, Vec3[][] Frame, double Thickness, int Light) {
        int Count = Points.length - 1;
        Vec3[] Sides = Frame[0];
        Vec3[] Lifts = Frame[1];
        PoseStack.Pose Last = Pose.last();
        for (int Index = 0; Index < Count; Index++) {
            double NearShare = (double) Index / Count;
            double FarShare = (double) (Index + 1) / Count;
            double Near = Scp337Lash.width(NearShare) * 0.5 * Thickness;
            double Far = Scp337Lash.width(FarShare) * 0.5 * Thickness;
            for (int Face = 0; Face < 4; Face++) {
                Vec3 NearA = corner(Points[Index], Sides[Index], Lifts[Index], Face, Near);
                Vec3 NearB = corner(Points[Index], Sides[Index], Lifts[Index], Face + 1, Near);
                Vec3 FarA = corner(Points[Index + 1], Sides[Index + 1], Lifts[Index + 1], Face, Far);
                Vec3 FarB = corner(Points[Index + 1], Sides[Index + 1], Lifts[Index + 1], Face + 1, Far);
                Vec3 Normal = outward(Sides[Index], Lifts[Index], Face);
                float U0 = Face * 0.25f;
                float U1 = U0 + 0.25f;
                vertex(Buffer, Last, NearA, U0, (float) NearShare, Light, Normal);
                vertex(Buffer, Last, NearB, U1, (float) NearShare, Light, Normal);
                vertex(Buffer, Last, FarB, U1, (float) FarShare, Light, Normal);
                vertex(Buffer, Last, FarA, U0, (float) FarShare, Light, Normal);
            }
        }
    }

    private static Vec3 corner(Vec3 Centre, Vec3 Side, Vec3 Lift, int Face, double Half) {
        double Angle = Math.PI * 0.5 * (Face % 4) + Math.PI * 0.25;
        return Centre.add(Side.scale(Math.cos(Angle) * Half * 1.4142)).add(Lift.scale(Math.sin(Angle) * Half * 1.4142));
    }

    private static Vec3 outward(Vec3 Side, Vec3 Lift, int Face) {
        double Angle = Math.PI * 0.5 * (Face % 4) + Math.PI * 0.5;
        return Side.scale(Math.cos(Angle)).add(Lift.scale(Math.sin(Angle)));
    }

    private static void vertex(VertexConsumer Buffer, PoseStack.Pose Last, Vec3 Point, float U, float V, int Light, Vec3 Normal) {
        Buffer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(255, 255, 255, 255).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }
}
