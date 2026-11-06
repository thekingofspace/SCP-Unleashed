package net.rai.scp_unleashed.scp001gate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001gate.Scp001GateGuardian;
import net.rai.scp_unleashed.scp001gate.Scp001GatePlan;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;
import net.rai.scp_unleashed.scp001gate.network.Scp001GateStrikePayload;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Scp001GateEffects {
    public static final int FlashTicks = 6;
    public static final int WaveTicks = 16;
    public static final int ScorchTicks = 600;
    public static final int ScorchFade = 100;
    public static final float WaveRadius = 10.0f;
    public static final float ScorchRadius = 2.6f;
    public static final double AmbientReach = Scp001GatePlan.PlainRadius;
    public static final int ScanEvery = 20;
    public static final DustParticleOptions Dust = new DustParticleOptions(new Vector3f(0.72f, 0.6f, 0.42f), 1.1f);
    private static final List<Impact> Impacts = new ArrayList<>();
    private static Vec3 Gate;
    private static float Nearness;

    private record Impact(Vec3 Point, double Ground, long Start) {
    }

    private Scp001GateEffects() {
    }

    public static float nearness() {
        return Nearness;
    }

    public static void strike(Scp001GateStrikePayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null) {
            return;
        }
        Vec3 Point = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
        BlockPos Column = BlockPos.containing(Point);
        double Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Column.getX(), Column.getZ());
        if (Ground > Point.y + 3.0 || Ground < Point.y - 12.0) {
            Ground = Math.floor(Point.y);
        }
        Impacts.add(new Impact(Point, Ground, World.getGameTime()));
        if (Impacts.size() > 24) {
            Impacts.remove(0);
        }
        RandomSource Random = World.getRandom();
        for (int Index = 0; Index < 40; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Speed = 0.2 + Random.nextDouble() * 0.5;
            World.addParticle(Index % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, Point.x, Ground + 0.2, Point.z, Math.cos(Angle) * Speed, 0.1 + Random.nextDouble() * 0.3,
                    Math.sin(Angle) * Speed);
        }
        for (int Index = 0; Index < 24; Index++) {
            double Angle = Index * Math.PI * 2.0 / 24.0;
            World.addParticle(Dust, Point.x + Math.cos(Angle) * 1.5, Ground + 0.3, Point.z + Math.sin(Angle) * 1.5, Math.cos(Angle) * 0.6, 0.05, Math.sin(Angle) * 0.6);
        }
    }

    public static void guardianTick(Scp001GateGuardian Guardian) {
        if (Guardian.newStrike()) {
            Minecraft Client = Minecraft.getInstance();
            if (Client.level != null) {
                Client.level.playLocalSound(Guardian.getX(), Guardian.getY() + 18.0, Guardian.getZ(), Scp001GateRegistry.Raise.get(), SoundSource.HOSTILE, 3.0f, 1.0f, false);
            }
        }
    }

    public static void tick() {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Client.player == null) {
            Impacts.clear();
            Gate = null;
            Nearness = 0.0f;
            return;
        }
        long Now = World.getGameTime();
        Impacts.removeIf(Each -> Now - Each.Start() > ScorchTicks);
        if (Now % ScanEvery == 0) {
            Vec3 At = Client.player.position();
            List<Scp001GateGuardian> Found = World.getEntitiesOfClass(Scp001GateGuardian.class, new AABB(At, At).inflate(AmbientReach + 40.0, 256.0, AmbientReach + 40.0));
            Gate = Found.isEmpty() ? null : Vec3.atBottomCenterOf(Found.get(0).centre());
        }
        float Goal = 0.0f;
        if (Gate != null) {
            double Distance = Math.sqrt(Client.player.distanceToSqr(Gate.x, Client.player.getY(), Gate.z));
            Goal = (float) Mth.clamp(1.0 - (Distance - 60.0) / (AmbientReach - 60.0), 0.0, 1.0);
        }
        Nearness += (Goal - Nearness) * 0.05f;
        if (Nearness < 0.02f || Client.isPaused()) {
            return;
        }
        RandomSource Random = World.getRandom();
        int Motes = Client.options.particles().get().getId() == 0 ? 4 : 1;
        for (int Index = 0; Index < Motes; Index++) {
            if (Random.nextFloat() > Nearness) {
                continue;
            }
            double X = Client.player.getX() + (Random.nextDouble() - 0.5) * 28.0;
            double Y = Client.player.getY() + Random.nextDouble() * 8.0 - 1.0;
            double Z = Client.player.getZ() + (Random.nextDouble() - 0.5) * 28.0;
            World.addParticle(Random.nextInt(5) == 0 ? ParticleTypes.WHITE_ASH : Dust, X, Y, Z, 0.08 + Random.nextDouble() * 0.06, -0.005, 0.03);
        }
    }

    public static void render(PoseStack Pose, Camera View, float Partial) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Impacts.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource Buffers = Client.renderBuffers().bufferSource();
        Vec3 Eye = View.getPosition();
        float Now = World.getGameTime() + Partial;
        Pose.pushPose();
        Pose.translate(-Eye.x, -Eye.y, -Eye.z);
        Iterator<Impact> Each = Impacts.iterator();
        while (Each.hasNext()) {
            Impact Hit = Each.next();
            float Age = Now - Hit.Start();
            Vec3 Floor = new Vec3(Hit.Point().x, Hit.Ground() + 0.03, Hit.Point().z);
            int Light = LevelRenderer.getLightColor(World, BlockPos.containing(Floor.x, Floor.y + 0.5, Floor.z));
            float Burnt = Age < ScorchTicks - ScorchFade ? 1.0f : Mth.clamp((ScorchTicks - Age) / ScorchFade, 0.0f, 1.0f);
            VertexConsumer Char = Buffers.getBuffer(RenderType.entityTranslucent(Scp001GateLight.Scorch));
            Scp001GateLight.flat(Char, Pose.last(), Floor, new Vec3(0.0, 1.0, 0.0), ScorchRadius, (float) (Hit.Point().x * 0.7), 1.0f, 1.0f, 1.0f, 0.9f * Burnt, Light);
            if (Age < WaveTicks) {
                float Share = Age / WaveTicks;
                VertexConsumer Wave = Buffers.getBuffer(Scp001GateLight.glow(Scp001GateLight.Shockwave));
                Scp001GateLight.flat(Wave, Pose.last(), Floor.add(0.0, 0.08, 0.0), new Vec3(0.0, 1.0, 0.0), 1.0f + WaveRadius * Share, 0.0f, 1.0f, 0.85f, 0.6f,
                        (1.0f - Share) * 0.9f, LightTexture.FULL_BRIGHT);
            }
            if (Age < FlashTicks) {
                float Share = Age / FlashTicks;
                VertexConsumer Bloom = Buffers.getBuffer(Scp001GateLight.glow(Scp001GateLight.Flash));
                Vec3 Centre = Floor.add(0.0, 1.5, 0.0);
                Scp001GateLight.flat(Bloom, Pose.last(), Centre, Eye.subtract(Centre), 4.0f + 3.0f * Share, Age * 0.2f, 1.0f, 0.95f, 0.8f, 1.0f - Share,
                        LightTexture.FULL_BRIGHT);
            }
        }
        Pose.popPose();
        Buffers.endBatch();
    }
}
