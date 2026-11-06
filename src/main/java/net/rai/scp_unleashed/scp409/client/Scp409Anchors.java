package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;

public final class Scp409Anchors {
    public static final int BaseClusters = 22;
    public static final int MinClusters = 8;
    public static final int MaxClusters = 34;
    public static final float PlayerArea = 1632.0f;
    public static final int Variants = 5;
    public static final float BirthSpan = 0.9f;
    public static final float Ripen = 0.35f;
    public static final float Sprout = 0.3f;
    public static final float FinalBoost = 1.1f;
    public static final float MinJitter = 0.6f;
    public static final float JitterSpan = 0.45f;
    public static final float MaxTilt = 18.0f;
    public static final float Margin = 0.14f;
    public static final float AreaPower = 0.8f;
    public static final float Embed = 0.03f;

    private Scp409Anchors() {
    }

    public record Anchor(int Face, float S, float T, int Variant, float Spin, float Tilt, float Birth, float Jitter) {
    }

    public interface Sink {
        void cluster(PoseStack Pose, int Variant, float Size);
    }

    public static float body(float TotalArea) {
        return Mth.clamp((float) Math.sqrt(TotalArea / PlayerArea), 0.55f, 1.8f);
    }

    public static float sizeScale(float Body) {
        return Mth.clamp((float) Math.sqrt(Body), 0.7f, 1.35f);
    }

    public static int count(float Body) {
        return Mth.clamp(Math.round(BaseClusters * Body), MinClusters, MaxClusters);
    }

    public static long seed(UUID Id) {
        return Id.getMostSignificantBits() * 31L ^ Id.getLeastSignificantBits();
    }

    public static List<Anchor> pick(float[] Areas, float TotalArea, long Seed) {
        if (Areas.length == 0) {
            return List.of();
        }
        double[] Cumulative = new double[Areas.length];
        double Running = 0.0;
        for (int Index = 0; Index < Areas.length; Index++) {
            Running += Math.pow(Math.max(0.0f, Areas[Index]), AreaPower);
            Cumulative[Index] = Running;
        }
        if (Running <= 0.0) {
            return List.of();
        }
        SplittableRandom Random = new SplittableRandom(Seed);
        int Total = count(body(TotalArea));
        List<Anchor> Picked = new ArrayList<>(Total);
        for (int Index = 0; Index < Total; Index++) {
            double Roll = Random.nextDouble() * Running;
            int Face = Arrays.binarySearch(Cumulative, Roll);
            Face = Face >= 0 ? Face : -Face - 1;
            Face = Math.min(Face, Areas.length - 1);
            float S = Margin + (float) Random.nextDouble() * (1.0f - 2.0f * Margin);
            float T = Margin + (float) Random.nextDouble() * (1.0f - 2.0f * Margin);
            int Variant = Random.nextInt(Variants);
            float Spin = (float) Random.nextDouble() * 360.0f;
            float Tilt = ((float) Random.nextDouble() * 2.0f - 1.0f) * MaxTilt;
            float Birth = Index / (float) Total * BirthSpan;
            float Jitter = MinJitter + (float) Random.nextDouble() * JitterSpan;
            Picked.add(new Anchor(Face, S, T, Variant, Spin, Tilt, Birth, Jitter));
        }
        return List.copyOf(Picked);
    }

    public static float size(Anchor Item, float Growth, int Stage, float Scale) {
        if (Stage <= 0 || Growth + 1.0e-4f < Item.Birth()) {
            return 0.0f;
        }
        float Ripe = Stage >= Scp409Infection.Stages ? 1.0f : Mth.clamp((Growth - Item.Birth()) / Ripen, 0.0f, 1.0f);
        float Eased = Ripe * Ripe * (3.0f - 2.0f * Ripe);
        float Boost = Stage >= Scp409Infection.Stages ? FinalBoost : 1.0f;
        return Scale * Item.Jitter() * (Sprout + (1.0f - Sprout) * Eased) * Boost;
    }

    public static void orient(PoseStack Pose, float NormalX, float NormalY, float NormalZ, Anchor Item) {
        Pose.mulPose(new Quaternionf().rotationTo(0.0f, 1.0f, 0.0f, NormalX, NormalY, NormalZ));
        Pose.mulPose(Axis.YP.rotationDegrees(Item.Spin()));
        Pose.mulPose(Axis.XP.rotationDegrees(Item.Tilt()));
    }
}
