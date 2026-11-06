package net.rai.scp_unleashed.ultralaser.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.client.GunClientSounds;
import net.rai.scp_unleashed.guns.client.GunClientState;
import net.rai.scp_unleashed.guns.client.GunMuzzle;
import net.rai.scp_unleashed.guns.network.GunImpact;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.ultralaser.UltralaserItem;
import net.rai.scp_unleashed.ultralaser.UltralaserRegistry;
import net.rai.scp_unleashed.ultralaser.UltralaserScore;
import net.rai.scp_unleashed.ultralaser.network.UltralaserBlastPayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class UltralaserEffects {
    public static final int HoldTicks = 16;
    public static final int FadeTicks = 18;
    public static final int BeamLife = HoldTicks + FadeTicks;
    public static final int AfterglowTicks = 30;
    public static final int ShakeHold = 10;
    public static final int ShakeTicks = 24;
    public static final float ShakeDegrees = 2.6F;
    public static final float ReadyShakeDegrees = 0.35F;
    public static final double SegmentLength = 1.5;
    public static final double TaperLength = 5.0;
    public static final float TaperFloor = 0.12F;
    public static final double FlareReach = 5.0;
    public static final double RingSpeed = 14.0;
    public static final int RingCount = 5;
    public static final double MotePull = 0.09;
    public static final float[] Teal = {0.22F, 0.92F, 0.86F};
    public static final float[] Pink = {1.0F, 0.32F, 0.7F};
    public static final float[] White = {1.0F, 1.0F, 1.0F};
    public static final Vector3f DustTeal = new Vector3f(0.24F, 0.9F, 0.84F);
    public static final Vector3f DustPink = new Vector3f(1.0F, 0.36F, 0.72F);
    private static final List<Blast> Blasts = new ArrayList<>();
    private static long ShakeStart = Long.MIN_VALUE / 2;
    @Nullable
    private static Vec3 LocalMuzzle;

    private UltralaserEffects() {
    }

    private static final class Blast {
        private final int ShooterId;
        private final Vec3 Origin;
        private final Vec3 End;
        private final long Born;
        private final float Seed;
        @Nullable
        private Vec3 Start;

        private Blast(int ShooterId, Vec3 Origin, Vec3 End, long Born, float Seed) {
            this.ShooterId = ShooterId;
            this.Origin = Origin;
            this.End = End;
            this.Born = Born;
            this.Seed = Seed;
        }

        private Vec3 start() {
            return Start == null ? Origin : Start;
        }
    }

    public static void onBlast(UltralaserBlastPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        LocalPlayer User = Game.player;
        boolean Local = User != null && User.getId() == Payload.ShooterId();
        if (Local) {
            GunClientState.onLocalShot(User, GunKind.Ultralaser);
            ShakeStart = GunClientState.ticks();
        } else {
            GunClientState.onShotSeen(Payload.ShooterId(), GunKind.Ultralaser, User);
        }
        RandomSource Random = World.getRandom();
        Blast Shot = new Blast(Payload.ShooterId(), Payload.Origin(), Payload.End(), GunClientState.ticks(), Random.nextFloat() * 100.0F);
        Blasts.add(Shot);
        boolean Hit = false;
        boolean Kill = false;
        for (GunImpact Impact : Payload.Impacts()) {
            Vec3 Point = Impact.point(Payload.Origin());
            if (Impact.isEntity()) {
                Hit = true;
                Kill |= Impact.Type() == GunImpact.Kill;
                wound(World, Point, Impact.Type() == GunImpact.Kill, Random);
            } else if (Impact.Type() == GunImpact.Surface) {
                strike(World, Point, Vec3.atLowerCornerOf(Impact.side().getNormal()), Random);
            }
        }
        beamParticles(World, Payload.Origin(), Payload.End(), Random, 1.0F);
        if (Local && Hit) {
            GunClientState.onHit(Kill);
            GunClientSounds.play(GunSounds.hitMarker(Kill), 1.0F, GunSounds.PitchJitter);
        }
    }

    private static void wound(ClientLevel World, Vec3 Point, boolean Kill, RandomSource Random) {
        int Count = Kill ? 18 : 10;
        for (int Index = 0; Index < Count; Index++) {
            double Dx = (Random.nextDouble() - 0.5) * 0.5;
            double Dy = Random.nextDouble() * 0.3;
            double Dz = (Random.nextDouble() - 0.5) * 0.5;
            World.addParticle(Index % 2 == 0 ? UltralaserRegistry.MoteTeal.get() : UltralaserRegistry.MotePink.get(), true, Point.x, Point.y, Point.z, Dx, Dy, Dz);
        }
        World.addParticle(ParticleTypes.FLASH, true, Point.x, Point.y, Point.z, 0.0, 0.0, 0.0);
        if (Kill) {
            World.addParticle(ParticleTypes.EXPLOSION, true, Point.x, Point.y, Point.z, 0.0, 0.0, 0.0);
        }
    }

    private static void strike(ClientLevel World, Vec3 Point, Vec3 Normal, RandomSource Random) {
        Vec3 Lifted = Point.add(Normal.scale(0.15));
        World.addParticle(ParticleTypes.FLASH, true, Lifted.x, Lifted.y, Lifted.z, 0.0, 0.0, 0.0);
        World.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, Lifted.x, Lifted.y, Lifted.z, 0.0, 0.0, 0.0);
        for (int Index = 0; Index < 40; Index++) {
            double Speed = 0.25 + Random.nextDouble() * 0.45;
            Vec3 Out = new Vec3(Random.nextGaussian(), Random.nextGaussian(), Random.nextGaussian()).normalize().add(Normal).normalize().scale(Speed);
            World.addParticle(ParticleTypes.FIREWORK, true, Lifted.x, Lifted.y, Lifted.z, Out.x, Out.y, Out.z);
            World.addParticle(Index % 2 == 0 ? UltralaserRegistry.MotePink.get() : UltralaserRegistry.MoteTeal.get(), true, Lifted.x, Lifted.y, Lifted.z,
                    Out.x * 0.6, Out.y * 0.6, Out.z * 0.6);
        }
    }

    private static void beamParticles(ClientLevel World, Vec3 Start, Vec3 End, RandomSource Random, float Amount) {
        Vec3 Span = End.subtract(Start);
        double Length = Span.length();
        if (Length < 0.5) {
            return;
        }
        Vec3 Forward = Span.scale(1.0 / Length);
        int Count = Mth.clamp((int) (Length * 2.0 * Amount), 2, 240);
        for (int Index = 0; Index < Count; Index++) {
            double Along = 1.2 + (Length - 1.2) * (Index + Random.nextDouble()) / Count;
            Vec3 Point = Start.add(Forward.scale(Along)).add((Random.nextDouble() - 0.5) * 0.9, (Random.nextDouble() - 0.5) * 0.9, (Random.nextDouble() - 0.5) * 0.9);
            boolean Rosy = Random.nextBoolean();
            float Scale = 1.1F + Random.nextFloat() * 1.3F;
            World.addParticle(new DustColorTransitionOptions(Rosy ? DustPink : DustTeal, Rosy ? DustTeal : DustPink, Scale), true, Point.x, Point.y, Point.z, 0.0, 0.0, 0.0);
            if (Index % 3 == 0) {
                World.addParticle(ParticleTypes.END_ROD, true, Point.x, Point.y, Point.z, (Random.nextDouble() - 0.5) * 0.04, (Random.nextDouble() - 0.5) * 0.04,
                        (Random.nextDouble() - 0.5) * 0.04);
            }
            if (Index % 2 == 0) {
                World.addParticle(Rosy ? UltralaserRegistry.MotePink.get() : UltralaserRegistry.MoteTeal.get(), true, Point.x, Point.y, Point.z,
                        (Random.nextDouble() - 0.5) * 0.06, (Random.nextDouble() - 0.5) * 0.06, (Random.nextDouble() - 0.5) * 0.06);
            }
        }
        if (Amount >= 1.0F) {
            for (int Ring = 1; Ring <= 3; Ring++) {
                Vec3 Point = Start.add(Forward.scale(Math.min(Length, Ring * 1.6)));
                World.addParticle(ParticleTypes.SONIC_BOOM, true, Point.x, Point.y, Point.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            Blasts.clear();
            LocalMuzzle = null;
            return;
        }
        long Now = GunClientState.ticks();
        Blasts.removeIf(Shot -> Now - Shot.Born > BeamLife + 2);
        RandomSource Random = World.getRandom();
        for (Blast Shot : Blasts) {
            long Age = Now - Shot.Born;
            if (Age > 0 && Age < AfterglowTicks && Age % 2 == 0) {
                float Afterglow = Age < HoldTicks ? 0.35F : 0.35F * (1.0F - (Age - HoldTicks) / (float) (AfterglowTicks - HoldTicks));
                beamParticles(World, Shot.start(), Shot.End, Random, Afterglow);
            }
        }
        for (Player Holder : World.players()) {
            ItemStack Stack = Holder.getMainHandItem();
            GunTimer Charging = UltralaserItem.isUltralaser(Stack) ? GunItem.getCharging(Stack) : null;
            if (Charging == null) {
                continue;
            }
            float Progress = UltralaserPrimingSound.primeProgress(Holder, Charging, 0.0F);
            boolean FirstPerson = Holder == Game.player && Game.options.getCameraType().isFirstPerson();
            Vec3 Muzzle = FirstPerson ? LocalMuzzle : GunMuzzle.thirdPersonWorld(Holder, Stack, GunKind.Ultralaser, 1.0F);
            if (Muzzle == null) {
                continue;
            }
            int Count = 1 + Math.round(Progress * 3.0F);
            for (int Index = 0; Index < Count; Index++) {
                Vec3 Offset = new Vec3(Random.nextGaussian(), Random.nextGaussian(), Random.nextGaussian()).normalize().scale(0.35 + 0.4 * Random.nextDouble());
                Vec3 From = Muzzle.add(Offset);
                Vec3 Pull = Offset.scale(-MotePull);
                boolean Rosy = Progress > 0.55F ? Random.nextBoolean() : Random.nextFloat() < 0.25F;
                World.addParticle(Rosy ? UltralaserRegistry.MotePink.get() : UltralaserRegistry.MoteTeal.get(), true, From.x, From.y, From.z, Pull.x, Pull.y, Pull.z);
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera View = Event.getCamera();
        Matrix4f Projection = Event.getProjectionMatrix();
        LocalPlayer User = Game.player;
        LocalMuzzle = null;
        if (User != null && Game.options.getCameraType().isFirstPerson() && UltralaserItem.isUltralaser(User.getMainHandItem())) {
            LocalMuzzle = GunMuzzle.firstPersonWorld(User, User.getMainHandItem(), GunKind.Ultralaser, View, Projection, Partial);
        }
        Vec3 Eye = View.getPosition();
        PoseStack.Pose Last = Event.getPoseStack().last();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        float Now = GunClientState.ticks() + Partial;
        for (Blast Shot : Blasts) {
            if (Shot.Start == null) {
                Shot.Start = muzzleOf(Game, Shot.ShooterId, Shot.Origin, View, Projection, Partial);
            }
        }
        RenderType Beams = UltralaserRenderTypes.glow(UltralaserRenderTypes.BeamTexture);
        VertexConsumer Consumer = Buffers.getBuffer(Beams);
        for (Blast Shot : Blasts) {
            drawBeam(Consumer, Last, Shot, Eye, Now);
        }
        Buffers.endBatch(Beams);
        RenderType Rings = UltralaserRenderTypes.glow(UltralaserRenderTypes.RingTexture);
        Consumer = Buffers.getBuffer(Rings);
        for (Blast Shot : Blasts) {
            drawRings(Consumer, Last, Shot, Eye, Now);
        }
        Buffers.endBatch(Rings);
        RenderType Flares = UltralaserRenderTypes.glow(UltralaserRenderTypes.FlareTexture);
        Consumer = Buffers.getBuffer(Flares);
        for (Blast Shot : Blasts) {
            drawFlares(Consumer, Last, Shot, Eye, Now);
        }
        for (Player Holder : World.players()) {
            drawCharge(Consumer, Last, Game, Holder, Eye, Partial);
        }
        Buffers.endBatch(Flares);
    }

    private static Vec3 muzzleOf(Minecraft Game, int ShooterId, Vec3 Origin, Camera View, Matrix4f Projection, float Partial) {
        Entity Shooter = Game.level == null ? null : Game.level.getEntity(ShooterId);
        if (!(Shooter instanceof Player Gunner)) {
            return Origin;
        }
        ItemStack Stack = Gunner.getMainHandItem();
        if (!UltralaserItem.isUltralaser(Stack)) {
            return Origin.add(Gunner.getLookAngle().scale(0.8)).add(0.0, -0.2, 0.0);
        }
        if (Gunner == Game.player && Game.options.getCameraType().isFirstPerson() && Gunner instanceof LocalPlayer Local) {
            return GunMuzzle.firstPersonWorld(Local, Stack, GunKind.Ultralaser, View, Projection, Partial);
        }
        return GunMuzzle.thirdPersonWorld(Gunner, Stack, GunKind.Ultralaser, Partial);
    }

    private static float life(Blast Shot, float Now) {
        float Age = Now - Shot.Born;
        if (Age < HoldTicks) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (Age - HoldTicks) / FadeTicks, 0.0F, 1.0F);
    }

    private static void drawBeam(VertexConsumer Consumer, PoseStack.Pose Last, Blast Shot, Vec3 Eye, float Now) {
        float Age = Now - Shot.Born;
        float Life = life(Shot, Now);
        if (Life <= 0.0F) {
            return;
        }
        Vec3 A = Shot.start().subtract(Eye);
        Vec3 B = Shot.End.subtract(Eye);
        Vec3 Span = B.subtract(A);
        double Length = Span.length();
        if (Length < 0.1) {
            return;
        }
        float Fade = Life * Life * (3.0F - 2.0F * Life);
        float Burst = Age < 3.0F ? 1.4F - 0.13F * Age : 1.0F;
        float Thin = 0.3F + 0.7F * Mth.sqrt(Life);
        float Flicker = 0.9F + 0.1F * Mth.sin(Age * 2.7F + Shot.Seed);
        float Width = Burst * Thin;
        ribbon(Consumer, Last, A, B, 2.6F * Width * (1.0F + 0.3F * (1.0F - Life)), Pink, 0.5F * Fade * Flicker);
        ribbon(Consumer, Last, A, B, 1.25F * Width, Teal, 0.9F * Fade * Flicker);
        ribbon(Consumer, Last, A, B, 0.42F * Width, White, Fade);
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Helper = Math.abs(Forward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 AxisA = Forward.cross(Helper).normalize();
        Vec3 AxisB = Forward.cross(AxisA).normalize();
        int Steps = Mth.clamp((int) (Length * 3.0), 12, 400);
        for (int Strand = 0; Strand < 2; Strand++) {
            float[] Colour = Strand == 0 ? Teal : Pink;
            Vec3 Previous = null;
            for (int Index = 0; Index <= Steps; Index++) {
                double Along = Length * Index / Steps;
                double Phase = Along * 1.6 - Age * 0.9 + Strand * Math.PI + Shot.Seed;
                double Radius = (0.55 + 0.35 * (1.0 - Life)) * taper(Along) * Width;
                Vec3 Point = A.add(Forward.scale(Along)).add(AxisA.scale(Math.cos(Phase) * Radius)).add(AxisB.scale(Math.sin(Phase) * Radius));
                if (Previous != null) {
                    float Thickness = 0.16F * Width * taper(Along);
                    segment(Consumer, Last, Previous, Point, Thickness, Thickness, Colour, 0.85F * Fade);
                }
                Previous = Point;
            }
        }
    }

    private static float taper(double Along) {
        return (float) Mth.clamp(TaperFloor + Along / TaperLength, 0.0, 1.0);
    }

    private static void ribbon(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 A, Vec3 B, float Width, float[] Colour, float Strength) {
        Vec3 Span = B.subtract(A);
        double Length = Span.length();
        int Pieces = Math.max(1, (int) Math.ceil(Length / SegmentLength));
        for (int Piece = 0; Piece < Pieces; Piece++) {
            double Near = Length * Piece / Pieces;
            double Far = Length * (Piece + 1) / Pieces;
            Vec3 From = A.add(Span.scale(Piece / (double) Pieces));
            Vec3 To = A.add(Span.scale((Piece + 1) / (double) Pieces));
            segment(Consumer, Last, From, To, Width * taper(Near), Width * taper(Far), Colour, Strength);
        }
    }

    private static void segment(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 From, Vec3 To, float WidthFrom, float WidthTo, float[] Colour, float Strength) {
        Vec3 Tangent = To.subtract(From);
        Vec3 Middle = From.add(To).scale(0.5);
        Vec3 Side = Tangent.cross(Middle);
        double Size = Side.length();
        Side = Size < 1.0E-7 ? new Vec3(0.0, 1.0, 0.0) : Side.scale(1.0 / Size);
        Vec3 SideFrom = Side.scale(WidthFrom * 0.5);
        Vec3 SideTo = Side.scale(WidthTo * 0.5);
        float Red = Colour[0] * Strength;
        float Green = Colour[1] * Strength;
        float Blue = Colour[2] * Strength;
        vertex(Consumer, Last, From.subtract(SideFrom), 0.0F, 0.0F, Red, Green, Blue);
        vertex(Consumer, Last, From.add(SideFrom), 1.0F, 0.0F, Red, Green, Blue);
        vertex(Consumer, Last, To.add(SideTo), 1.0F, 1.0F, Red, Green, Blue);
        vertex(Consumer, Last, To.subtract(SideTo), 0.0F, 1.0F, Red, Green, Blue);
    }

    private static void drawRings(VertexConsumer Consumer, PoseStack.Pose Last, Blast Shot, Vec3 Eye, float Now) {
        float Age = Now - Shot.Born;
        Vec3 A = Shot.start().subtract(Eye);
        Vec3 Span = Shot.End.subtract(Eye).subtract(A);
        double Length = Span.length();
        if (Length < 0.5) {
            return;
        }
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Helper = Math.abs(Forward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 AxisA = Forward.cross(Helper).normalize();
        Vec3 AxisB = Forward.cross(AxisA).normalize();
        for (int Ring = 0; Ring < RingCount; Ring++) {
            float Delay = Ring * 1.5F;
            float Travel = Age - Delay;
            if (Travel <= 0.0F) {
                continue;
            }
            double Along = 1.0 + Travel * RingSpeed;
            if (Along > Length) {
                continue;
            }
            float Fade = Mth.clamp(1.0F - Travel / 9.0F, 0.0F, 1.0F);
            if (Fade <= 0.0F) {
                continue;
            }
            double Radius = 0.6 + 0.12 * Travel;
            Vec3 Centre = A.add(Forward.scale(Along));
            float[] Colour = Ring % 2 == 0 ? Teal : Pink;
            Vec3 U = AxisA.scale(Radius);
            Vec3 V = AxisB.scale(Radius);
            float Red = Colour[0] * Fade;
            float Green = Colour[1] * Fade;
            float Blue = Colour[2] * Fade;
            vertex(Consumer, Last, Centre.subtract(U).subtract(V), 0.0F, 0.0F, Red, Green, Blue);
            vertex(Consumer, Last, Centre.add(U).subtract(V), 1.0F, 0.0F, Red, Green, Blue);
            vertex(Consumer, Last, Centre.add(U).add(V), 1.0F, 1.0F, Red, Green, Blue);
            vertex(Consumer, Last, Centre.subtract(U).add(V), 0.0F, 1.0F, Red, Green, Blue);
        }
    }

    private static void drawFlares(VertexConsumer Consumer, PoseStack.Pose Last, Blast Shot, Vec3 Eye, float Now) {
        float Age = Now - Shot.Born;
        float Life = life(Shot, Now);
        if (Life <= 0.0F) {
            return;
        }
        float Fade = Life * Life;
        float Pop = Age < 4.0F ? 1.6F - 0.15F * Age : 1.0F;
        Vec3 Start = Shot.start().subtract(Eye);
        float Near = (float) Mth.clamp(Start.length() / FlareReach, 0.15, 1.0);
        flare(Consumer, Last, Start, 0.7F * Pop * Near, Age * 0.08F, White, Fade);
        flare(Consumer, Last, Start, 1.2F * Pop * Near, -Age * 0.05F, Teal, 0.7F * Fade);
        flare(Consumer, Last, Shot.End.subtract(Eye), 2.0F * Pop * (0.7F + 0.3F * Life), Age * 0.06F, Pink, 0.8F * Fade);
        flare(Consumer, Last, Shot.End.subtract(Eye), 1.2F * Pop, -Age * 0.1F, White, Fade);
    }

    private static void drawCharge(VertexConsumer Consumer, PoseStack.Pose Last, Minecraft Game, Player Holder, Vec3 Eye, float Partial) {
        ItemStack Stack = Holder.getMainHandItem();
        GunTimer Charging = UltralaserItem.isUltralaser(Stack) ? GunItem.getCharging(Stack) : null;
        if (Charging == null || Game.level == null) {
            return;
        }
        boolean FirstPerson = Holder == Game.player && Game.options.getCameraType().isFirstPerson();
        Vec3 Muzzle = FirstPerson ? LocalMuzzle : GunMuzzle.thirdPersonWorld(Holder, Stack, GunKind.Ultralaser, Partial);
        if (Muzzle == null) {
            return;
        }
        float Elapsed = UltralaserPrimingSound.primeTicks(Holder, Charging, Partial);
        float Progress = UltralaserPrimingSound.primeProgress(Holder, Charging, Partial);
        float Pulse = Math.max(0.0F, Elapsed - UltralaserScore.OriginTicks) % UltralaserScore.PulseTicks;
        float Beat = 1.0F - Pulse / UltralaserScore.PulseTicks;
        Beat *= Beat;
        float Count = Elapsed >= UltralaserScore.CountTick ? 1.0F : 0.0F;
        float Size = (0.08F + 0.22F * Progress) * (1.0F + 0.25F * Beat);
        float Scale = FirstPerson ? 0.6F : 1.0F;
        Vec3 Centre = Muzzle.subtract(Eye);
        flare(Consumer, Last, Centre, Size * 2.2F * Scale, Elapsed * 0.05F, Count > 0.0F ? Pink : Teal, 0.35F + 0.4F * Progress);
        flare(Consumer, Last, Centre, Size * Scale, -Elapsed * 0.08F, White, 0.5F + 0.5F * Progress);
    }

    private static void flare(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, float Size, float Turn, float[] Colour, float Strength) {
        Vec3 Facing = Centre.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : Centre.normalize();
        Vec3 Across = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Across = Across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        Vec3 Over = Across.cross(Facing).normalize();
        double Cos = Math.cos(Turn) * Size;
        double Sin = Math.sin(Turn) * Size;
        Vec3 U = Across.scale(Cos).add(Over.scale(Sin));
        Vec3 V = Over.scale(Cos).subtract(Across.scale(Sin));
        float Red = Colour[0] * Strength;
        float Green = Colour[1] * Strength;
        float Blue = Colour[2] * Strength;
        vertex(Consumer, Last, Centre.subtract(U).subtract(V), 0.0F, 0.0F, Red, Green, Blue);
        vertex(Consumer, Last, Centre.add(U).subtract(V), 1.0F, 0.0F, Red, Green, Blue);
        vertex(Consumer, Last, Centre.add(U).add(V), 1.0F, 1.0F, Red, Green, Blue);
        vertex(Consumer, Last, Centre.subtract(U).add(V), 0.0F, 1.0F, Red, Green, Blue);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float U, float V, float Red, float Green, float Blue) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Mth.clamp(Red, 0.0F, 1.0F), Mth.clamp(Green, 0.0F, 1.0F), Mth.clamp(Blue, 0.0F, 1.0F), 1.0F)
                .setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0F, 1.0F, 0.0F);
    }

    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        float Age = GunClientState.ticks() + Partial - ShakeStart;
        float Amount = 0.0F;
        if (Age >= 0.0F && Age < ShakeTicks) {
            float Left = Age < ShakeHold ? 1.0F : 1.0F - (Age - ShakeHold) / (ShakeTicks - ShakeHold);
            Amount = ShakeDegrees * Left * Left;
        }
        ItemStack Stack = User.getMainHandItem();
        GunTimer Charging = UltralaserItem.isUltralaser(Stack) ? GunItem.getCharging(Stack) : null;
        if (Charging != null && Game.level != null) {
            float Elapsed = UltralaserPrimingSound.primeTicks(User, Charging, Partial);
            if (Elapsed >= UltralaserScore.ReadyTick) {
                Amount += ReadyShakeDegrees * Mth.clamp((Elapsed - UltralaserScore.ReadyTick) / (UltralaserScore.PrimeTicks - UltralaserScore.ReadyTick), 0.0F, 1.0F);
            }
        }
        if (Amount <= 0.0F) {
            return;
        }
        float Clock = GunClientState.ticks() + Partial;
        Event.setYaw(Event.getYaw() + Amount * Mth.sin(Clock * 3.1F));
        Event.setPitch(Event.getPitch() + Amount * Mth.cos(Clock * 2.3F + 0.7F));
        Event.setRoll(Event.getRoll() + Amount * 0.7F * Mth.sin(Clock * 4.7F + 1.3F));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Blasts.clear();
        LocalMuzzle = null;
        ShakeStart = Long.MIN_VALUE / 2;
    }

    public static boolean isCharging(Player Holder) {
        ItemStack Stack = Holder.getMainHandItem();
        return UltralaserItem.isUltralaser(Stack) && Stack.get(GunsRegistry.Charging.get()) != null;
    }
}
