package net.rai.scp_unleashed.extraction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ExtractionBeam {
    private static final int GlowSides = 12;
    private static final int MoteSides = 6;
    private static final int Motes = 7;
    private static final double MoteSpeed = 0.17;
    private static final double PulseSpacing = 0.7;

    private ExtractionBeam() {
    }

    public static void draw(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Start, Vec3 End, int Colour, float Progress, float Time, int Seed) {
        Vec3 Span = End.subtract(Start);
        double Length = Span.length();
        if (Length < 0.05) {
            return;
        }
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Helper = Math.abs(Forward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 AxisA = Forward.cross(Helper).normalize();
        Vec3 AxisB = Forward.cross(AxisA).normalize();
        float Red = (Colour >> 16 & 255) / 255.0F;
        float Green = (Colour >> 8 & 255) / 255.0F;
        float Blue = (Colour & 255) / 255.0F;
        float Charge = Mth.clamp(Progress, 0.0F, 1.0F);
        float Intensity = (0.6F + 0.4F * Charge) * (1.0F + 0.25F * Charge * Charge * Mth.sin(Time * 2.7F + Seed));
        int Count = Mth.clamp(Mth.ceil(Length * 10.0), 10, 96);
        Vec3[] Path = new Vec3[Count + 1];
        float[] Taper = new float[Count + 1];
        float[] Flow = new float[Count + 1];
        double Wobble = 0.02 + 0.07 * Charge;
        for (int Index = 0; Index <= Count; Index++) {
            double T = (double) Index / Count;
            double Envelope = Math.sin(Math.PI * T);
            double SwayA = Math.sin(T * 9.0 - Time * 0.35 + Seed) * Wobble * Envelope;
            double SwayB = Math.cos(T * 7.0 - Time * 0.29 + Seed * 1.7) * Wobble * Envelope;
            Path[Index] = Start.add(Span.scale(T)).add(AxisA.scale(SwayA)).add(AxisB.scale(SwayB));
            double Along = T * Length;
            Taper[Index] = (float) (smooth(Along / 0.25) * smooth((Length - Along) / 0.2));
            Flow[Index] = 0.55F + 0.45F * Mth.sin((float) (Mth.TWO_PI * (Along / PulseSpacing + Time * 0.1)));
        }
        float[] GlowAlpha = new float[Count + 1];
        float[] CoreAlpha = new float[Count + 1];
        float[] StrandAlpha = new float[Count + 1];
        for (int Index = 0; Index <= Count; Index++) {
            GlowAlpha[Index] = 0.3F * Intensity * Taper[Index] * Flow[Index];
            CoreAlpha[Index] = 0.85F * Intensity * Taper[Index] * (0.75F + 0.25F * Flow[Index]);
            StrandAlpha[Index] = 0.6F * Intensity * Taper[Index] * Flow[Index];
        }
        ribbon(Consumer, Last, Path, 0.1 + 0.06 * Charge, Red, Green, Blue, GlowAlpha);
        ribbon(Consumer, Last, Path, 0.02 + 0.012 * Charge, mixWhite(Red, 0.7F), mixWhite(Green, 0.7F), mixWhite(Blue, 0.7F), CoreAlpha);
        for (int Strand = 0; Strand < 2; Strand++) {
            Vec3[] Twist = new Vec3[Count + 1];
            for (int Index = 0; Index <= Count; Index++) {
                double T = (double) Index / Count;
                double Phase = T * Length * 5.5 - Time * 0.45 + Strand * Math.PI + Seed;
                double Radius = (0.045 + 0.05 * Charge) * Math.sqrt(Math.sin(Math.PI * T));
                Twist[Index] = Path[Index].add(AxisA.scale(Math.cos(Phase) * Radius)).add(AxisB.scale(Math.sin(Phase) * Radius));
            }
            ribbon(Consumer, Last, Twist, 0.012, mixWhite(Red, 0.35F), mixWhite(Green, 0.35F), mixWhite(Blue, 0.35F), StrandAlpha);
        }
        for (int Mote = 0; Mote < Motes; Mote++) {
            double T = 1.0 - frac((double) Mote / Motes + Time * MoteSpeed / Length + Seed * 0.13);
            Vec3 Point = pointAt(Path, T);
            float Fade = (float) Math.sin(Math.PI * T) * Intensity;
            glow(Consumer, Last, Point, 0.035 * (0.6 + 0.4 * Charge), mixWhite(Red, 0.5F), mixWhite(Green, 0.5F), mixWhite(Blue, 0.5F), 0.9F * Fade, MoteSides);
        }
        glow(Consumer, Last, Start, 0.05 + 0.03 * Charge, mixWhite(Red, 0.5F), mixWhite(Green, 0.5F), mixWhite(Blue, 0.5F), 0.75F * Intensity, GlowSides);
        glow(Consumer, Last, End, 0.25 + 0.2 * Charge, Red, Green, Blue, 0.45F * Intensity, GlowSides);
        glow(Consumer, Last, End, 0.08 + 0.06 * Charge, mixWhite(Red, 0.6F), mixWhite(Green, 0.6F), mixWhite(Blue, 0.6F), 0.7F * Intensity, GlowSides);
    }

    private static void ribbon(VertexConsumer Consumer, PoseStack.Pose Last, Vec3[] Points, double HalfWidth, float Red, float Green, float Blue, float[] Alpha) {
        for (int Index = 0; Index < Points.length - 1; Index++) {
            Vec3 From = Points[Index];
            Vec3 To = Points[Index + 1];
            Vec3 SideFrom = side(Points, Index).scale(HalfWidth);
            Vec3 SideTo = side(Points, Index + 1).scale(HalfWidth);
            vertex(Consumer, Last, From.subtract(SideFrom), Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, From, Red, Green, Blue, Alpha[Index]);
            vertex(Consumer, Last, To, Red, Green, Blue, Alpha[Index + 1]);
            vertex(Consumer, Last, To.subtract(SideTo), Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, From, Red, Green, Blue, Alpha[Index]);
            vertex(Consumer, Last, From.add(SideFrom), Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, To.add(SideTo), Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, To, Red, Green, Blue, Alpha[Index + 1]);
        }
    }

    private static void glow(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, double Radius, float Red, float Green, float Blue, float Alpha, int Sides) {
        Vec3 Facing = Centre.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : Centre.scale(-1.0).normalize();
        Vec3 Across = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Across = Across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        Vec3 Over = Across.cross(Facing).normalize();
        for (int Index = 0; Index < Sides; Index++) {
            double First = Mth.TWO_PI * Index / Sides;
            double Second = Mth.TWO_PI * (Index + 1) / Sides;
            Vec3 RimFrom = Centre.add(Across.scale(Math.cos(First) * Radius)).add(Over.scale(Math.sin(First) * Radius));
            Vec3 RimTo = Centre.add(Across.scale(Math.cos(Second) * Radius)).add(Over.scale(Math.sin(Second) * Radius));
            vertex(Consumer, Last, Centre, Red, Green, Blue, Alpha);
            vertex(Consumer, Last, RimFrom, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, RimTo, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, Centre, Red, Green, Blue, Alpha);
        }
    }

    private static Vec3 side(Vec3[] Points, int Index) {
        Vec3 Tangent = Points[Math.min(Index + 1, Points.length - 1)].subtract(Points[Math.max(Index - 1, 0)]);
        Vec3 Side = Tangent.cross(Points[Index].scale(-1.0));
        double Length = Side.length();
        return Length < 1.0E-7 ? new Vec3(0.0, 1.0, 0.0) : Side.scale(1.0 / Length);
    }

    private static Vec3 pointAt(Vec3[] Points, double T) {
        double Scaled = Mth.clamp(T, 0.0, 1.0) * (Points.length - 1);
        int Index = Math.min(Points.length - 2, (int) Math.floor(Scaled));
        return Points[Index].lerp(Points[Index + 1], Scaled - Index);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float Red, float Green, float Blue, float Alpha) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Red, Green, Blue, Mth.clamp(Alpha, 0.0F, 1.0F));
    }

    private static float mixWhite(float Channel, float Amount) {
        return Channel + (1.0F - Channel) * Amount;
    }

    private static double smooth(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    private static double frac(double Value) {
        return Value - Math.floor(Value);
    }
}
