package net.rai.scp_unleashed.scp001gate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001gate.Scp001GateBlade;
import net.rai.scp_unleashed.scp001gate.Scp001GateGuardian;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

public class Scp001GateRenderer extends GeoEntityRenderer<Scp001GateGuardian> {
    public static final int TrailPoints = 14;
    public static final float SweepStart = 3.0f;
    public static final float FadeEnd = 16.0f;
    public static final int FireFrames = 16;
    public static final int FireTicks = 2;
    public static final int FireTiles = 3;
    public static final float FireHalfWidth = 1.15f;
    public static final double SparkReach = 64.0;
    public static final int BladeSparks = 3;
    public static final int FlaringSparks = 8;
    private final Matrix4f BladePose = new Matrix4f();
    private boolean Known;
    private long LastSpark = -1;

    public Scp001GateRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp001GateGeoModel());
        addRenderLayer(new Scp001GateGlowLayer(this));
        withScale(Scp001GateGuardian.RenderScale);
        shadowRadius = 0.0f;
    }

    @Override
    public boolean shouldRender(Scp001GateGuardian Owner, Frustum View, double X, double Y, double Z) {
        return View.isVisible(Owner.getBoundingBoxForCulling());
    }

    @Override
    public void renderFinal(PoseStack Pose, Scp001GateGuardian Owner, BakedGeoModel Model, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer, float Partial, int Light,
                            int Overlay, int Colour) {
        super.renderFinal(Pose, Owner, Model, Buffers, Buffer, Partial, Light, Overlay, Colour);
        Optional<GeoBone> Found = Model.getBone(Scp001GateBlade.Bone);
        if (Found.isPresent() && Found.get().isTrackingMatrices()) {
            BladePose.set(Scp001GateBlade.pose(Scp001GateBlade.placement(Owner.facing(Partial), Scp001GateGuardian.RenderScale), Found.get().getModelSpaceMatrix()));
            Known = true;
        }
    }

    @Override
    public void render(Scp001GateGuardian Owner, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Known = false;
        super.render(Owner, Yaw, Partial, Pose, Buffers, Light);
        if (ShaderCompat.renderingShadowPass() || !Known) {
            return;
        }
        Vec3 Origin = Owner.getPosition(Partial);
        Vec3 From = Scp001GateBlade.guard(BladePose);
        Vec3 Edge = Scp001GateBlade.tip(BladePose);
        sparks(Owner, Origin);
        Vec3 Eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(Origin);
        bladeFire(Owner, Pose, Buffers, Eye, From, Edge, Partial);
        float Age = Owner.strikeTime(Partial);
        if (Age < SweepStart || Age > FadeEnd || Owner.strikeStart() < 0) {
            return;
        }
        Vec3 Target = Owner.aim().subtract(Origin);
        float Reach = Mth.clamp((Age - SweepStart) / (Scp001GateGuardian.StrikeImpact - SweepStart), 0.0f, 1.0f);
        float Alpha = Age <= Scp001GateGuardian.StrikeImpact ? 1.0f : 1.0f - (Age - Scp001GateGuardian.StrikeImpact) / (FadeEnd - Scp001GateGuardian.StrikeImpact);
        Vec3 Lift = From.add(0.0, 18.0, 0.0);
        Vec3 Reached = Lift.add(Target.subtract(Lift).scale(Reach));
        Vec3[] Arc = new Vec3[TrailPoints];
        Vec3 Bulge = From.add(Reached).scale(0.5).add(0.0, 6.0 * (1.0f - Reach) + 2.0, 0.0);
        for (int Index = 0; Index < TrailPoints; Index++) {
            float Share = Index / (float) (TrailPoints - 1);
            Vec3 Ab = From.scale(1.0 - Share).add(Bulge.scale(Share));
            Vec3 Bc = Bulge.scale(1.0 - Share).add(Reached.scale(Share));
            Arc[Index] = Ab.scale(1.0 - Share).add(Bc.scale(Share));
        }
        float Time = Owner.tickCount + Partial;
        VertexConsumer Consumer = Buffers.getBuffer(Scp001GateLight.glow(Scp001GateLight.Trail));
        Scp001GateLight.ribbon(Consumer, Pose.last(), Eye, Arc, 1.6f, -Time * 0.15f, Alpha);
        Scp001GateLight.ribbon(Consumer, Pose.last(), Eye, Arc, 3.4f, -Time * 0.09f, Alpha * 0.45f);
        if (Reach >= 1.0f) {
            VertexConsumer Bloom = Buffers.getBuffer(Scp001GateLight.glow(Scp001GateLight.Flash));
            Scp001GateLight.flat(Bloom, Pose.last(), Target.add(0.0, 1.0, 0.0), Eye.subtract(Target), 3.0f + 2.0f * Alpha, Time * 0.1f, 1.0f, 0.9f, 0.7f, Alpha,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        }
    }

    private void bladeFire(Scp001GateGuardian Owner, PoseStack Pose, MultiBufferSource Buffers, Vec3 Eye, Vec3 Hilt, Vec3 Tip, float Partial) {
        int Frame = Math.floorMod((int) ((Owner.tickCount + Partial) / FireTicks), FireFrames);
        float Top = Frame / (float) FireFrames;
        float Bottom = (Frame + 1) / (float) FireFrames;
        VertexConsumer Consumer = Buffers.getBuffer(Scp001GateLight.glow(Scp001GateLight.BladeFire));
        PoseStack.Pose Last = Pose.last();
        for (int Tile = 0; Tile < FireTiles; Tile++) {
            Vec3 Base = Tip.y <= Hilt.y ? Tip : Hilt;
            Vec3 Crown = Tip.y <= Hilt.y ? Hilt : Tip;
            Vec3 Low = Base.add(Crown.subtract(Base).scale(Tile / (double) FireTiles));
            Vec3 High = Base.add(Crown.subtract(Base).scale((Tile + 1) / (double) FireTiles));
            Vec3 Along = High.subtract(Low);
            Vec3 Side = Along.cross(Low.subtract(Eye));
            if (Side.lengthSqr() < 1.0e-8) {
                continue;
            }
            Side = Side.normalize().scale(FireHalfWidth);
            Scp001GateLight.quad(Consumer, Last, High.subtract(Side), Low.subtract(Side), Low.add(Side), High.add(Side), 0.0f, Top, 1.0f, Bottom, 1.0f, 1.0f, 1.0f, 1.0f,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        }
    }

    private void sparks(Scp001GateGuardian Owner, Vec3 Origin) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.player == null || Client.isPaused() || Client.level.getGameTime() == LastSpark || Client.options.particles().get().getId() >= 2) {
            return;
        }
        LastSpark = Client.level.getGameTime();
        if (Client.player.distanceToSqr(Origin) > SparkReach * SparkReach) {
            return;
        }
        RandomSource Random = Owner.getRandom();
        float Swing = Owner.strikeTime(0.0f);
        boolean Flaring = Swing >= 2.0f && Swing <= Scp001GateGuardian.StrikeImpact + 2.0f;
        int Count = Flaring ? FlaringSparks : BladeSparks;
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Along = Scp001GateBlade.point(BladePose, Random.nextFloat(), Random.nextFloat() * 2.0f - 1.0f, Random.nextFloat() * 2.0f - 1.0f).add(Origin);
            boolean Ember = Index % 3 == 2;
            Client.level.addParticle(Ember ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, Along.x, Along.y, Along.z, Random.nextGaussian() * 0.01,
                    (Ember ? 0.12 : 0.05) + Random.nextDouble() * 0.04, Random.nextGaussian() * 0.01);
        }
        if (Random.nextInt(6) == 0) {
            Vec3 Spark = Scp001GateBlade.point(BladePose, Random.nextFloat(), 0.0f, 0.0f).add(Origin);
            Client.level.addParticle(ParticleTypes.LAVA, Spark.x, Spark.y, Spark.z, 0.0, 0.0, 0.0);
        }
        double Scale = Scp001GateGuardian.RenderScale / 16.0;
        float Radians = Owner.facing() * Mth.DEG_TO_RAD;
        for (int Index = 0; Index < 2; Index++) {
            double Ox = (Random.nextDouble() - 0.5) * 30.0 * Scale;
            double Oy = (72.0 + Random.nextDouble() * 34.0) * Scale;
            double Oz = (Random.nextDouble() * 10.0 - 2.0) * Scale;
            double X = Origin.x + Ox * Mth.cos(Radians) - Oz * Mth.sin(Radians);
            double Z = Origin.z + Ox * Mth.sin(Radians) + Oz * Mth.cos(Radians);
            Client.level.addParticle(Random.nextInt(4) == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, X, Origin.y + Oy, Z, 0.0, 0.08, 0.0);
        }
    }

    @Override
    protected void applyRotations(Scp001GateGuardian Owner, PoseStack Pose, float Age, float Yaw, float Partial, float NativeScale) {
        super.applyRotations(Owner, Pose, Age, Owner.facing(Partial), Partial, NativeScale);
    }

    @Override
    protected float getDeathMaxRotation(Scp001GateGuardian Owner) {
        return 0.0f;
    }
}
