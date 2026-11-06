package net.rai.scp_unleashed.scp205.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.client.OutletRenderer;
import net.rai.scp_unleashed.scp205.Scp205Beam;
import net.rai.scp_unleashed.scp205.Scp205LampBlock;
import net.rai.scp_unleashed.scp205.Scp205LampBlockEntity;
import net.rai.scp_unleashed.scp205.Scp205Poses;
import net.rai.scp_unleashed.scp205.Scp205Registry;
import net.rai.scp_unleashed.scp205.Scp205Tilt;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class Scp205LampRenderer implements BlockEntityRenderer<Scp205LampBlockEntity> {
    public static final ResourceLocation BeamTexture = Scp205Registry.location("textures/entity/scp205/beam.png");
    public static final ResourceLocation PoolTexture = Scp205Registry.location("textures/entity/scp205/pool.png");
    public static final int Sides = 12;
    public static final int Slices = 6;
    public static final float OuterAlpha = 0.13f;
    public static final float CoreAlpha = 0.17f;
    public static final float CoreShare = 0.45f;
    public static final float PoolAlpha = 0.62f;
    public static final float AdditiveScale = 0.5f;
    public static final int Motes = 34;
    public static final float MoteSize = 0.03f;
    public static final float MoteAlpha = 0.55f;
    public static final float DayFigureHeight = 1.85f;
    public static final float DevilFigureHeight = 2.3f;
    public static final float DayAspect = 24.0f / 40.0f;
    public static final float DevilAspect = 32.0f / 44.0f;
    public static final float DayShadowAlpha = 0.6f;
    public static final float DevilShadowAlpha = 0.86f;
    public static final float WallLift = 0.012f;
    public static final float FigureLift = 0.022f;
    public static final float PoolFill = 0.92f;
    public static final float MinimumFigure = 0.4f;
    public static final float MaxStretch = 3.0f;
    public static final float BeamRed = 1.0f;
    public static final float BeamGreen = 0.93f;
    public static final float BeamBlue = 0.78f;
    public static final int FloorSearch = 6;
    private static final float[] BlinkCurve = {0.9f, 0.35f, 0.75f, 0.15f, 0.05f, 0.2f, 0.6f, 0.25f, 0.85f, 1.0f};
    private static final Map<String, ResourceLocation> Figures = new HashMap<>();

    private final OutletRenderer Wires;

    public Scp205LampRenderer(BlockEntityRendererProvider.Context Context) {
        Wires = new OutletRenderer(Context);
    }

    public static ResourceLocation figure(boolean DevilKind, int Index) {
        String Name = Scp205Poses.name(DevilKind, Index);
        return Figures.computeIfAbsent(Name, Key -> Scp205Registry.location("textures/entity/scp205/" + (DevilKind ? Key : "shadow_" + Key) + ".png"));
    }

    @Override
    public void render(Scp205LampBlockEntity Lamp, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Wires.render(Lamp, PartialTick, Pose, Buffers, PackedLight, PackedOverlay);
        Level World = Lamp.getLevel();
        BlockState State = Lamp.getBlockState();
        if (World == null || !State.hasProperty(Scp205LampBlock.Lit) || !State.getValue(Scp205LampBlock.Lit) || ShaderCompat.renderingShadowPass()) {
            return;
        }
        BlockPos Pos = Lamp.getBlockPos();
        Vec3 Corner = Vec3.atLowerCornerOf(Pos);
        Direction Side = Scp205Beam.facing(State);
        Scp205Tilt Tilt = Scp205Beam.tilt(State);
        Vec3 Origin = Scp205Beam.origin(Pos, Side, Tilt);
        Vec3 Axis = Scp205Beam.axis(Side, Tilt);
        double Length = Lamp.beamLength();
        Vec3 End = Origin.add(Axis.scale(Length));
        Direction Face = Lamp.hitFace();
        long Time = World.getGameTime();
        long Since = Time - Lamp.blinkAt();
        boolean Blinking = Since >= 0 && Since < BlinkCurve.length;
        float Flutter = 0.95f + 0.05f * ((HashCommon.mix(Time * 31L + Pos.asLong()) >>> 40) & 255) / 255.0f;
        float Flicker = (Blinking ? BlinkCurve[(int) Since] : 1.0f) * Flutter;
        boolean Additive = ShaderCompat.useFallback();
        PoseStack.Pose Last = Pose.last();
        if (Face != null) {
            pool(Buffers, Last, Corner, Origin, Axis, End, Face, Length, Flicker, Additive);
            byte Mode = Lamp.mode();
            boolean Hidden = Blinking && Since >= 3 && Since <= 5;
            if (Lamp.hitsWall() && !Hidden && (Mode == Scp205LampBlockEntity.ModeShadow || Mode == Scp205LampBlockEntity.ModeDevil)) {
                boolean DevilKind = Mode == Scp205LampBlockEntity.ModeDevil;
                int Shown = Blinking && Since < BlinkCurve.length / 2 ? Lamp.previousPose() : Lamp.pose();
                shadow(World, Buffers, Last, Corner, End, Face, Length, DevilKind, Shown, Flicker);
            }
        }
        cone(Buffers, Last, Corner, Origin, Axis, Face, End, Length, Flicker, Additive);
        motes(Buffers, Last, Corner, Origin, Axis, Length, Time + PartialTick, Pos.asLong(), Flicker, Additive);
    }

    private static RenderType glowType(ResourceLocation Texture, boolean Additive) {
        return Additive ? RenderType.eyes(Texture) : RenderType.beaconBeam(Texture, true);
    }

    private static void pool(MultiBufferSource Buffers, PoseStack.Pose Last, Vec3 Corner, Vec3 Origin, Vec3 Axis, Vec3 End, Direction Face, double Length, float Flicker,
                             boolean Additive) {
        Vec3 Normal = new Vec3(Face.getStepX(), Face.getStepY(), Face.getStepZ());
        Vec3 First;
        Vec3 Second;
        double Facing = Math.max(0.33, Math.abs(Axis.dot(Normal)));
        double Radius = Scp205Beam.radiusAt(Length);
        if (Face.getAxis().isHorizontal()) {
            First = new Vec3(0.0, 1.0, 0.0);
            Second = First.cross(Normal);
        } else {
            Vec3 Flat = new Vec3(Axis.x, 0.0, Axis.z);
            First = Flat.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Flat.normalize();
            Second = First.cross(Normal);
        }
        double Along = Math.min(Radius / Facing, Radius * MaxStretch);
        Vec3 Centre = End.add(Normal.scale(WallLift)).subtract(Corner);
        Vec3 A = Centre.subtract(First.scale(Along)).subtract(Second.scale(Radius));
        Vec3 B = Centre.subtract(First.scale(Along)).add(Second.scale(Radius));
        Vec3 C = Centre.add(First.scale(Along)).add(Second.scale(Radius));
        Vec3 D = Centre.add(First.scale(Along)).subtract(Second.scale(Radius));
        float Alpha = PoolAlpha * Flicker;
        float Shade = Additive ? AdditiveScale * Alpha : 1.0f;
        VertexConsumer Consumer = Buffers.getBuffer(glowType(PoolTexture, Additive));
        quad(Consumer, Last, A, B, C, D, 0.0f, 1.0f, 0.0f, 1.0f, BeamRed * Shade, BeamGreen * Shade, BeamBlue * Shade, Additive ? 1.0f : Alpha, LightTexture.FULL_BRIGHT,
                Normal, true);
    }

    private static void shadow(Level World, MultiBufferSource Buffers, PoseStack.Pose Last, Vec3 Corner, Vec3 End, Direction Face, double Length, boolean DevilKind, int Index,
                               float Flicker) {
        Vec3 Normal = new Vec3(Face.getStepX(), Face.getStepY(), Face.getStepZ());
        double Radius = Scp205Beam.radiusAt(Length) * PoolFill;
        double Top = End.y + Radius;
        double Floor = floorUnder(World, End.add(Normal.scale(0.3)), End.y - Radius);
        double Bottom = Math.max(Floor + 0.01, End.y - Radius);
        double Height = Math.min(DevilKind ? DevilFigureHeight : DayFigureHeight, Top - Bottom);
        if (Height < MinimumFigure) {
            return;
        }
        double Width = Height * (DevilKind ? DevilAspect : DayAspect);
        Vec3 Up = new Vec3(0.0, 1.0, 0.0);
        Vec3 Right = Up.cross(Normal);
        Vec3 Base = new Vec3(End.x, Bottom, End.z).add(Normal.scale(FigureLift)).subtract(Corner);
        Vec3 A = Base.subtract(Right.scale(Width / 2.0));
        Vec3 B = Base.add(Right.scale(Width / 2.0));
        Vec3 C = B.add(Up.scale(Height));
        Vec3 D = A.add(Up.scale(Height));
        float Alpha = (DevilKind ? DevilShadowAlpha : DayShadowAlpha) * Math.min(1.0f, 0.35f + 0.65f * Flicker);
        int Light = LevelRenderer.getLightColor(World, BlockPos.containing(End.add(Normal.scale(0.5))));
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.entityTranslucent(figure(DevilKind, Index)));
        float Tone = DevilKind ? 0.04f : 0.07f;
        quad(Consumer, Last, A, B, C, D, 0.0f, 1.0f, 1.0f, 0.0f, Tone, Tone * 0.9f, Tone * 1.2f, Alpha, Light, Normal, false);
    }

    private static double floorUnder(Level World, Vec3 Point, double Lowest) {
        BlockPos Cursor = BlockPos.containing(Point);
        for (int Step = 0; Step < FloorSearch; Step++) {
            BlockState Found = World.getBlockState(Cursor);
            VoxelShape Shape = Found.getCollisionShape(World, Cursor);
            if (!Shape.isEmpty()) {
                double Surface = Cursor.getY() + Shape.max(Direction.Axis.Y);
                if (Surface <= Point.y + 1.0E-3) {
                    return Surface;
                }
            }
            Cursor = Cursor.below();
        }
        return Lowest;
    }

    private static void cone(MultiBufferSource Buffers, PoseStack.Pose Last, Vec3 Corner, Vec3 Origin, Vec3 Axis, Direction Face, Vec3 End, double Length, float Flicker,
                             boolean Additive) {
        VertexConsumer Consumer = Buffers.getBuffer(glowType(BeamTexture, Additive));
        Vec3 Helper = Math.abs(Axis.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 Across = Axis.cross(Helper).normalize();
        Vec3 Second = Axis.cross(Across).normalize();
        Vec3 Normal = Face == null ? null : new Vec3(Face.getStepX(), Face.getStepY(), Face.getStepZ());
        boolean Open = Face == null;
        for (int Shell = 0; Shell < 2; Shell++) {
            double Share = Shell == 0 ? 1.0 : CoreShare;
            float Base = (Shell == 0 ? OuterAlpha : CoreAlpha) * Flicker;
            double Root = Scp205Beam.LensRadius * (Shell == 0 ? 1.0 : 0.6);
            double Spread = Scp205Beam.Spread * Share;
            Vec3[] Radials = new Vec3[Sides + 1];
            double[] Reach = new double[Sides + 1];
            for (int Index = 0; Index <= Sides; Index++) {
                double Angle = Math.PI * 2.0 * Index / Sides;
                Vec3 Radial = Across.scale(Math.cos(Angle)).add(Second.scale(Math.sin(Angle)));
                Radials[Index] = Radial;
                Reach[Index] = Length;
                if (Normal != null) {
                    Vec3 Heading = Axis.add(Radial.scale(Spread));
                    double Toward = Heading.dot(Normal);
                    if (Toward < -1.0E-4) {
                        double Hit = End.subtract(Origin).subtract(Radial.scale(Root)).dot(Normal) / Toward;
                        if (Hit > 0.0 && Hit < Scp205Beam.MaxReach * 1.5) {
                            Reach[Index] = Hit;
                        }
                    }
                }
            }
            for (int Slice = 0; Slice < Slices; Slice++) {
                float Near = Slice / (float) Slices;
                float Far = (Slice + 1) / (float) Slices;
                float NearAlpha = fade(Base, Near, Open);
                float FarAlpha = fade(Base, Far, Open);
                for (int Index = 0; Index < Sides; Index++) {
                    Vec3 A = ring(Origin, Axis, Radials[Index], Root, Spread, Reach[Index] * Near).subtract(Corner);
                    Vec3 B = ring(Origin, Axis, Radials[Index + 1], Root, Spread, Reach[Index + 1] * Near).subtract(Corner);
                    Vec3 C = ring(Origin, Axis, Radials[Index + 1], Root, Spread, Reach[Index + 1] * Far).subtract(Corner);
                    Vec3 D = ring(Origin, Axis, Radials[Index], Root, Spread, Reach[Index] * Far).subtract(Corner);
                    float U0 = Index / (float) Sides;
                    float U1 = (Index + 1) / (float) Sides;
                    float V0 = (float) (Near * Length / 4.0);
                    float V1 = (float) (Far * Length / 4.0);
                    gradient(Consumer, Last, A, B, C, D, U0, U1, V0, V1, NearAlpha, FarAlpha, Additive);
                }
            }
        }
    }

    private static float fade(float Base, float Share, boolean Open) {
        float Alpha = Base * (1.0f - 0.5f * Share);
        if (Open) {
            Alpha *= 1.0f - Share * Share;
        }
        return Alpha;
    }

    private static Vec3 ring(Vec3 Origin, Vec3 Axis, Vec3 Radial, double Root, double Spread, double Distance) {
        return Origin.add(Axis.scale(Distance)).add(Radial.scale(Root + Distance * Spread));
    }

    private static void motes(MultiBufferSource Buffers, PoseStack.Pose Last, Vec3 Corner, Vec3 Origin, Vec3 Axis, double Length, float Clock, long Seed, float Flicker,
                              boolean Additive) {
        VertexConsumer Consumer = Buffers.getBuffer(glowType(BeamTexture, Additive));
        Vec3 Helper = Math.abs(Axis.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 Across = Axis.cross(Helper).normalize();
        Vec3 Second = Axis.cross(Across).normalize();
        Quaternionf Facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        Vector3f Sideways = Facing.transform(new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f Upward = Facing.transform(new Vector3f(0.0f, 1.0f, 0.0f));
        double Usable = Math.max(0.0, Length - 0.15);
        for (int Index = 0; Index < Motes; Index++) {
            long Hash = HashCommon.mix(Seed * 1_000_003L + Index * 7919L);
            double Phase = (Hash & 1023) / 1024.0;
            double Speed = 0.004 + ((Hash >>> 10) & 255) / 255.0 * 0.006;
            double Angle = ((Hash >>> 18) & 1023) / 1024.0 * Math.PI * 2.0 + Clock * 0.004 * (((Hash >>> 28) & 1) == 0 ? 1.0 : -1.0);
            double Spot = Math.sqrt(((Hash >>> 30) & 1023) / 1024.0) * 0.85;
            double Along = ((Phase + Clock * Speed) % 1.0) * Usable;
            double Wobble = Math.sin(Clock * 0.05 + Index * 1.7) * 0.03;
            Vec3 Radial = Across.scale(Math.cos(Angle)).add(Second.scale(Math.sin(Angle)));
            Vec3 Centre = Origin.add(Axis.scale(Along)).add(Radial.scale(Scp205Beam.radiusAt(Along) * Spot)).add(0.0, Wobble, 0.0).subtract(Corner);
            float Twinkle = 0.5f + 0.5f * (float) Math.sin(Clock * 0.21 + Index * 2.3);
            float Edge = (float) Math.min(1.0, Math.min(Along / 0.6, (Usable - Along) / 0.6));
            float Alpha = MoteAlpha * Twinkle * Flicker * Math.max(0.0f, Edge);
            if (Alpha < 0.02f) {
                continue;
            }
            float Size = MoteSize * (0.7f + ((Hash >>> 40) & 3) * 0.2f);
            Vec3 Right = new Vec3(Sideways.x() * Size, Sideways.y() * Size, Sideways.z() * Size);
            Vec3 Up = new Vec3(Upward.x() * Size, Upward.y() * Size, Upward.z() * Size);
            float Shade = Additive ? Alpha * AdditiveScale * 1.6f : 1.0f;
            quad(Consumer, Last, Centre.subtract(Right).subtract(Up), Centre.add(Right).subtract(Up), Centre.add(Right).add(Up), Centre.subtract(Right).add(Up),
                    0.0f, 0.0625f, 0.0f, 0.0625f, BeamRed * Shade, BeamGreen * Shade, BeamBlue * Shade, Additive ? 1.0f : Alpha, LightTexture.FULL_BRIGHT, new Vec3(0.0, 1.0, 0.0), true);
        }
    }

    private static void gradient(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 A, Vec3 B, Vec3 C, Vec3 D, float U0, float U1, float V0, float V1, float NearAlpha, float FarAlpha,
                                 boolean Additive) {
        float NearShade = Additive ? NearAlpha * AdditiveScale : 1.0f;
        float FarShade = Additive ? FarAlpha * AdditiveScale : 1.0f;
        float NearA = Additive ? 1.0f : NearAlpha;
        float FarA = Additive ? 1.0f : FarAlpha;
        vertex(Consumer, Last, A, U0, V0, BeamRed * NearShade, BeamGreen * NearShade, BeamBlue * NearShade, NearA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, B, U1, V0, BeamRed * NearShade, BeamGreen * NearShade, BeamBlue * NearShade, NearA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, C, U1, V1, BeamRed * FarShade, BeamGreen * FarShade, BeamBlue * FarShade, FarA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, D, U0, V1, BeamRed * FarShade, BeamGreen * FarShade, BeamBlue * FarShade, FarA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, D, U0, V1, BeamRed * FarShade, BeamGreen * FarShade, BeamBlue * FarShade, FarA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, C, U1, V1, BeamRed * FarShade, BeamGreen * FarShade, BeamBlue * FarShade, FarA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, B, U1, V0, BeamRed * NearShade, BeamGreen * NearShade, BeamBlue * NearShade, NearA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, A, U0, V0, BeamRed * NearShade, BeamGreen * NearShade, BeamBlue * NearShade, NearA, LightTexture.FULL_BRIGHT, 0.0f, 1.0f, 0.0f);
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 A, Vec3 B, Vec3 C, Vec3 D, float U0, float U1, float V0, float V1, float Red, float Green, float Blue,
                             float Alpha, int Light, Vec3 Normal, boolean Both) {
        float Nx = (float) Normal.x;
        float Ny = (float) Normal.y;
        float Nz = (float) Normal.z;
        vertex(Consumer, Last, A, U0, V0, Red, Green, Blue, Alpha, Light, Nx, Ny, Nz);
        vertex(Consumer, Last, B, U1, V0, Red, Green, Blue, Alpha, Light, Nx, Ny, Nz);
        vertex(Consumer, Last, C, U1, V1, Red, Green, Blue, Alpha, Light, Nx, Ny, Nz);
        vertex(Consumer, Last, D, U0, V1, Red, Green, Blue, Alpha, Light, Nx, Ny, Nz);
        if (Both) {
            vertex(Consumer, Last, D, U0, V1, Red, Green, Blue, Alpha, Light, -Nx, -Ny, -Nz);
            vertex(Consumer, Last, C, U1, V1, Red, Green, Blue, Alpha, Light, -Nx, -Ny, -Nz);
            vertex(Consumer, Last, B, U1, V0, Red, Green, Blue, Alpha, Light, -Nx, -Ny, -Nz);
            vertex(Consumer, Last, A, U0, V0, Red, Green, Blue, Alpha, Light, -Nx, -Ny, -Nz);
        }
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float U, float V, float Red, float Green, float Blue, float Alpha, int Light,
                               float Nx, float Ny, float Nz) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z)
                .setColor(Red, Green, Blue, Alpha)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light)
                .setNormal(Last, Nx, Ny, Nz);
    }

    @Override
    public boolean shouldRenderOffScreen(Scp205LampBlockEntity Lamp) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(Scp205LampBlockEntity Lamp) {
        AABB Box = Wires.getRenderBoundingBox(Lamp);
        BlockPos Pos = Lamp.getBlockPos();
        BlockState State = Lamp.getBlockState();
        if (!State.hasProperty(Scp205LampBlock.Lit) || !State.getValue(Scp205LampBlock.Lit)) {
            return Box.minmax(new AABB(Pos.above()));
        }
        Direction Side = Scp205Beam.facing(State);
        Scp205Tilt Tilt = Scp205Beam.tilt(State);
        Vec3 End = Scp205Beam.origin(Pos, Side, Tilt).add(Scp205Beam.axis(Side, Tilt).scale(Lamp.beamLength()));
        double Reach = Scp205Beam.radiusAt(Lamp.beamLength()) * MaxStretch + 1.0;
        return Box.minmax(new AABB(Pos.above())).minmax(new AABB(End, End).inflate(Reach));
    }
}
