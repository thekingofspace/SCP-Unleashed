package net.rai.scp_unleashed.matrix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlock;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlockEntity;
import net.rai.scp_unleashed.rift.client.RiftRenderTypes;
import net.rai.scp_unleashed.shadercompat.client.RiftFallback;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MatrixRenderer implements BlockEntityRenderer<InstabilityMatrixBlockEntity> {
    public static final ResourceLocation LatticeTexture = texture("lattice");
    public static final ResourceLocation StrandTexture = texture("strand");
    public static final ResourceLocation HaloTexture = texture("halo");
    public static final ResourceLocation GemTexture = texture("gem");
    private static final float CoreY = (float) InstabilityMatrixBlockEntity.CoreHeight;
    private static final float OuterHalf = 2.2f / 16.0f;
    private static final float InnerHalf = 1.0f / 16.0f;
    private static final float StrutHalf = 0.22f / 16.0f;
    private static final float StrandHalf = 1.0f / 16.0f;
    private static final float NodeHalf = 0.5f / 16.0f;
    private static final float CornerTilt = 54.7356f;
    private static final float RingRadius = 5.1f / 16.0f;
    private static final float RingSecondRadius = 4.4f / 16.0f;
    private static final float RingHalf = 0.8f / 16.0f;
    private static final float RingBodyHalf = 0.3f / 16.0f;
    private static final float RingMetalRed = 0.62f;
    private static final float RingMetalGreen = 0.65f;
    private static final float RingMetalBlue = 0.7f;
    private static final float RingTilt = 24.0f;
    private static final int RingSegments = 32;
    private static final float EmitterOffset = 4.5f / 16.0f;
    private static final float GemLeft = 4.0f / 16.0f;
    private static final float GemRight = 12.0f / 16.0f;
    private static final float GemBottom = 10.0f / 16.0f;
    private static final float GemTop = 12.0f / 16.0f;
    private static final float GemLift = 0.3f / 16.0f;
    private static final float GemU = (GemRight - GemLeft);
    private static final float GemV = (GemTop - GemBottom);
    private static final float SeedMinHalf = 0.05f;
    private static final float SeedGrowth = 0.12f;
    private static final float SeedExtentX = 0.75f;
    private static final float SeedExtentY = 1.3f;
    private static final float SeedGlowX = 1.6f;
    private static final float SeedGlowY = 2.0f;
    private static final int SeedProfile = 4;
    private static final float Dormant = 0.42f;
    private static final int[][] CubeEdges = {{0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3}, {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}};

    public MatrixRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ResourceLocation texture(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/matrix/" + Name + ".png");
    }

    @Override
    public void render(InstabilityMatrixBlockEntity Matrix, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Matrix.getLevel();
        if (World == null) {
            return;
        }
        BlockPos Base = Matrix.getBlockPos();
        float Glow = Matrix.getGlow(PartialTick);
        float Resonance = Matrix.getShownResonance(PartialTick);
        float Flash = Matrix.getFlash(PartialTick);
        float Time = Matrix.getClientTicks() + PartialTick;
        float Beat = beat(Time, Resonance);
        float Red = Matrix.getTintRed();
        float Green = Matrix.getTintGreen();
        float Blue = Matrix.getTintBlue();
        int UpperLight = LevelRenderer.getLightColor(World, Base.above());
        float LatticeAngle = Matrix.getLatticeAngle(PartialTick);
        float Wobble = 4.0f * Mth.sin(LatticeAngle * Mth.DEG_TO_RAD * 0.1f) * Mth.clamp(Glow * 2.0f, 0.0f, 1.0f);
        Vector3f[] Outer = cube(cornerUp(LatticeAngle, Wobble), OuterHalf);
        Vector3f[] Inner = cube(cornerUp(-LatticeAngle * 1.7f + 30.0f, -Wobble), InnerHalf);
        float RingAngle = Matrix.getRingAngle(PartialTick);
        Pose.pushPose();
        Pose.translate(0.5f, CoreY, 0.5f);
        VertexConsumer Solid = Buffers.getBuffer(RenderType.entitySolid(LatticeTexture));
        latticeBody(Solid, Pose.last(), Outer, Inner, UpperLight, Glow, Red, Green, Blue);
        ringBodies(Solid, Pose, RingAngle, UpperLight);
        boolean ShadowPass = ShaderCompat.renderingShadowPass();
        if (Glow > 0.01f && !ShadowPass) {
            VertexConsumer Strands = Buffers.getBuffer(RenderType.eyes(StrandTexture));
            latticeGlow(Strands, Pose.last(), Outer, Inner, eyeLocal(Matrix), Glow, Beat, Flash, Red, Green, Blue);
            ringGlow(Strands, Pose, RingAngle, Glow, Beat, Flash, Resonance, Red, Green, Blue);
            halos(Pose, Buffers, Glow, Beat, Flash, Resonance, Red, Green, Blue);
        }
        Pose.popPose();
        if (Glow > 0.05f && RiftRenderTypes.available() && !ShadowPass) {
            seed(Matrix, Pose, Buffers, Glow, Resonance, Flash, Time, Red, Green, Blue);
        }
        gem(Matrix, Pose, Buffers, PackedLight, Glow, Beat, Red, Green, Blue);
    }

    private static float beat(float Time, float Resonance) {
        float Rate = 0.11f + 0.16f * Resonance;
        float Wave = 0.5f + 0.5f * Mth.sin(Time * Rate);
        return 0.78f + 0.22f * Wave * Wave;
    }

    private static Quaternionf cornerUp(float Spin, float Wobble) {
        return new Quaternionf().rotateY(Spin * Mth.DEG_TO_RAD).rotateX(Wobble * Mth.DEG_TO_RAD).rotateZ(CornerTilt * Mth.DEG_TO_RAD).rotateY(45.0f * Mth.DEG_TO_RAD);
    }

    private static Vector3f[] cube(Quaternionf Turn, float Half) {
        Vector3f[] Corners = new Vector3f[8];
        for (int Index = 0; Index < 8; Index++) {
            Vector3f Corner = new Vector3f((Index & 1) == 0 ? -Half : Half, (Index & 2) == 0 ? -Half : Half, (Index & 4) == 0 ? -Half : Half);
            Corners[Index] = Turn.transform(Corner);
        }
        return Corners;
    }

    private static void latticeBody(VertexConsumer Solid, PoseStack.Pose Last, Vector3f[] Outer, Vector3f[] Inner, int UpperLight, float Glow, float Red, float Green,
                                    float Blue) {
        float Lit = Mth.clamp(Glow * 1.6f, 0.0f, 1.0f);
        float BodyRed = Mth.lerp(Lit, Dormant, Mth.lerp(0.35f, Red, 1.0f));
        float BodyGreen = Mth.lerp(Lit, Dormant, Mth.lerp(0.35f, Green, 1.0f));
        float BodyBlue = Mth.lerp(Lit, Dormant * 1.08f, Mth.lerp(0.35f, Blue, 1.0f));
        int Light = Lit > 0.2f ? LightTexture.FULL_BRIGHT : UpperLight;
        for (int[] Edge : CubeEdges) {
            strut(Solid, Last, Outer[Edge[0]], Outer[Edge[1]], StrutHalf, BodyRed, BodyGreen, BodyBlue, Light);
            strut(Solid, Last, Inner[Edge[0]], Inner[Edge[1]], StrutHalf * 0.8f, BodyRed, BodyGreen, BodyBlue, Light);
        }
        for (int Index = 0; Index < 8; Index++) {
            strut(Solid, Last, Outer[Index], Inner[Index], StrutHalf * 0.7f, BodyRed * 0.92f, BodyGreen * 0.92f, BodyBlue * 0.92f, Light);
            node(Solid, Last, Outer[Index], NodeHalf, BodyRed, BodyGreen, BodyBlue, Light);
        }
    }

    private static void latticeGlow(VertexConsumer Strands, PoseStack.Pose Last, Vector3f[] Outer, Vector3f[] Inner, Vector3f Eye, float Glow, float Beat, float Flash,
                                    float Red, float Green, float Blue) {
        float Strength = Mth.clamp(Glow * Beat + Flash * 0.8f, 0.0f, 1.4f);
        for (int[] Edge : CubeEdges) {
            strand(Strands, Last, Outer[Edge[0]], Outer[Edge[1]], StrandHalf, Eye, Red, Green, Blue, Strength * 0.85f);
            strand(Strands, Last, Inner[Edge[0]], Inner[Edge[1]], StrandHalf * 0.8f, Eye, Red, Green, Blue, Strength);
        }
        for (int Index = 0; Index < 8; Index++) {
            strand(Strands, Last, Outer[Index], Inner[Index], StrandHalf * 0.7f, Eye, Red, Green, Blue, Strength * 0.6f);
        }
    }

    private static void ringFrame(PoseStack Pose, float Angle, boolean Second) {
        if (Second) {
            Pose.mulPose(Axis.YP.rotationDegrees(Angle * 0.25f + 90.0f));
            Pose.mulPose(Axis.XP.rotationDegrees(-RingTilt * 1.6f));
            Pose.mulPose(Axis.YP.rotationDegrees(-Angle * 1.25f));
        } else {
            Pose.mulPose(Axis.YP.rotationDegrees(Angle * 0.25f));
            Pose.mulPose(Axis.XP.rotationDegrees(RingTilt));
            Pose.mulPose(Axis.YP.rotationDegrees(Angle));
        }
    }

    private static void ringBodies(VertexConsumer Solid, PoseStack Pose, float Angle, int Light) {
        for (int Ring = 0; Ring < 2; Ring++) {
            boolean Second = Ring == 1;
            float Radius = Second ? RingSecondRadius : RingRadius;
            float Half = RingBodyHalf * (Second ? 0.85f : 1.0f);
            Pose.pushPose();
            ringFrame(Pose, Angle, Second);
            ringBand(Solid, Pose.last(), Radius, Half, Light);
            Pose.popPose();
        }
    }

    private static void ringBand(VertexConsumer Consumer, PoseStack.Pose Last, float Radius, float Half, int Light) {
        float Miter = Half / Mth.cos(Mth.PI / RingSegments);
        Vector3f Up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f Down = new Vector3f(0.0f, -1.0f, 0.0f);
        for (int Segment = 0; Segment < RingSegments; Segment++) {
            float Start = Segment * Mth.TWO_PI / RingSegments;
            float End = (Segment + 1) * Mth.TWO_PI / RingSegments;
            float Middle = (Start + End) * 0.5f;
            Vector3f Outward = new Vector3f(Mth.cos(Middle), 0.0f, Mth.sin(Middle));
            Vector3f[] From = bandCorners(Start, Radius, Miter, Half);
            Vector3f[] To = bandCorners(End, Radius, Miter, Half);
            Vector3f[] Normals = {Outward, Down, new Vector3f(Outward).negate(), Up};
            for (int Face = 0; Face < 4; Face++) {
                int Next = (Face + 1) % 4;
                facet(Consumer, Last, From[Face], From[Next], To[Next], To[Face], Normals[Face], Light);
            }
        }
    }

    private static Vector3f[] bandCorners(float Angle, float Radius, float Miter, float Half) {
        float Cos = Mth.cos(Angle);
        float Sin = Mth.sin(Angle);
        float Outer = Radius + Miter;
        float Inner = Radius - Miter;
        return new Vector3f[]{new Vector3f(Cos * Outer, Half, Sin * Outer), new Vector3f(Cos * Outer, -Half, Sin * Outer),
                new Vector3f(Cos * Inner, -Half, Sin * Inner), new Vector3f(Cos * Inner, Half, Sin * Inner)};
    }

    private static void facet(VertexConsumer Consumer, PoseStack.Pose Last, Vector3f A, Vector3f B, Vector3f C, Vector3f D, Vector3f Normal, int Light) {
        boolean Flip = new Vector3f(B).sub(A).cross(new Vector3f(C).sub(B)).dot(Normal) < 0.0f;
        Vector3f Second = Flip ? D : B;
        Vector3f Fourth = Flip ? B : D;
        solidVertex(Consumer, Last, A.x, A.y, A.z, 0.0f, 0.0f, RingMetalRed, RingMetalGreen, RingMetalBlue, Light, Normal);
        solidVertex(Consumer, Last, Second.x, Second.y, Second.z, Flip ? 1.0f : 0.0f, Flip ? 0.0f : 1.0f, RingMetalRed, RingMetalGreen, RingMetalBlue, Light, Normal);
        solidVertex(Consumer, Last, C.x, C.y, C.z, 1.0f, 1.0f, RingMetalRed, RingMetalGreen, RingMetalBlue, Light, Normal);
        solidVertex(Consumer, Last, Fourth.x, Fourth.y, Fourth.z, Flip ? 0.0f : 1.0f, Flip ? 1.0f : 0.0f, RingMetalRed, RingMetalGreen, RingMetalBlue, Light, Normal);
    }

    private static Vector3f eyeLocal(InstabilityMatrixBlockEntity Matrix) {
        Camera View = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        BlockPos Base = Matrix.getBlockPos();
        return new Vector3f((float) (Eye.x - Base.getX() - 0.5), (float) (Eye.y - Base.getY() - CoreY), (float) (Eye.z - Base.getZ() - 0.5));
    }

    private static void strut(VertexConsumer Consumer, PoseStack.Pose Last, Vector3f From, Vector3f To, float Half, float Red, float Green, float Blue, int Light) {
        Vector3f Along = new Vector3f(To).sub(From);
        float Length = Along.length();
        if (Length < 1.0e-4f) {
            return;
        }
        Along.div(Length);
        Vector3f Helper = Math.abs(Along.y) > 0.9f ? new Vector3f(1.0f, 0.0f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f Side = new Vector3f(Along).cross(Helper).normalize().mul(Half);
        Vector3f Lift = new Vector3f(Side).cross(Along).normalize().mul(Half);
        Vector3f[] Ring = {new Vector3f(Side).add(Lift), new Vector3f(Side).sub(Lift), new Vector3f(Side).negate().sub(Lift), new Vector3f(Side).negate().add(Lift)};
        for (int Face = 0; Face < 4; Face++) {
            Vector3f First = Ring[Face];
            Vector3f Second = Ring[(Face + 1) % 4];
            Vector3f Normal = new Vector3f(First).add(Second).normalize();
            solidVertex(Consumer, Last, From.x + First.x, From.y + First.y, From.z + First.z, 0.0f, 0.0f, Red, Green, Blue, Light, Normal);
            solidVertex(Consumer, Last, From.x + Second.x, From.y + Second.y, From.z + Second.z, 0.0f, 1.0f, Red, Green, Blue, Light, Normal);
            solidVertex(Consumer, Last, To.x + Second.x, To.y + Second.y, To.z + Second.z, 1.0f, 1.0f, Red, Green, Blue, Light, Normal);
            solidVertex(Consumer, Last, To.x + First.x, To.y + First.y, To.z + First.z, 1.0f, 0.0f, Red, Green, Blue, Light, Normal);
        }
    }

    private static void node(VertexConsumer Consumer, PoseStack.Pose Last, Vector3f Centre, float Half, float Red, float Green, float Blue, int Light) {
        for (Direction Side : Direction.values()) {
            Vector3f Normal = new Vector3f(Side.getStepX(), Side.getStepY(), Side.getStepZ());
            Vector3f Across = Side.getAxis() == Direction.Axis.Y ? new Vector3f(1.0f, 0.0f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
            Vector3f Other = new Vector3f(Normal).cross(Across);
            Vector3f Face = new Vector3f(Normal).mul(Half).add(Centre);
            Vector3f A = new Vector3f(Across).mul(Half);
            Vector3f B = new Vector3f(Other).mul(Half);
            solidVertex(Consumer, Last, Face.x - A.x - B.x, Face.y - A.y - B.y, Face.z - A.z - B.z, 0.0f, 0.0f, Red, Green, Blue, Light, Normal);
            solidVertex(Consumer, Last, Face.x + A.x - B.x, Face.y + A.y - B.y, Face.z + A.z - B.z, 1.0f, 0.0f, Red, Green, Blue, Light, Normal);
            solidVertex(Consumer, Last, Face.x + A.x + B.x, Face.y + A.y + B.y, Face.z + A.z + B.z, 1.0f, 1.0f, Red, Green, Blue, Light, Normal);
            solidVertex(Consumer, Last, Face.x - A.x + B.x, Face.y - A.y + B.y, Face.z - A.z + B.z, 0.0f, 1.0f, Red, Green, Blue, Light, Normal);
        }
    }

    private static void solidVertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Red, float Green, float Blue, int Light,
                                    Vector3f Normal) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Red, Green, Blue, 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light)
                .setNormal(Last, Normal.x, Normal.y, Normal.z);
    }

    private static void strand(VertexConsumer Consumer, PoseStack.Pose Last, Vector3f From, Vector3f To, float Half, Vector3f Eye, float Red, float Green, float Blue,
                               float Strength) {
        Vector3f Along = new Vector3f(To).sub(From);
        if (Along.lengthSquared() < 1.0e-8f) {
            return;
        }
        Vector3f Middle = new Vector3f(From).add(To).mul(0.5f);
        Vector3f Toward = new Vector3f(Eye).sub(Middle);
        Vector3f Side = new Vector3f(Along).cross(Toward);
        if (Side.lengthSquared() < 1.0e-8f) {
            return;
        }
        Side.normalize().mul(Half);
        float R = Red * Strength;
        float G = Green * Strength;
        float B = Blue * Strength;
        glowVertex(Consumer, Last, From.x - Side.x, From.y - Side.y, From.z - Side.z, 0.0f, 0.0f, R, G, B);
        glowVertex(Consumer, Last, From.x + Side.x, From.y + Side.y, From.z + Side.z, 0.0f, 1.0f, R, G, B);
        glowVertex(Consumer, Last, To.x + Side.x, To.y + Side.y, To.z + Side.z, 1.0f, 1.0f, R, G, B);
        glowVertex(Consumer, Last, To.x - Side.x, To.y - Side.y, To.z - Side.z, 1.0f, 0.0f, R, G, B);
        glowVertex(Consumer, Last, To.x - Side.x, To.y - Side.y, To.z - Side.z, 1.0f, 0.0f, R, G, B);
        glowVertex(Consumer, Last, To.x + Side.x, To.y + Side.y, To.z + Side.z, 1.0f, 1.0f, R, G, B);
        glowVertex(Consumer, Last, From.x + Side.x, From.y + Side.y, From.z + Side.z, 0.0f, 1.0f, R, G, B);
        glowVertex(Consumer, Last, From.x - Side.x, From.y - Side.y, From.z - Side.z, 0.0f, 0.0f, R, G, B);
    }

    private static void glowVertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Red, float Green, float Blue) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Mth.clamp(Red, 0.0f, 1.0f), Mth.clamp(Green, 0.0f, 1.0f), Mth.clamp(Blue, 0.0f, 1.0f), 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    private static void ringGlow(VertexConsumer Consumer, PoseStack Pose, float Angle, float Glow, float Beat, float Flash, float Resonance, float Red, float Green,
                                 float Blue) {
        float Strength = Mth.clamp(Glow * (0.7f + 0.5f * Beat) + Flash, 0.0f, 1.5f);
        Pose.pushPose();
        ringFrame(Pose, Angle, false);
        ring(Consumer, Pose.last(), RingRadius, RingHalf, Red, Green, Blue, Strength, Resonance);
        Pose.popPose();
        Pose.pushPose();
        ringFrame(Pose, Angle, true);
        ring(Consumer, Pose.last(), RingSecondRadius, RingHalf * 0.85f, Red, Green, Blue, Strength * 0.85f, Resonance);
        Pose.popPose();
    }

    private static void ring(VertexConsumer Consumer, PoseStack.Pose Last, float Radius, float Half, float Red, float Green, float Blue, float Strength, float Resonance) {
        for (int Segment = 0; Segment < RingSegments; Segment++) {
            float Start = Segment * Mth.TWO_PI / RingSegments;
            float End = (Segment + 1) * Mth.TWO_PI / RingSegments;
            float Pulse = 0.55f + 0.45f * Mth.sin(Start * 3.0f + Resonance * 6.0f);
            float Shade = Strength * (0.6f + 0.4f * Pulse);
            float R = Red * Shade;
            float G = Green * Shade;
            float B = Blue * Shade;
            float CosA = Mth.cos(Start);
            float SinA = Mth.sin(Start);
            float CosB = Mth.cos(End);
            float SinB = Mth.sin(End);
            float InnerA = Radius - Half;
            float OuterA = Radius + Half;
            glowVertex(Consumer, Last, CosA * InnerA, 0.0f, SinA * InnerA, 0.0f, 0.0f, R, G, B);
            glowVertex(Consumer, Last, CosA * OuterA, 0.0f, SinA * OuterA, 0.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * OuterA, 0.0f, SinB * OuterA, 1.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * InnerA, 0.0f, SinB * InnerA, 1.0f, 0.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * InnerA, 0.0f, SinB * InnerA, 1.0f, 0.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * OuterA, 0.0f, SinB * OuterA, 1.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosA * OuterA, 0.0f, SinA * OuterA, 0.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosA * InnerA, 0.0f, SinA * InnerA, 0.0f, 0.0f, R, G, B);
            glowVertex(Consumer, Last, CosA * Radius, -Half, SinA * Radius, 0.0f, 0.0f, R, G, B);
            glowVertex(Consumer, Last, CosA * Radius, Half, SinA * Radius, 0.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * Radius, Half, SinB * Radius, 1.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * Radius, -Half, SinB * Radius, 1.0f, 0.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * Radius, -Half, SinB * Radius, 1.0f, 0.0f, R, G, B);
            glowVertex(Consumer, Last, CosB * Radius, Half, SinB * Radius, 1.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosA * Radius, Half, SinA * Radius, 0.0f, 1.0f, R, G, B);
            glowVertex(Consumer, Last, CosA * Radius, -Half, SinA * Radius, 0.0f, 0.0f, R, G, B);
        }
    }

    private static void halos(PoseStack Pose, MultiBufferSource Buffers, float Glow, float Beat, float Flash, float Resonance, float Red, float Green, float Blue) {
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.eyes(HaloTexture));
        float Core = Mth.clamp(Glow * (0.35f + 0.65f * Resonance) * Beat + Flash, 0.0f, 1.3f);
        sprite(Pose, Consumer, 0.0f, 0.0f, 0.0f, (4.2f + 2.6f * Resonance + 3.0f * Flash) / 16.0f, Red, Green, Blue, Core * 0.75f);
        float Tips = Mth.clamp(Glow * Beat + Flash * 0.6f, 0.0f, 1.2f);
        sprite(Pose, Consumer, 0.0f, EmitterOffset, 0.0f, 1.6f / 16.0f, Red, Green, Blue, Tips);
        sprite(Pose, Consumer, 0.0f, -EmitterOffset, 0.0f, 1.6f / 16.0f, Red, Green, Blue, Tips);
    }

    private static void sprite(PoseStack Pose, VertexConsumer Consumer, float X, float Y, float Z, float Radius, float Red, float Green, float Blue, float Strength) {
        Pose.pushPose();
        Pose.translate(X, Y, Z);
        Pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose Last = Pose.last();
        float R = Mth.lerp(0.25f, Red, 1.0f) * Strength;
        float G = Mth.lerp(0.25f, Green, 1.0f) * Strength;
        float B = Mth.lerp(0.25f, Blue, 1.0f) * Strength;
        glowVertex(Consumer, Last, -Radius, -Radius, 0.0f, 0.0f, 1.0f, R, G, B);
        glowVertex(Consumer, Last, Radius, -Radius, 0.0f, 1.0f, 1.0f, R, G, B);
        glowVertex(Consumer, Last, Radius, Radius, 0.0f, 1.0f, 0.0f, R, G, B);
        glowVertex(Consumer, Last, -Radius, Radius, 0.0f, 0.0f, 0.0f, R, G, B);
        Pose.popPose();
    }

    private static void seed(InstabilityMatrixBlockEntity Matrix, PoseStack Pose, MultiBufferSource Buffers, float Glow, float Resonance, float Flash, float Time,
                             float Red, float Green, float Blue) {
        Camera View = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        Vec3 Centre = Matrix.getCoreCentre();
        double OffsetX = Centre.x - Eye.x;
        double OffsetZ = Centre.z - Eye.z;
        double Length = Math.sqrt(OffsetX * OffsetX + OffsetZ * OffsetZ);
        float DepthX = Length > 0.001 ? (float) (OffsetX / Length) : 0.0f;
        float DepthZ = Length > 0.001 ? (float) (OffsetZ / Length) : 1.0f;
        float AcrossX = -DepthZ;
        float AcrossZ = DepthX;
        float Open = Mth.clamp((0.25f + 0.75f * Resonance) * Glow + Flash * 0.5f, 0.0f, 1.0f);
        float Half = SeedMinHalf + SeedGrowth * Resonance * Resonance + 0.06f * Flash;
        int Colour = (Mth.clamp(Math.round(Open * 255.0f), 0, 255) << 24)
                | (Mth.clamp(Math.round(Red * 255.0f), 0, 255) << 16) | (Mth.clamp(Math.round(Green * 255.0f), 0, 255) << 8) | Mth.clamp(Math.round(Blue * 255.0f), 0, 255);
        int Pull = Mth.clamp(Math.round((0.15f + 0.6f * Resonance) * 1000.0f), 0, 1000);
        int Implosion = Mth.clamp(Math.round(Flash * 700.0f), 0, 1000);
        int Variant = (int) (Matrix.getBlockPos().asLong() & 4095L);
        int Profile = SeedProfile + 16 * Mth.clamp(Math.round(Half * 100.0f), 5, 2000);
        float Wander = Mth.sin(Time * 0.37f) * 0.004f;
        PoseStack.Pose Last = Pose.last();
        if (RiftRenderTypes.fallback()) {
            RiftFallback.draw(Last, Buffers, 0.5f + AcrossX * Wander, (float) InstabilityMatrixBlockEntity.CoreHeight, 0.5f + AcrossZ * Wander, AcrossX, AcrossZ, Half, Colour,
                    Pull, Implosion, Variant, Profile);
            return;
        }
        SeedQuad Quad = new SeedQuad(Last, 0.5f + AcrossX * Wander, (float) InstabilityMatrixBlockEntity.CoreHeight, 0.5f + AcrossZ * Wander, AcrossX, AcrossZ, Half, Colour,
                Pull, Implosion, Variant, Profile, DepthX, DepthZ);
        Quad.draw(Buffers.getBuffer(RiftRenderTypes.Core), SeedExtentX, SeedExtentY);
        Quad.draw(Buffers.getBuffer(RiftRenderTypes.Glow), SeedGlowX, SeedGlowY);
    }

    private static void gem(InstabilityMatrixBlockEntity Matrix, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, float Glow, float Beat, float Red, float Green,
                            float Blue) {
        Direction Front = Matrix.getBlockState().getValue(InstabilityMatrixBlock.Facing);
        float Turn = switch (Front) {
            case EAST -> -90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 90.0f;
            default -> 0.0f;
        };
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(Turn));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        PoseStack.Pose Last = Pose.last();
        float Lit = Mth.clamp(0.3f + Glow * Beat, 0.0f, 1.0f);
        boolean Bright = Glow > 0.05f;
        VertexConsumer Consumer = Buffers.getBuffer(Bright ? RenderType.entityTranslucentEmissive(GemTexture) : RenderType.entityCutout(GemTexture));
        int Light = Bright ? LightTexture.FULL_BRIGHT : PackedLight;
        float R = Mth.lerp(Bright ? Lit : 0.0f, Red * 0.45f, Red);
        float G = Mth.lerp(Bright ? Lit : 0.0f, Green * 0.45f, Green);
        float B = Mth.lerp(Bright ? Lit : 0.0f, Blue * 0.45f, Blue);
        Vector3f Normal = new Vector3f(0.0f, 0.0f, -1.0f);
        solidVertex(Consumer, Last, GemRight, GemTop, -GemLift, 0.0f, 0.0f, R, G, B, Light, Normal);
        solidVertex(Consumer, Last, GemRight, GemBottom, -GemLift, 0.0f, GemV, R, G, B, Light, Normal);
        solidVertex(Consumer, Last, GemLeft, GemBottom, -GemLift, GemU, GemV, R, G, B, Light, Normal);
        solidVertex(Consumer, Last, GemLeft, GemTop, -GemLift, GemU, 0.0f, R, G, B, Light, Normal);
        Pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(InstabilityMatrixBlockEntity Matrix) {
        BlockPos Base = Matrix.getBlockPos();
        return new AABB(Base.getX() - 0.25, Base.getY(), Base.getZ() - 0.25, Base.getX() + 1.25, Base.getY() + 2.25, Base.getZ() + 1.25);
    }

    private record SeedQuad(PoseStack.Pose Last, float CentreX, float CentreY, float CentreZ, float AcrossX, float AcrossZ, float Half, int Colour,
                            int Pull, int Implosion, int Variant, int Profile, float DepthX, float DepthZ) {
        void draw(VertexConsumer Consumer, float ExtentX, float ExtentY) {
            corner(Consumer, -ExtentX, -ExtentY);
            corner(Consumer, ExtentX, -ExtentY);
            corner(Consumer, ExtentX, ExtentY);
            corner(Consumer, -ExtentX, ExtentY);
        }

        private void corner(VertexConsumer Consumer, float U, float V) {
            Consumer.addVertex(Last, CentreX + AcrossX * U * Half, CentreY + V * Half, CentreZ + AcrossZ * U * Half)
                    .setColor(Colour)
                    .setUv(U, V)
                    .setUv1(Pull, Implosion)
                    .setUv2(Variant, Profile)
                    .setNormal(DepthX, 0.0f, DepthZ);
        }
    }
}
