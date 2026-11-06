package net.rai.scp_unleashed.scp001scarlet.king.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletArena;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKing;
import org.joml.Matrix4f;

public class ScarletKingRenderer extends SharedGeoRenderer<ScarletKing> {
    public static final float Scale = 3.0f;
    public static final float ShadowRadius = 6.0f;
    public static final double LinkPitch = 1.4;
    public static final double CoreWide = 0.2;
    public static final double CoreThin = 0.1;
    public static final double HaloHalf = 0.5;

    public ScarletKingRenderer(EntityRendererProvider.Context Context) {
        super(Context, new ScarletKingGeoModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
        addRenderLayer(new ScarletKingGlowLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(ScarletKing Owner) {
        return 0.0f;
    }

    @Override
    public int getPackedOverlay(ScarletKing Owner, float U, float PartialTick) {
        if (Owner.isDeadOrDying()) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(Owner, U, PartialTick);
    }

    @Override
    public void render(ScarletKing Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
        Vec3 Base = new Vec3(Mth.lerp(PartialTick, Owner.xo, Owner.getX()), Mth.lerp(PartialTick, Owner.yo, Owner.getY()), Mth.lerp(PartialTick, Owner.zo, Owner.getZ()));
        Vec3 Eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(Base);
        VertexConsumer Consumer = Buffers.getBuffer(ScarletGlowTypes.Glow);
        Matrix4f Matrix = Pose.last().pose();
        float Time = Owner.tickCount + PartialTick;
        int Action = Owner.isDeadOrDying() ? ScarletKing.Dying : Owner.action();
        float Since = Owner.actionAge(PartialTick);
        bands(Owner, Consumer, Matrix, Base, Eye, Time, PartialTick, Action);
        if (Action == ScarletKing.Dying && Owner.deathTime < ScarletKing.SlackTicks) {
            slack(Owner, Consumer, Matrix, Base, Eye, Owner.deathTime + PartialTick, PartialTick);
        }
        rings(Owner, Consumer, Matrix, Time);
        if (Owner.warded()) {
            ward(Owner, Consumer, Matrix, Time);
        }
        if (Owner.beamState() > 0) {
            beam(Owner, Consumer, Matrix, Base, Eye, Time);
        }
        if (Action == ScarletKing.Slam && Since < 22.0f) {
            Vec3 Spot = Owner.spot().subtract(Base).add(0.0, 0.06, 0.0);
            float Pulse = 0.35f + 0.25f * Mth.sin(Time * 0.8f);
            ScarletGlowDraw.groundRing(Consumer, Matrix, Spot, ScarletKing.GrabRadius, 0.35, 32, 0.95f, 0.12f, 0.08f, Pulse);
            ScarletGlowDraw.groundRing(Consumer, Matrix, Spot, ScarletKing.GrabRadius * Math.min(1.0, Since / 22.0), 0.2, 32, 1.0f, 0.5f, 0.3f, 0.5f);
        }
        if (Action == ScarletKing.Lash && Since < 19.0f) {
            lashSector(Owner, Consumer, Matrix, Since);
        }
        if (Action == ScarletKing.Nova && Since < 35.0f) {
            float Charge = Since / 35.0f;
            ScarletGlowDraw.groundRing(Consumer, Matrix, new Vec3(0.0, 0.08, 0.0), ScarletKing.NovaStart + 1.0, 0.6, 48, 1.0f, 0.4f, 0.12f, 0.2f + 0.5f * Charge);
        }
    }

    private void bands(ScarletKing Owner, VertexConsumer Consumer, Matrix4f Matrix, Vec3 Base, Vec3 Eye, float Time, float Partial, int Action) {
        ScarletArena Where = Owner.arena();
        int Shown = Owner.shown();
        int Intact = Owner.bands();
        if (Where == null || Shown <= 0 || Action == ScarletKing.Dying) {
            return;
        }
        boolean Strained = Action >= ScarletKing.Lash && Action <= ScarletKing.Shades;
        float Strain = (Strained ? 1.0f : 0.8f) + 0.04f * (ScarletArena.Count - Integer.bitCount(Intact));
        for (int Band = 0; Band < Math.min(Shown, ScarletArena.Count); Band++) {
            if ((Intact & 1 << Band) == 0) {
                continue;
            }
            chain(Consumer, Matrix, Owner.bindPoint(Band, Partial).subtract(Base), Where.anchor(Band).subtract(Base), 0.04, Time, Band, Strain, false);
        }
    }

    private void chain(VertexConsumer Consumer, Matrix4f Matrix, Vec3 From, Vec3 To, double SagShare, float Time, int Band, float Bright, boolean Floor) {
        Vec3 Span = To.subtract(From);
        double Length = Span.length();
        if (Length < 1.0E-3) {
            return;
        }
        int Links = Math.max(2, (int) (Length / LinkPitch));
        Vec3[] Frame = ScarletGlowDraw.frame(Span);
        Vec3 Previous = From;
        for (int Link = 1; Link <= Links; Link++) {
            double Share = Link / (double) Links;
            Vec3 Point = From.add(Span.scale(Share)).add(0.0, -Math.sin(Share * Math.PI) * Length * SagShare, 0.0);
            if (Floor) {
                Point = new Vec3(Point.x, Math.max(0.1, Point.y), Point.z);
            }
            boolean Wide = Link % 2 == 0;
            float Shimmer = 0.75f + 0.25f * Mth.sin(Time * 0.35f - Link * 0.6f + Band * 1.3f);
            float Core = Mth.clamp(0.5f * Shimmer * Bright, 0.0f, 1.0f);
            float Halo = Mth.clamp(0.1f * Shimmer * Bright, 0.0f, 1.0f);
            ScarletGlowDraw.prism(Consumer, Matrix, Previous, Point, Frame[0], Frame[1], Wide ? CoreWide : CoreThin, 1.0f, 0.55f, 0.38f, Core);
            ScarletGlowDraw.prism(Consumer, Matrix, Previous, Point, Frame[0], Frame[1], HaloHalf, 1.0f, 0.42f, 0.3f, Halo);
            Previous = Point;
        }
    }

    private void slack(ScarletKing Owner, VertexConsumer Consumer, Matrix4f Matrix, Vec3 Base, Vec3 Eye, float Dying, float Partial) {
        ScarletArena Where = Owner.arena();
        int Intact = Owner.bands();
        if (Where == null || Intact == 0) {
            return;
        }
        float Share = Mth.clamp(Dying / ScarletKing.SlackTicks, 0.0f, 1.0f);
        float Fade = 1.0f - Share * Share;
        for (int Band = 0; Band < Math.min(Owner.shown(), ScarletArena.Count); Band++) {
            if ((Intact & 1 << Band) == 0) {
                continue;
            }
            chain(Consumer, Matrix, Owner.bindPoint(Band, Partial).subtract(Base), Where.anchor(Band).subtract(Base), 0.04 + 0.5 * Share, Owner.tickCount + Partial, Band,
                    Fade, true);
        }
    }

    private void rings(ScarletKing Owner, VertexConsumer Consumer, Matrix4f Matrix, float Time) {
        for (int Start : new int[]{Owner.novaTick(), Owner.secondNovaTick()}) {
            float Age = Owner.tickCount - Start + Owner.renderPartial();
            if (Age < 0.0f || Start < 0) {
                continue;
            }
            double Radius = ScarletKing.NovaStart + Age * ScarletKing.NovaSpeed;
            if (Radius > ScarletKing.NovaReach) {
                continue;
            }
            float Fade = (float) (1.0 - Radius / ScarletKing.NovaReach);
            int Segments = Math.max(48, (int) (Radius * 4.0));
            ScarletGlowDraw.wall(Consumer, Matrix, Vec3.ZERO, Radius, 0.0, 1.3, Segments, 1.0f, 0.32f, 0.08f, 0.55f * Fade);
            ScarletGlowDraw.wall(Consumer, Matrix, Vec3.ZERO, Radius - 0.4, 0.0, 0.6, Segments, 1.0f, 0.75f, 0.4f, 0.5f * Fade);
            ScarletGlowDraw.groundRing(Consumer, Matrix, new Vec3(0.0, 0.05, 0.0), Radius, 1.2, Segments, 1.0f, 0.2f, 0.05f, 0.3f * Fade);
        }
    }

    private void ward(ScarletKing Owner, VertexConsumer Consumer, Matrix4f Matrix, float Time) {
        float Hit = Math.max(0.0f, 1.0f - (Owner.tickCount - Owner.wardHitTick()) / 6.0f);
        double Radius = ScarletKing.WardRadius;
        int Segments = 40;
        for (int Ring = 0; Ring < 12; Ring++) {
            double Low = Ring * 2.3;
            double High = Low + 2.3;
            double RadiusLow = Radius * dome(Low);
            double RadiusHigh = Radius * dome(High);
            for (int Index = 0; Index < Segments; Index++) {
                double A0 = Index * Math.PI * 2.0 / Segments + Time * 0.004;
                double A1 = (Index + 1) * Math.PI * 2.0 / Segments + Time * 0.004;
                Vec3 P0 = new Vec3(Math.cos(A0) * RadiusLow, Low, Math.sin(A0) * RadiusLow);
                Vec3 P1 = new Vec3(Math.cos(A1) * RadiusLow, Low, Math.sin(A1) * RadiusLow);
                Vec3 P2 = new Vec3(Math.cos(A1) * RadiusHigh, High, Math.sin(A1) * RadiusHigh);
                Vec3 P3 = new Vec3(Math.cos(A0) * RadiusHigh, High, Math.sin(A0) * RadiusHigh);
                float Alpha = 0.07f + 0.12f * Hit;
                boolean Glyph = (Ring == 3 || Ring == 6 || Ring == 9) && (Index * 7 + Ring) % 5 < 2;
                ScarletGlowDraw.quad(Consumer, Matrix, P0, P1, P2, P3, 0.9f, 0.08f, 0.1f, Glyph ? Alpha + 0.3f : Alpha);
            }
        }
    }

    private static double dome(double Height) {
        double Top = 27.0;
        double Shoulder = 16.0;
        if (Height <= Shoulder) {
            return 1.0;
        }
        double Share = Math.min(1.0, (Height - Shoulder) / (Top - Shoulder));
        return Math.sqrt(Math.max(0.02, 1.0 - Share * Share));
    }

    private void beam(ScarletKing Owner, VertexConsumer Consumer, Matrix4f Matrix, Vec3 Base, Vec3 Eye, float Time) {
        Vec3 Halo = Owner.local(ScarletKing.HaloRest).subtract(Owner.position());
        Vec3 Point = Owner.beamPoint().subtract(Base);
        if (Owner.beamState() == 1) {
            float Pulse = 0.4f + 0.3f * Mth.sin(Time * 0.9f);
            ScarletGlowDraw.ribbon(Consumer, Matrix, Halo, Point, Eye, 0.12f, 1.0f, 0.3f, 0.2f, Pulse);
            ScarletGlowDraw.groundRing(Consumer, Matrix, Point.add(0.0, 0.06, 0.0), 1.6, 0.25, 24, 1.0f, 0.2f, 0.1f, Pulse);
            return;
        }
        ScarletGlowDraw.ribbon(Consumer, Matrix, Halo, Point, Eye, 2.2f, 1.0f, 0.25f, 0.08f, 0.25f);
        ScarletGlowDraw.ribbon(Consumer, Matrix, Halo, Point, Eye, 0.9f, 1.0f, 0.55f, 0.25f, 0.6f);
        ScarletGlowDraw.ribbon(Consumer, Matrix, Halo, Point, Eye, 0.3f, 1.0f, 0.95f, 0.8f, 0.9f);
        ScarletGlowDraw.groundRing(Consumer, Matrix, Point.add(0.0, 0.06, 0.0), 2.0, 1.0, 24, 1.0f, 0.4f, 0.1f, 0.5f);
    }

    private void lashSector(ScarletKing Owner, VertexConsumer Consumer, Matrix4f Matrix, float Since) {
        Vec3 Forward = Owner.forward();
        double Base = Math.atan2(Forward.z, Forward.x);
        float Alpha = 0.25f + 0.3f * (Since / 19.0f);
        for (int Side = -1; Side <= 1; Side += 2) {
            double Angle = Base + Side * Math.toRadians(ScarletKing.LashHalfAngle);
            Vec3 Direction = new Vec3(Math.cos(Angle), 0.0, Math.sin(Angle));
            Vec3 Across = new Vec3(-Direction.z, 0.0, Direction.x).scale(0.25);
            Vec3 Near = Direction.scale(ScarletKing.LashInner).add(0.0, 0.07, 0.0);
            Vec3 Far = Direction.scale(ScarletKing.LashOuter).add(0.0, 0.07, 0.0);
            ScarletGlowDraw.quad(Consumer, Matrix, Near.subtract(Across), Near.add(Across), Far.add(Across), Far.subtract(Across), 0.95f, 0.1f, 0.08f, Alpha);
        }
        int Steps = 24;
        for (int Index = 0; Index < Steps; Index++) {
            double A0 = Base + Math.toRadians(-ScarletKing.LashHalfAngle + 2.0 * ScarletKing.LashHalfAngle * Index / Steps);
            double A1 = Base + Math.toRadians(-ScarletKing.LashHalfAngle + 2.0 * ScarletKing.LashHalfAngle * (Index + 1) / Steps);
            double Outer = ScarletKing.LashOuter;
            Vec3 P0 = new Vec3(Math.cos(A0) * Outer, 0.07, Math.sin(A0) * Outer);
            Vec3 P1 = new Vec3(Math.cos(A1) * Outer, 0.07, Math.sin(A1) * Outer);
            Vec3 P2 = new Vec3(Math.cos(A1) * (Outer - 0.5), 0.07, Math.sin(A1) * (Outer - 0.5));
            Vec3 P3 = new Vec3(Math.cos(A0) * (Outer - 0.5), 0.07, Math.sin(A0) * (Outer - 0.5));
            ScarletGlowDraw.quad(Consumer, Matrix, P0, P1, P2, P3, 0.95f, 0.1f, 0.08f, Alpha);
        }
    }

    public static Vec3 pillarTop(BlockPos Pillar) {
        return Vec3.atCenterOf(Pillar);
    }
}
