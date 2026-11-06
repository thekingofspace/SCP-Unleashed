package net.rai.scp_unleashed.scp2317.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp2317.Scp2317Chains;
import net.rai.scp_unleashed.scp2317.Scp2317KingBlockEntity;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.ArrayList;
import java.util.List;

public class Scp2317KingRenderer extends GeoBlockRenderer<Scp2317KingBlockEntity> {
    public static final ResourceLocation LinkTexture = Scp2317Registry.location("textures/entity/scp2317/chain_iron.png");
    public static final double LinkLength = 0.42;
    public static final double LinkWidth = 0.26;
    public static final double LinkBar = 0.09;
    public static final double LinkStep = 0.32;
    public static final double CollarSize = 0.42;
    public static final double CollarDepth = 0.16;
    public static final double CuffSize = 0.3;
    public static final double CuffDepth = 0.12;
    public static final double Sag = 0.06;
    public static final int Samples = 24;
    public static final double HangPillar = 4.0;
    public static final double HangBody = 2.0;
    public static final int MinimumLight = 7;

    public Scp2317KingRenderer(BlockEntityRendererProvider.Context Context) {
        super(new Scp2317KingGeoModel());
        withScale(Scp2317Chains.ModelScale);
    }

    @Override
    public void render(Scp2317KingBlockEntity King, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        super.render(King, PartialTick, Pose, Buffers, PackedLight, PackedOverlay);
        float Time = King.getLevel() == null ? 0.0f : King.getLevel().getGameTime() + PartialTick;
        int Light = LightTexture.pack(Math.max(MinimumLight, LightTexture.block(PackedLight)), LightTexture.sky(PackedLight));
        VertexConsumer Sink = Buffers.getBuffer(RenderType.entityCutoutNoCull(LinkTexture));
        Matrix4f Matrix = Pose.last().pose();
        for (int Index = 0; Index < Scp2317Chains.count(); Index++) {
            Vec3 Body = Scp2317Chains.attach(Index);
            Vec3 Anchor = Scp2317Chains.anchor(Index);
            Vec3 Toward = new Vec3(0.5 - Anchor.x, 0.0, 0.5 - Anchor.z);
            collar(Sink, Matrix, Pose, Anchor, Toward, CollarSize, CollarDepth, Light);
            collar(Sink, Matrix, Pose, Body, Anchor.subtract(Body), CuffSize, CuffDepth, Light);
            if (Index < King.broken()) {
                links(Sink, Matrix, Pose, dangling(Anchor, Math.min(HangPillar, Anchor.y - 0.3), Time, Index), Light);
                links(Sink, Matrix, Pose, dangling(Body, Math.min(HangBody, Math.max(0.4, Body.y + 0.2)), Time, Index + 7), Light);
            } else {
                links(Sink, Matrix, Pose, sagged(Anchor, Body), Light);
            }
        }
    }

    public static List<Vec3> sagged(Vec3 From, Vec3 To) {
        List<Vec3> Points = new ArrayList<>();
        double Length = From.distanceTo(To);
        for (int Index = 0; Index <= Samples; Index++) {
            double Share = (double) Index / Samples;
            Vec3 Point = From.lerp(To, Share);
            Points.add(Point.add(0.0, -4.0 * Sag * Length * Share * (1.0 - Share), 0.0));
        }
        return Points;
    }

    public static List<Vec3> dangling(Vec3 From, double Length, float Time, int Seed) {
        List<Vec3> Points = new ArrayList<>();
        int Steps = 10;
        double Sway = Mth.sin(Time * 0.03f + Seed * 1.7f) * 0.12;
        for (int Index = 0; Index <= Steps; Index++) {
            double Share = (double) Index / Steps;
            Points.add(From.add(Sway * Share * Share, -Length * Share, Sway * 0.6 * Share * Share));
        }
        return Points;
    }

    public static Vec3[] frame(Vec3 Direction) {
        Vec3 Along = Direction.normalize();
        Vec3 Helper = Math.abs(Along.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 Side = Along.cross(Helper).normalize();
        Vec3 Other = Along.cross(Side).normalize();
        return new Vec3[]{Along, Side, Other};
    }

    public static void collar(VertexConsumer Sink, Matrix4f Matrix, PoseStack Pose, Vec3 Point, Vec3 Toward, double Size, double Depth, int Light) {
        if (Toward.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] Axes = frame(Toward);
        box(Sink, Matrix, Pose, Point, Axes, new double[]{Depth, Size, Size}, Light);
        box(Sink, Matrix, Pose, Point.add(Axes[0].scale(Depth + 0.05)), Axes, new double[]{0.05, Size * 0.5, Size * 0.5}, Light);
    }

    public static void links(VertexConsumer Sink, Matrix4f Matrix, PoseStack Pose, List<Vec3> Path, int Light) {
        double Total = 0.0;
        for (int Index = 0; Index < Path.size() - 1; Index++) {
            Total += Path.get(Index).distanceTo(Path.get(Index + 1));
        }
        int Count = Math.max(1, (int) (Total / LinkStep));
        for (int Link = 0; Link < Count; Link++) {
            double Travel = (Link + 0.5) * Total / Count;
            double Walked = 0.0;
            Vec3 Centre = Path.get(0);
            Vec3 Direction = new Vec3(0.0, -1.0, 0.0);
            for (int Index = 0; Index < Path.size() - 1; Index++) {
                Vec3 A = Path.get(Index);
                Vec3 B = Path.get(Index + 1);
                double Length = A.distanceTo(B);
                if (Walked + Length >= Travel || Index == Path.size() - 2) {
                    double Share = Length < 1.0E-6 ? 0.0 : (Travel - Walked) / Length;
                    Centre = A.lerp(B, Share);
                    Direction = Length < 1.0E-6 ? Direction : B.subtract(A).scale(1.0 / Length);
                    break;
                }
                Walked += Length;
            }
            Vec3[] Axes = frame(Direction);
            Vec3 Across = Link % 2 == 0 ? Axes[1] : Axes[2];
            Vec3 Normal = Axes[0].cross(Across);
            Vec3[] Local = {Axes[0], Across, Normal};
            for (int Sign = -1; Sign <= 1; Sign += 2) {
                box(Sink, Matrix, Pose, Centre.add(Across.scale(Sign * (LinkWidth - LinkBar) / 2.0)), Local, new double[]{LinkLength / 2.0, LinkBar / 2.0, LinkBar / 2.0}, Light);
                box(Sink, Matrix, Pose, Centre.add(Axes[0].scale(Sign * (LinkLength - LinkBar) / 2.0)), Local, new double[]{LinkBar / 2.0, LinkWidth / 2.0 - LinkBar, LinkBar / 2.0}, Light);
            }
        }
    }

    public static void box(VertexConsumer Sink, Matrix4f Matrix, PoseStack Pose, Vec3 Centre, Vec3[] Axes, double[] Half, int Light) {
        for (int Axis = 0; Axis < 3; Axis++) {
            for (int Sign = -1; Sign <= 1; Sign += 2) {
                Vec3 Normal = Axes[Axis].scale(Sign);
                Vec3 A = Axes[(Axis + 1) % 3].scale(Half[(Axis + 1) % 3]);
                Vec3 B = Axes[(Axis + 2) % 3].scale(Half[(Axis + 2) % 3]);
                Vec3 Middle = Centre.add(Normal.scale(Half[Axis]));
                Vec3 C0 = Middle.subtract(A).subtract(B);
                Vec3 C1 = Middle.add(A).subtract(B);
                Vec3 C2 = Middle.add(A).add(B);
                Vec3 C3 = Middle.subtract(A).add(B);
                if (C1.subtract(C0).cross(C3.subtract(C0)).dot(Normal) < 0.0) {
                    quad(Sink, Matrix, Pose, C3, C2, C1, C0, Normal, Light);
                } else {
                    quad(Sink, Matrix, Pose, C0, C1, C2, C3, Normal, Light);
                }
            }
        }
    }

    private static void quad(VertexConsumer Sink, Matrix4f Matrix, PoseStack Pose, Vec3 C0, Vec3 C1, Vec3 C2, Vec3 C3, Vec3 Normal, int Light) {
        vertex(Sink, Matrix, Pose, C0, 0.0f, 1.0f, Normal, Light);
        vertex(Sink, Matrix, Pose, C1, 1.0f, 1.0f, Normal, Light);
        vertex(Sink, Matrix, Pose, C2, 1.0f, 0.0f, Normal, Light);
        vertex(Sink, Matrix, Pose, C3, 0.0f, 0.0f, Normal, Light);
    }

    private static void vertex(VertexConsumer Sink, Matrix4f Matrix, PoseStack Pose, Vec3 Point, float U, float V, Vec3 Normal, int Light) {
        Sink.addVertex(Matrix, (float) Point.x, (float) Point.y, (float) Point.z).setColor(255, 255, 255, 255).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light)
                .setNormal(Pose.last(), (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }

    @Override
    public boolean shouldRenderOffScreen(Scp2317KingBlockEntity King) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(Scp2317KingBlockEntity King) {
        return King.renderBounds();
    }
}
