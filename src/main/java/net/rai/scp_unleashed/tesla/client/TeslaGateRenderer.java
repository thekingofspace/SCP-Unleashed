package net.rai.scp_unleashed.tesla.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.tesla.TeslaGateBlockEntity;
import net.rai.scp_unleashed.tesla.TeslaGeometry;
import net.rai.scp_unleashed.tesla.TeslaPhase;
import net.rai.scp_unleashed.tesla.TeslaStatus;
import net.rai.scp_unleashed.tesla.TeslaSurface;

import java.util.ArrayList;
import java.util.List;

public class TeslaGateRenderer implements BlockEntityRenderer<TeslaGateBlockEntity> {
    private static final ResourceLocation ArcTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/tesla/fx_arc.png");
    private static final ResourceLocation GlowTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/tesla/fx_glow.png");
    private static final long FlickerMillis = 45L;
    private static final float ReadyShimmer = 0.22f;
    private static final double LensHalf = 1.0;
    private static final double NearFadeStart = 1.0;
    private static final double NearFadeRange = 5.0;
    private static final float[] ArcCore = {1.0f, 1.0f, 1.0f};
    private static final float[] ArcHalo = {0.32f, 0.55f, 1.0f};
    private static final float[] ArcHaze = {0.16f, 0.3f, 0.85f};
    private static final float[] KnobGlow = {0.3f, 0.55f, 1.0f};
    private static final float[] LampGreen = {0.25f, 1.0f, 0.35f};
    private static final float[] LampRed = {1.0f, 0.18f, 0.12f};
    private static final float[] LampAmber = {1.0f, 0.62f, 0.1f};
    private static final Vec3 Up = new Vec3(0.0, 1.0, 0.0);
    private static final float[][] FullUvs = {{0.0f, 1.0f}, {1.0f, 1.0f}, {1.0f, 0.0f}, {0.0f, 0.0f}};
    private static final float[][] LensUvs = {{0.47f, 0.53f}, {0.53f, 0.53f}, {0.53f, 0.47f}, {0.47f, 0.47f}};
    private static final int[][] Triangles = {{0, 1, 2}, {0, 2, 3}};

    public TeslaGateRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(TeslaGateBlockEntity Gate, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Gate.getLevel();
        if (World == null || ShaderCompat.renderingShadowPass()) {
            return;
        }
        TeslaPhase Phase = Gate.getPhase();
        TeslaStatus Status = Gate.getStatus();
        if (Status == TeslaStatus.Off && !Phase.isActive()) {
            return;
        }
        Direction Front = Gate.getFront();
        BlockPos Origin = Gate.getOrigin();
        Pose.pushPose();
        Pose.translate(Origin.getX() - Gate.getBlockPos().getX() + 0.5, Origin.getY() - Gate.getBlockPos().getY(), Origin.getZ() - Gate.getBlockPos().getZ() + 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(TeslaGeometry.yaw(Front)));
        Pose.translate(TeslaGeometry.pivotAcross(), 0.0, TeslaGeometry.pivotAlong());
        Pose.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        Pose.translate(-TeslaGeometry.PixelsWide / 2.0, 0.0, -TeslaGeometry.PixelsDeep / 2.0);
        PoseStack.Pose Last = Pose.last();
        Vec3 Eye = TeslaGeometry.toLocal(Origin, Front, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        float Clock = World.getGameTime() % 24000L + PartialTick;
        float Heat = Gate.getHeat(PartialTick);
        long Window = Util.getMillis() / FlickerMillis;
        float Pulse = Phase == TeslaPhase.Charged ? 0.85f + 0.15f * Mth.sin(Clock * 0.55f) : 1.0f;
        float Energy = Phase.isActive() ? Heat * Pulse : Status == TeslaStatus.Ready ? ReadyShimmer : 0.0f;
        RandomSource Random = RandomSource.create(Gate.getBlockPos().asLong() * 31L + Window);
        List<TeslaArcs.Bolt> Bolts = new ArrayList<>();
        float Strength = TeslaArcs.generate(Random, Phase, Gate.windUpProgress(PartialTick), Bolts);
        VertexConsumer FaceGlow = Buffers.getBuffer(TeslaRenderTypes.surface(GlowTexture));
        lamps(FaceGlow, Last, Eye, Status, Phase, Clock);
        if (Energy > 0.3f) {
            for (TeslaGeometry.Emitter Point : TeslaGeometry.BarPoints) {
                decal(FaceGlow, Last, Eye, Point.Start(), Point.Surface(), 1.6f + 1.4f * Energy, KnobGlow, 0.35f * Energy);
            }
        }
        if (Phase == TeslaPhase.Discharge || Energy > 0.75f) {
            VertexConsumer AirGlow = Buffers.getBuffer(TeslaRenderTypes.additive(GlowTexture));
            float Haze = Phase == TeslaPhase.Discharge ? 0.32f : 0.12f * (Energy - 0.75f) / 0.25f;
            for (Vec3 Glow : TeslaGeometry.Glows) {
                sprite(AirGlow, Last, Glow, Eye, 13.0f, ArcHaze, Haze);
            }
        }
        VertexConsumer FaceArc = Buffers.getBuffer(TeslaRenderTypes.surface(ArcTexture));
        if (Energy > 0.0f) {
            bars(FaceArc, Last, Eye, Energy, Clock, Random);
        }
        for (TeslaArcs.Bolt Streak : Bolts) {
            if (!Streak.Surface().isVolume()) {
                bolt(FaceArc, Last, Eye, Streak, Strength);
            }
        }
        if (Bolts.stream().anyMatch(Streak -> Streak.Surface().isVolume())) {
            VertexConsumer AirArc = Buffers.getBuffer(TeslaRenderTypes.additive(ArcTexture));
            for (TeslaArcs.Bolt Streak : Bolts) {
                if (Streak.Surface().isVolume()) {
                    bolt(AirArc, Last, Eye, Streak, Strength);
                }
            }
        }
        Pose.popPose();
    }

    private static void bolt(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, TeslaArcs.Bolt Streak, float Strength) {
        if (Streak.Branch()) {
            ribbon(Consumer, Last, Streak.Points(), Eye, 0.4f, ArcCore, 0.7f * Strength, true, Streak.Surface());
            ribbon(Consumer, Last, Streak.Points(), Eye, 1.4f, ArcHalo, 0.35f * Strength, true, Streak.Surface());
            return;
        }
        ribbon(Consumer, Last, Streak.Points(), Eye, 0.55f, ArcCore, 1.0f * Strength, false, Streak.Surface());
        ribbon(Consumer, Last, Streak.Points(), Eye, 2.0f, ArcHalo, 0.55f * Strength, false, Streak.Surface());
        ribbon(Consumer, Last, Streak.Points(), Eye, 3.6f, ArcHaze, 0.16f * Strength, false, Streak.Surface());
    }

    private static void bars(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, float Energy, float Clock, RandomSource Random) {
        int Index = 0;
        for (TeslaGeometry.Emitter Bar : TeslaGeometry.Emitters) {
            float Flicker = 0.7f + 0.2f * Mth.sin(Clock * 0.9f + Index * 2.3f) + 0.1f * Random.nextFloat();
            float Strength = Energy * Flicker;
            Vec3 Inset = Bar.End().subtract(Bar.Start()).normalize().scale(0.2);
            List<Vec3> Path = List.of(Bar.Start().add(Inset), Bar.End().subtract(Inset));
            ribbon(Consumer, Last, Path, Eye, 0.5f, ArcCore, 0.45f * Strength, false, Bar.Surface());
            ribbon(Consumer, Last, Path, Eye, 1.6f, ArcHalo, 0.6f * Strength, false, Bar.Surface());
            ribbon(Consumer, Last, Path, Eye, 3.0f, ArcHaze, 0.3f * Strength, false, Bar.Surface());
            Index++;
        }
    }

    private static void lamps(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, TeslaStatus Status, TeslaPhase Phase, float Clock) {
        float[] Colour;
        float Strength;
        boolean LeftOn = true;
        boolean RightOn = true;
        switch (Status) {
            case Ready -> {
                Colour = LampGreen;
                Strength = 0.55f;
            }
            case Maintenance -> {
                Colour = LampAmber;
                Strength = 0.75f;
            }
            case Fault -> {
                Colour = LampAmber;
                Strength = Clock % 10.0f < 5.0f ? 0.8f : 0.0f;
            }
            case Armed -> {
                Colour = LampRed;
                switch (Phase) {
                    case Charged -> {
                        boolean Swap = Clock % 16.0f < 8.0f;
                        LeftOn = Swap;
                        RightOn = !Swap;
                        Strength = 1.0f;
                    }
                    case WindUp, Discharge -> Strength = Clock % 4.0f < 2.0f ? 1.0f : 0.55f;
                    default -> Strength = Clock % 20.0f < 10.0f ? 0.8f : 0.0f;
                }
            }
            default -> {
                return;
            }
        }
        if (Strength <= 0.0f) {
            return;
        }
        for (int Index = 0; Index < TeslaGeometry.Lamps.size(); Index++) {
            boolean Left = Index % 2 == 0;
            TeslaGeometry.Emitter Lamp = TeslaGeometry.Lamps.get(Index);
            if ((Left ? !LeftOn : !RightOn) || !Lamp.Surface().faces(Lamp.Start(), Eye)) {
                continue;
            }
            patch(Consumer, Last, Eye, Lamp.Start(), Lamp.Surface(), LensHalf, LensHalf, LensUvs, Colour, 1.0f * Strength);
            decal(Consumer, Last, Eye, Lamp.Start(), Lamp.Surface(), 1.8f, ArcCore, 0.3f * Strength);
            decal(Consumer, Last, Eye, Lamp.Start(), Lamp.Surface(), 4.5f, Colour, 0.5f * Strength);
        }
    }

    private static float nearFade(Vec3 Eye, Vec3 A, Vec3 B) {
        Vec3 Along = B.subtract(A);
        double Fraction = Mth.clamp(Eye.subtract(A).dot(Along) / Math.max(Along.lengthSqr(), 1.0E-9), 0.0, 1.0);
        double Distance = Eye.distanceTo(A.add(Along.scale(Fraction)));
        return (float) Mth.clamp((Distance - NearFadeStart) / NearFadeRange, 0.0, 1.0);
    }

    private static void decal(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, Vec3 Centre, TeslaSurface Surface, float Radius, float[] Colour, float Strength) {
        patch(Consumer, Last, Eye, Centre, Surface, Radius, Radius, FullUvs, Colour, Strength);
    }

    private static void patch(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, Vec3 Centre, TeslaSurface Surface, double HalfWidth, double HalfHeight, float[][] Uvs, float[] Colour, float Strength) {
        float Amount = Strength * nearFade(Eye, Centre, Centre);
        float Red = Mth.clamp(Colour[0] * Amount, 0.0f, 1.0f);
        float Green = Mth.clamp(Colour[1] * Amount, 0.0f, 1.0f);
        float Blue = Mth.clamp(Colour[2] * Amount, 0.0f, 1.0f);
        if (Red + Green + Blue <= 0.0f) {
            return;
        }
        Vec3 Across = Surface.across().scale(HalfWidth);
        Vec3 Over = Surface.up().scale(HalfHeight);
        Vec3[] Corners = {Centre.subtract(Across).subtract(Over), Centre.add(Across).subtract(Over), Centre.add(Across).add(Over), Centre.subtract(Across).add(Over)};
        emit(Consumer, Last, Corners, Uvs, Red, Green, Blue, Surface.normal(), Surface);
    }

    private static void sprite(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, Vec3 Eye, float Radius, float[] Colour, float Strength) {
        Vec3 Toward = Eye.subtract(Centre);
        if (Toward.lengthSqr() < 1.0E-6) {
            return;
        }
        Toward = Toward.normalize();
        Vec3 Right = Up.cross(Toward);
        Right = Right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Right.normalize();
        Vec3 Lift = Toward.cross(Right);
        Vec3 Across = Right.scale(Radius);
        Vec3 Over = Lift.scale(Radius);
        float Amount = Strength * nearFade(Eye, Centre, Centre);
        float Red = Mth.clamp(Colour[0] * Amount, 0.0f, 1.0f);
        float Green = Mth.clamp(Colour[1] * Amount, 0.0f, 1.0f);
        float Blue = Mth.clamp(Colour[2] * Amount, 0.0f, 1.0f);
        if (Red + Green + Blue <= 0.0f) {
            return;
        }
        Vec3[] Corners = {Centre.subtract(Across).subtract(Over), Centre.add(Across).subtract(Over), Centre.add(Across).add(Over), Centre.subtract(Across).add(Over)};
        emit(Consumer, Last, Corners, FullUvs, Red, Green, Blue, Toward, TeslaSurface.Opening);
    }

    private static void ribbon(VertexConsumer Consumer, PoseStack.Pose Last, List<Vec3> Path, Vec3 Eye, float HalfWidth, float[] Colour, float Strength, boolean Taper, TeslaSurface Surface) {
        int Count = Path.size();
        if (Strength <= 0.0f || Count < 2) {
            return;
        }
        boolean Flat = !Surface.isVolume();
        Vec3[] Sides = new Vec3[Count];
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Along = Path.get(Math.min(Count - 1, Index + 1)).subtract(Path.get(Math.max(0, Index - 1)));
            Vec3 Side = Along.cross(Flat ? Surface.normal() : Eye.subtract(Path.get(Index)));
            if (Side.lengthSqr() < 1.0E-9) {
                Sides[Index] = Index > 0 ? Sides[Index - 1] : Vec3.ZERO;
                continue;
            }
            float Width = Taper ? HalfWidth * (1.0f - 0.85f * Index / (Count - 1)) : HalfWidth;
            Side = Side.normalize().scale(Width);
            if (Index > 0 && Side.dot(Sides[Index - 1]) < 0.0) {
                Side = Side.scale(-1.0);
            }
            Sides[Index] = Side;
        }
        for (int Index = 0; Index + 1 < Count; Index++) {
            Vec3 A = Path.get(Index);
            Vec3 B = Path.get(Index + 1);
            float Amount = Strength * nearFade(Eye, A, B);
            float Red = Mth.clamp(Colour[0] * Amount, 0.0f, 1.0f);
            float Green = Mth.clamp(Colour[1] * Amount, 0.0f, 1.0f);
            float Blue = Mth.clamp(Colour[2] * Amount, 0.0f, 1.0f);
            if (Red + Green + Blue <= 0.0f) {
                continue;
            }
            Vec3[] Corners = {A.subtract(Sides[Index]), A.add(Sides[Index]), B.add(Sides[Index + 1]), B.subtract(Sides[Index + 1])};
            emit(Consumer, Last, Corners, FullUvs, Red, Green, Blue, Flat ? Surface.normal() : Eye.subtract(A).normalize(), Surface);
        }
    }

    private static void emit(VertexConsumer Consumer, PoseStack.Pose Last, Vec3[] Corners, float[][] Uvs, float Red, float Green, float Blue, Vec3 Normal, TeslaSurface Bounds) {
        boolean Contained = true;
        for (Vec3 Corner : Corners) {
            Contained &= Bounds.contains(Corner, 0.0);
        }
        if (Contained) {
            for (int Corner = 0; Corner < 4; Corner++) {
                vertex(Consumer, Last, Corners[Corner].x, Corners[Corner].y, Corners[Corner].z, Uvs[Corner][0], Uvs[Corner][1], Red, Green, Blue, Normal);
            }
            return;
        }
        for (int[] Triangle : Triangles) {
            List<double[]> Polygon = new ArrayList<>(9);
            for (int Corner : Triangle) {
                Polygon.add(new double[]{Corners[Corner].x, Corners[Corner].y, Corners[Corner].z, Uvs[Corner][0], Uvs[Corner][1]});
            }
            for (int Dimension = 0; Dimension < 3 && Polygon.size() >= 3; Dimension++) {
                Polygon = cut(Polygon, Dimension, Bounds.low(Dimension), true);
                if (Polygon.size() >= 3) {
                    Polygon = cut(Polygon, Dimension, Bounds.high(Dimension), false);
                }
            }
            for (int Index = 1; Index + 1 < Polygon.size(); Index++) {
                double[] A = Polygon.getFirst();
                double[] B = Polygon.get(Index);
                double[] C = Polygon.get(Index + 1);
                vertex(Consumer, Last, A[0], A[1], A[2], (float) A[3], (float) A[4], Red, Green, Blue, Normal);
                vertex(Consumer, Last, B[0], B[1], B[2], (float) B[3], (float) B[4], Red, Green, Blue, Normal);
                vertex(Consumer, Last, C[0], C[1], C[2], (float) C[3], (float) C[4], Red, Green, Blue, Normal);
                vertex(Consumer, Last, C[0], C[1], C[2], (float) C[3], (float) C[4], Red, Green, Blue, Normal);
            }
        }
    }

    private static List<double[]> cut(List<double[]> Polygon, int Dimension, double Limit, boolean Above) {
        List<double[]> Kept = new ArrayList<>(Polygon.size() + 2);
        int Count = Polygon.size();
        for (int Index = 0; Index < Count; Index++) {
            double[] Current = Polygon.get(Index);
            double[] Next = Polygon.get((Index + 1) % Count);
            boolean CurrentIn = Above ? Current[Dimension] >= Limit : Current[Dimension] <= Limit;
            boolean NextIn = Above ? Next[Dimension] >= Limit : Next[Dimension] <= Limit;
            if (CurrentIn) {
                Kept.add(Current);
            }
            if (CurrentIn != NextIn) {
                double Fraction = (Limit - Current[Dimension]) / (Next[Dimension] - Current[Dimension]);
                double[] Cut = new double[Current.length];
                for (int Channel = 0; Channel < Current.length; Channel++) {
                    Cut[Channel] = Current[Channel] + (Next[Channel] - Current[Channel]) * Fraction;
                }
                Cut[Dimension] = Limit;
                Kept.add(Cut);
            }
        }
        return Kept;
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, double X, double Y, double Z, float U, float V, float Red, float Green, float Blue, Vec3 Normal) {
        Consumer.addVertex(Last, (float) X, (float) Y, (float) Z)
                .setColor(Red, Green, Blue, 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }

    @Override
    public boolean shouldRenderOffScreen(TeslaGateBlockEntity Gate) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TeslaGateBlockEntity Gate) {
        return Gate.getRenderBounds();
    }
}
