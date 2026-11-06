package net.rai.scp_unleashed.fission.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fission.FissionReactorBlockEntity;
import net.rai.scp_unleashed.fission.FissionStatus;
import net.rai.scp_unleashed.fission.ReactorLayout;
import net.rai.scp_unleashed.fission.ReactorThermal;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class FissionReactorRenderer implements BlockEntityRenderer<FissionReactorBlockEntity> {
    public static final float Cell = 16.0f;
    public static final float GlassDepth = 3.0f;
    public static final float PlateDepth = 14.8f;
    public static final float RodDepth = 7.6f;
    public static final float HazeDepth = 11.5f;
    public static final float[] RodCentres = {5.0f, 11.0f};
    public static final float RodHalf = 3.0f;
    public static final float Opening = 2.0f;
    public static final float SurfaceLift = 0.04f;
    public static final int ScreenU0 = 3;
    public static final int ScreenU1 = 13;
    public static final int ScreenV0 = 3;
    public static final int ScreenV1 = 9;
    public static final int Segments = 5;
    public static final int[] LampU = {3, 7, 11};
    public static final int LampV = 11;
    public static final int LampSize = 2;
    public static final float BeaconU = 8.0f;
    public static final float BeaconV = 7.0f;
    public static final float BeaconHalf = 2.0f;
    public static final float[] Cherenkov = {0.22f, 0.55f, 1.0f};
    public static final float[] Overheat = {1.0f, 0.32f, 0.12f};
    public static final float[] LampGreen = {0.25f, 1.0f, 0.35f};
    public static final float[] LampAmber = {1.0f, 0.62f, 0.1f};
    public static final float[] LampRed = {1.0f, 0.16f, 0.1f};
    public static final float[] LampCyan = {0.3f, 0.85f, 1.0f};
    public static final float[] LampDim = {0.12f, 0.12f, 0.12f};
    public static final float[] WasteColour = {0.75f, 0.85f, 0.2f};
    public static final float[] GasColour = {0.45f, 1.0f, 0.3f};

    public FissionReactorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(FissionReactorBlockEntity Reactor, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Reactor.getLevel();
        if (World == null || ShaderCompat.renderingShadowPass()) {
            return;
        }
        Direction Facing = Reactor.getFacing();
        Vec3 Corner = ReactorLayout.frontCorner(Facing);
        float Power = Reactor.getShownPower(PartialTick);
        float Heat = Reactor.getShownHeat(PartialTick);
        float Clock = World.getGameTime() % 24000L + PartialTick;
        ReactorThermal Thermal = ReactorThermal.of(Reactor.getTemperature());
        Pose.pushPose();
        Pose.translate(Corner.x, 0.0, Corner.z);
        Pose.mulPose(Axis.YP.rotationDegrees(ReactorLayout.yaw(Facing)));
        Pose.scale(1.0f / Cell, 1.0f / Cell, 1.0f / Cell);
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Haze = Buffers.getBuffer(FissionRenderTypes.haze(FissionFx.Texture));
        core(Haze, Last, Power, Heat, Thermal, Clock);
        beaconHalos(Haze, Last, Reactor, Thermal, Clock);
        VertexConsumer Face = Buffers.getBuffer(FissionRenderTypes.surface(FissionFx.Texture));
        panel(Face, Last, Reactor, Power, Heat, Thermal, Clock);
        beaconLenses(Face, Last, Reactor, Thermal, Clock);
        Pose.popPose();
    }

    private static float[] coreColour(float Heat, ReactorThermal Thermal, float Clock) {
        float Mix = Mth.clamp((Heat - ReactorThermal.WarningLevel) / (ReactorThermal.MeltdownLevel - ReactorThermal.WarningLevel), 0.0f, 1.0f);
        if (Thermal == ReactorThermal.Critical) {
            Mix = Math.min(1.0f, Mix + 0.25f * (0.5f + 0.5f * Mth.sin(Clock * 0.9f)));
        }
        return new float[]{Mth.lerp(Mix, Cherenkov[0], Overheat[0]), Mth.lerp(Mix, Cherenkov[1], Overheat[1]), Mth.lerp(Mix, Cherenkov[2], Overheat[2])};
    }

    private static void core(VertexConsumer Consumer, PoseStack.Pose Last, float Power, float Heat, ReactorThermal Thermal, float Clock) {
        float Residual = Mth.clamp((Heat - 200.0f) / 1600.0f, 0.0f, 0.25f);
        float Drive = Math.max(Power, Residual);
        if (Drive <= 0.005f) {
            return;
        }
        float[] Colour = coreColour(Heat, Thermal, Clock);
        int Index = 0;
        for (int Row = ReactorLayout.WindowLow; Row <= ReactorLayout.WindowHigh; Row++) {
            for (int Across = ReactorLayout.WindowLeft; Across <= ReactorLayout.WindowRight; Across++) {
                float X0 = Across * Cell;
                float Y0 = Row * Cell;
                float Flicker = 0.88f + 0.08f * Mth.sin(Clock * 0.31f + Index * 1.7f) + 0.04f * Mth.sin(Clock * 1.37f + Index * 0.9f);
                float Glow = Drive * Flicker;
                FissionFx.faceZ(Consumer, Last, X0 + Opening, Y0 + Opening, X0 + Cell - Opening, Y0 + Cell - Opening, -PlateDepth, FissionFx.Plate, Colour, 1.15f * Glow);
                FissionFx.faceZ(Consumer, Last, X0 + Opening, Y0 + Opening, X0 + Cell - Opening, Y0 + Cell - Opening, -HazeDepth, FissionFx.Plate, Colour, 0.4f * Glow);
                for (float Centre : RodCentres) {
                    FissionFx.faceZ(Consumer, Last, X0 + Centre - RodHalf, Y0 + Opening, X0 + Centre + RodHalf, Y0 + Cell - Opening, -RodDepth, FissionFx.Band, Colour, 0.3f * Glow);
                }
                FissionFx.faceZ(Consumer, Last, X0 + Opening, Y0 + Opening, X0 + Cell - Opening, Y0 + Cell - Opening, -GlassDepth - 0.3f, FissionFx.Radial, Colour, 0.25f * Glow);
                Index++;
            }
        }
    }

    private static void panel(VertexConsumer Consumer, PoseStack.Pose Last, FissionReactorBlockEntity Reactor, float Power, float Heat, ReactorThermal Thermal, float Clock) {
        boolean Online = Reactor.isRunning() || Power > 0.01f;
        float HeatShare = ReactorThermal.fraction(Heat);
        float[] Bars = {HeatShare, Power, Reactor.getWasteShown() / 100.0f, Reactor.getGasShown() / 100.0f};
        for (int Module = 0; Module < Bars.length; Module++) {
            float X0 = (ReactorLayout.WindowLeft + Module) * Cell;
            float Y0 = ReactorLayout.PanelRow * Cell;
            bar(Consumer, Last, X0, Y0, Module, Bars[Module], Clock);
            for (int Lamp = 0; Lamp < LampU.length; Lamp++) {
                float[] Colour = lampColour(Reactor, Module, Lamp, Online, Thermal, Clock);
                if (Colour == null) {
                    continue;
                }
                float U = LampU[Lamp];
                FissionFx.faceZ(Consumer, Last, X0 + U, Y0 + Cell - LampV - LampSize, X0 + U + LampSize, Y0 + Cell - LampV, SurfaceLift, FissionFx.Solid, Colour, 1.0f);
            }
        }
    }

    private static void bar(VertexConsumer Consumer, PoseStack.Pose Last, float X0, float Y0, int Module, float Share, float Clock) {
        float Width = (float) (ScreenU1 - ScreenU0) / Segments;
        int Lit = Math.round(Mth.clamp(Share, 0.0f, 1.0f) * Segments);
        if (Share > 0.02f) {
            Lit = Math.max(1, Lit);
        }
        for (int Segment = 0; Segment < Lit; Segment++) {
            float Step = (float) Segment / (Segments - 1);
            float[] Colour = switch (Module) {
                case 0 -> Step < 0.5f ? LampGreen : Step < 0.8f ? LampAmber : LampRed;
                case 1 -> LampCyan;
                case 2 -> Step < 0.8f ? WasteColour : LampRed;
                default -> Step < 0.8f ? GasColour : LampAmber;
            };
            float Strength = 0.8f + 0.2f * Mth.sin(Clock * 0.4f + Segment);
            float U0 = ScreenU0 + Segment * Width;
            FissionFx.faceZ(Consumer, Last, X0 + U0 + 0.15f, Y0 + Cell - ScreenV1, X0 + U0 + Width - 0.15f, Y0 + Cell - ScreenV0, SurfaceLift, FissionFx.Solid, Colour, Strength);
        }
    }

    private static float[] lampColour(FissionReactorBlockEntity Reactor, int Module, int Lamp, boolean Online, ReactorThermal Thermal, float Clock) {
        boolean Slow = Clock % 20.0f < 11.0f;
        boolean Fast = Clock % 8.0f < 4.0f;
        FissionStatus Status = Reactor.getStatus();
        return switch (Module) {
            case 0 -> switch (Lamp) {
                case 0 -> Thermal == ReactorThermal.Normal && Online ? LampGreen : null;
                case 1 -> Thermal == ReactorThermal.Warning && Slow ? LampAmber : null;
                default -> Thermal == ReactorThermal.Critical && Fast ? LampRed : null;
            };
            case 1 -> switch (Lamp) {
                case 0 -> Status == FissionStatus.Running ? LampGreen : null;
                case 1 -> Status == FissionStatus.LoadFollowing ? LampCyan : Status == FissionStatus.NoFuel || Status == FissionStatus.SpentFull ? (Slow ? LampAmber : null) : null;
                default -> Status == FissionStatus.Interlocked ? (Slow ? LampRed : null) : null;
            };
            case 2 -> switch (Lamp) {
                case 0 -> Online && Reactor.getWasteShown() < 90 ? LampGreen : null;
                case 1 -> Reactor.getWasteShown() >= 75 && Reactor.getWasteShown() < 100 ? LampAmber : null;
                default -> Status == FissionStatus.WasteFull && Fast ? LampRed : null;
            };
            default -> switch (Lamp) {
                case 0 -> Online && Reactor.getLeakShown() == 0 ? LampGreen : null;
                case 1 -> Reactor.getGasShown() >= 60 && Reactor.getLeakShown() == 0 ? LampAmber : null;
                default -> Reactor.getLeakShown() > 0 && Fast ? LampRed : null;
            };
        };
    }

    private static float[] beaconColour(FissionReactorBlockEntity Reactor, ReactorThermal Thermal, float Clock) {
        if (Thermal == ReactorThermal.Critical) {
            return Clock % 8.0f < 4.0f ? LampRed : null;
        }
        if (Thermal == ReactorThermal.Warning) {
            return Clock % 20.0f < 11.0f ? LampAmber : null;
        }
        if (Reactor.getLeakShown() > 0) {
            return Clock % 14.0f < 7.0f ? LampAmber : null;
        }
        if (Reactor.isInterlocked()) {
            return Clock % 30.0f < 12.0f ? LampRed : null;
        }
        return Reactor.isRunning() ? LampGreen : null;
    }

    private static float beaconStrength(ReactorThermal Thermal, FissionReactorBlockEntity Reactor) {
        return Thermal != ReactorThermal.Normal || Reactor.getLeakShown() > 0 ? 1.0f : 0.45f;
    }

    private static void beaconLenses(VertexConsumer Consumer, PoseStack.Pose Last, FissionReactorBlockEntity Reactor, ReactorThermal Thermal, float Clock) {
        float[] Colour = beaconColour(Reactor, Thermal, Clock);
        if (Colour == null) {
            return;
        }
        float Strength = beaconStrength(Thermal, Reactor);
        float Width = ReactorLayout.Width * Cell;
        float Top = ReactorLayout.Height * Cell;
        float LensY = Top - BeaconV;
        for (int Corner = 0; Corner < 4; Corner++) {
            boolean Right = Corner % 2 == 1;
            boolean Back = Corner >= 2;
            float CentreX = Right ? Width - BeaconU : BeaconU;
            float CentreZ = Back ? -Width + BeaconU : -BeaconU;
            float FaceZ = Back ? -Width - SurfaceLift : SurfaceLift;
            FissionFx.faceZ(Consumer, Last, CentreX - BeaconHalf, LensY - BeaconHalf, CentreX + BeaconHalf, LensY + BeaconHalf, FaceZ, FissionFx.Solid, Colour, Strength);
            float FaceX = Right ? Width + SurfaceLift : -SurfaceLift;
            FissionFx.faceX(Consumer, Last, FaceX, LensY - BeaconHalf, CentreZ - BeaconHalf, LensY + BeaconHalf, CentreZ + BeaconHalf, FissionFx.Solid, Colour, Strength);
            FissionFx.faceY(Consumer, Last, CentreX - BeaconHalf, CentreZ - BeaconHalf, CentreX + BeaconHalf, CentreZ + BeaconHalf, Top + SurfaceLift, FissionFx.Solid, Colour, Strength);
        }
    }

    private static void beaconHalos(VertexConsumer Consumer, PoseStack.Pose Last, FissionReactorBlockEntity Reactor, ReactorThermal Thermal, float Clock) {
        float[] Colour = beaconColour(Reactor, Thermal, Clock);
        if (Colour == null) {
            return;
        }
        float Strength = beaconStrength(Thermal, Reactor);
        float Radius = Thermal == ReactorThermal.Normal && Reactor.getLeakShown() == 0 ? 4.0f : 9.0f;
        float Width = ReactorLayout.Width * Cell;
        float Top = ReactorLayout.Height * Cell;
        for (int Corner = 0; Corner < 4; Corner++) {
            boolean Right = Corner % 2 == 1;
            boolean Back = Corner >= 2;
            float CentreX = Right ? Width - BeaconU : BeaconU;
            float CentreZ = Back ? -Width + BeaconU : -BeaconU;
            float CentreY = Top + 1.5f;
            FissionFx.faceZ(Consumer, Last, CentreX - Radius, CentreY - Radius, CentreX + Radius, CentreY + Radius, CentreZ, FissionFx.Radial, Colour, 0.5f * Strength);
            FissionFx.faceX(Consumer, Last, CentreX, CentreY - Radius, CentreZ - Radius, CentreY + Radius, CentreZ + Radius, FissionFx.Radial, Colour, 0.5f * Strength);
            FissionFx.faceY(Consumer, Last, CentreX - Radius, CentreZ - Radius, CentreX + Radius, CentreZ + Radius, CentreY, FissionFx.Radial, Colour, 0.35f * Strength);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(FissionReactorBlockEntity Reactor) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(FissionReactorBlockEntity Reactor) {
        return Reactor.getRenderBounds();
    }
}
