package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.scp902.Scp902Scene;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Scp902Overhead {
    public static final int StartAfter = 70;
    public static final int Period = 360;
    public static final int GapQuiet = 120;
    public static final int GapPeak = 22;
    public static final int SpreadQuiet = 60;
    public static final int SpreadPeak = 40;
    public static final float SnapShare = 0.25f;
    public static final float TracerShare = 0.35f;
    public static final double CrossReach = 22.0;
    public static final double LowPass = 1.8;
    public static final double HighPass = 5.0;
    public static final double AheadNear = 4.0;
    public static final double AheadFar = 20.0;
    public static final int SweepTicks = 7;
    public static final int TracerTicks = 5;
    public static final double TracerLength = 4.5;
    public static final double TracerLift = 1.0;
    public static final double TracerAhead = 9.0;
    public static final float TracerWidth = 0.05f;
    public static final float TracerAlpha = 0.75f;
    public static final double ParapetX = 2.0;
    public static final double ParapetTop = 63.85;
    public static final int Puffs = 8;
    private static final RandomSource Random = RandomSource.create();
    private static final List<Tracer> Tracers = new ArrayList<>();
    private static int Next = StartAfter;
    private static int Now;

    private record Tracer(Vec3 From, Vec3 To, int Born) {
    }

    private Scp902Overhead() {
    }

    public static void reset() {
        Tracers.clear();
        Next = StartAfter;
        Now = 0;
    }

    public static float swell(int Clock) {
        if (Clock < StartAfter) {
            return 0.0f;
        }
        return 0.5f - 0.5f * Mth.cos((Clock - StartAfter) * Mth.TWO_PI / Period);
    }

    public static int gap(float Swell) {
        int Base = Math.round(Mth.lerp(Swell, GapQuiet, GapPeak));
        int Spread = Math.round(Mth.lerp(Swell, SpreadQuiet, SpreadPeak));
        return Base + Random.nextInt(Spread + 1);
    }

    public static void tick(Minecraft Client, int Clock) {
        Now = Clock;
        Tracers.removeIf(Each -> Clock - Each.Born() > TracerTicks + 2);
        if (Clock < Next || Clock > Scp902Scene.FinishAt) {
            return;
        }
        Next = Clock + gap(swell(Clock));
        if (Scp902Ambience.hushed(Clock) || Client.player == null) {
            return;
        }
        Vec3 Eye = Client.player.getEyePosition();
        if (Random.nextFloat() < SnapShare) {
            snap(Client, Eye);
        } else {
            flyBy(Client, Eye, Random.nextFloat() < TracerShare);
        }
    }

    private static void flyBy(Minecraft Client, Vec3 Eye, boolean Lit) {
        double Ahead = AheadNear + Random.nextDouble() * (AheadFar - AheadNear);
        if (Lit) {
            Ahead = Math.max(Ahead, TracerAhead);
        }
        double Height = Eye.y + LowPass + Random.nextDouble() * (HighPass - LowPass);
        double Drift = (Random.nextDouble() - 0.5) * 6.0;
        boolean Rightward = Random.nextBoolean();
        Vec3 From = new Vec3(Eye.x + (Rightward ? -CrossReach : CrossReach), Height + 0.6, Eye.z - Ahead + Drift);
        Vec3 To = new Vec3(Eye.x + (Rightward ? CrossReach : -CrossReach), Height - 0.6, Eye.z - Ahead - Drift);
        float Pitch = 0.9f + Random.nextFloat() * 0.25f;
        Client.getSoundManager().play(new Scp902FlyBy(Scp902Registry.BulletWhiz.get(), From, To, SweepTicks, 0.85f, Pitch, Random));
        Client.getSoundManager().play(new Scp902FlyBy(GunsRegistry.ShotWhump.get(), From, To, SweepTicks, 0.12f, 1.9f, Random));
        Vec3 Over = From.lerp(To, 0.5);
        play(Client, Scp902Registry.BulletCrack.get(), Over, 0.6f, 0.9f + Random.nextFloat() * 0.2f);
        play(Client, GunsRegistry.ShotCrack.get(), Over, 0.25f, 1.85f);
        if (Lit) {
            Tracers.add(new Tracer(From.add(0.0, TracerLift, 0.0), To.add(0.0, TracerLift, 0.0), Now));
            if (Random.nextBoolean()) {
                Tracers.add(new Tracer(From.add(0.0, TracerLift + 0.9, -1.5), To.add(0.0, TracerLift + 0.7, -1.5), Now + 2));
            }
        }
    }

    private static void snap(Minecraft Client, Vec3 Eye) {
        ClientLevel World = Client.level;
        if (World == null) {
            return;
        }
        double Side = Random.nextBoolean() ? ParapetX : -ParapetX;
        Vec3 Hit = new Vec3(Math.floor(Eye.x) + 0.5 + Side + (Side > 0 ? -0.35 : 0.35), ParapetTop, Eye.z - 1.0 - Random.nextDouble() * 7.0);
        play(Client, Scp902Registry.BulletSnap.get(), Hit, 0.9f, 0.9f + Random.nextFloat() * 0.25f);
        play(Client, Scp902Registry.BulletCrack.get(), Hit.add(0.0, 2.0, 0.0), 0.35f, 1.2f);
        BlockParticleOption Dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_MUD.defaultBlockState());
        for (int Puff = 0; Puff < Puffs; Puff++) {
            World.addParticle(Dirt, Hit.x + (Random.nextDouble() - 0.5) * 0.4, Hit.y + Random.nextDouble() * 0.15, Hit.z + (Random.nextDouble() - 0.5) * 0.4,
                    (Random.nextDouble() - 0.5) * 0.12, 0.08 + Random.nextDouble() * 0.12, (Random.nextDouble() - 0.5) * 0.12);
        }
        World.addParticle(ParticleTypes.POOF, Hit.x, Hit.y + 0.1, Hit.z, 0.0, 0.03, 0.0);
    }

    private static void play(Minecraft Client, SoundEvent Sound, Vec3 At, float Volume, float Pitch) {
        Client.getSoundManager().play(new SimpleSoundInstance(Sound.getLocation(), SoundSource.AMBIENT, Volume, Pitch, Random, false, 0,
                SoundInstance.Attenuation.LINEAR, At.x, At.y, At.z, false));
    }

    public static void render(PoseStack Pose, Vec3 Eye, float Partial) {
        if (Tracers.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource Buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer Lines = Buffers.getBuffer(RenderType.debugQuads());
        Matrix4f Matrix = Pose.last().pose();
        for (Tracer Each : Tracers) {
            float Age = Now - Each.Born() + Partial;
            if (Age < 0.0f || Age > TracerTicks) {
                continue;
            }
            Vec3 Course = Each.To().subtract(Each.From());
            Vec3 Head = Each.From().add(Course.scale(Age / TracerTicks));
            Vec3 Tail = Head.subtract(Course.normalize().scale(TracerLength));
            float Fade = TracerAlpha * (1.0f - Age / (TracerTicks + 1.0f) * 0.5f);
            ribbon(Lines, Matrix, Tail.subtract(Eye), Head.subtract(Eye), Fade);
        }
        Buffers.endBatch(RenderType.debugQuads());
    }

    private static void ribbon(VertexConsumer Lines, Matrix4f Matrix, Vec3 Tail, Vec3 Head, float Alpha) {
        Vector3f Along = new Vector3f((float) (Head.x - Tail.x), (float) (Head.y - Tail.y), (float) (Head.z - Tail.z));
        Vector3f Middle = new Vector3f((float) (Head.x + Tail.x) * 0.5f, (float) (Head.y + Tail.y) * 0.5f, (float) (Head.z + Tail.z) * 0.5f);
        Vector3f Side = new Vector3f(Along).cross(Middle);
        if (Side.lengthSquared() < 1.0e-6f) {
            return;
        }
        Side.normalize(TracerWidth);
        float Red = 1.0f;
        float Green = 0.62f;
        float Blue = 0.3f;
        vertex(Lines, Matrix, Tail, Side, -1.0f, Red, Green, Blue, 0.0f);
        vertex(Lines, Matrix, Tail, Side, 1.0f, Red, Green, Blue, 0.0f);
        vertex(Lines, Matrix, Head, Side, 1.0f, Red, Green, Blue, Alpha);
        vertex(Lines, Matrix, Head, Side, -1.0f, Red, Green, Blue, Alpha);
        vertex(Lines, Matrix, Head, Side, -1.0f, Red, Green, Blue, Alpha);
        vertex(Lines, Matrix, Head, Side, 1.0f, Red, Green, Blue, Alpha);
        vertex(Lines, Matrix, Tail, Side, 1.0f, Red, Green, Blue, 0.0f);
        vertex(Lines, Matrix, Tail, Side, -1.0f, Red, Green, Blue, 0.0f);
    }

    private static void vertex(VertexConsumer Lines, Matrix4f Matrix, Vec3 At, Vector3f Side, float Sign, float Red, float Green, float Blue, float Alpha) {
        Lines.addVertex(Matrix, (float) At.x + Side.x * Sign, (float) At.y + Side.y * Sign, (float) At.z + Side.z * Sign).setColor(Red, Green, Blue, Alpha);
    }

    public static int tracers() {
        return Tracers.size();
    }
}
