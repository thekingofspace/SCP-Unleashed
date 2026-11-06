package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunHeat;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.network.GunImpact;
import net.rai.scp_unleashed.guns.network.GunShotPayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GunEffects {
    public static final ResourceLocation HoleTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/misc/guns/bullet_hole.png");
    public static final int MaxHoles = 64;
    public static final int HoleLife = 400;
    public static final int HoleFade = 60;
    public static final float HoleSize = 0.09F;
    public static final int BeamLife = 16;
    public static final int TrailLife = 26;
    public static final double TracerSpeed = 22.0;
    public static final int FlashSpikes = 10;
    public static final ResourceLocation NailTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/misc/guns/nail.png");
    public static final int MaxNails = 96;
    public static final int NailLife = 160;
    public static final int NailSink = 12;
    public static final float NailLength = 0.18F;
    public static final float NailWidth = 0.014F;
    public static final float NailHead = 0.042F;
    public static final float NailDepth = 0.02F;
    public static final float NailShaftU = 0.25F;
    public static final float NailHeadU = 0.5F;
    public static final float NailHeadV = 0.5F;
    public static final double ParticleReach = 32.0;
    public static final double DetailReach = 16.0;
    public static final double MuzzleSlack = 3.0;
    public static final double TracerSlack = 2.0;
    public static final int ImpactBudget = 12;
    public static final int VentPuffEvery = 2;
    private static final List<Tracer> Tracers = new ArrayList<>();
    private static final List<Beam> Beams = new ArrayList<>();
    private static final List<Tracer> Trails = new ArrayList<>();
    private static final ArrayDeque<Hole> Holes = new ArrayDeque<>();
    private static final ArrayDeque<Nail> Nails = new ArrayDeque<>();
    private static long BudgetTick = Long.MIN_VALUE;
    private static int ImpactsThisTick;

    private GunEffects() {
    }

    public static void tick(Minecraft Game) {
        long Now = GunClientState.ticks();
        Tracers.removeIf(Shot -> Now - Shot.Born > 40 || Shot.spent(Now));
        Trails.removeIf(Shot -> Now - Shot.Born > TrailLife);
        Beams.removeIf(Shot -> Now - Shot.Born > BeamLife);
        while (!Holes.isEmpty() && Now - Holes.peekFirst().Born() > HoleLife) {
            Holes.pollFirst();
        }
        while (!Nails.isEmpty() && Now - Nails.peekFirst().Born() > NailLife) {
            Nails.pollFirst();
        }
        if (Game.level == null) {
            clear();
            return;
        }
        if (Now % VentPuffEvery == 0) {
            ventSmoke(Game, Game.level);
        }
    }

    private static void ventSmoke(Minecraft Game, ClientLevel World) {
        long Time = World.getGameTime();
        RandomSource Random = World.getRandom();
        for (Player Gunner : World.players()) {
            puff(Game, World, Gunner, Time, Random);
        }
        for (int ShooterId : GunClientState.shooters()) {
            if (World.getEntity(ShooterId) instanceof LivingEntity Gunner && !(Gunner instanceof Player)) {
                puff(Game, World, Gunner, Time, Random);
            }
        }
    }

    private static void puff(Minecraft Game, ClientLevel World, LivingEntity Gunner, long Time, RandomSource Random) {
        ItemStack Stack = Gunner.getMainHandItem();
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null || !GunHeat.venting(Stack, Time) || !within(Game, Gunner.getEyePosition(), ParticleReach)) {
            return;
        }
        Vec3 Muzzle;
        if (Gunner == Game.player && Game.options.getCameraType().isFirstPerson()) {
            Vec3 Look = Gunner.getLookAngle();
            Vec3 Right = Look.cross(new Vec3(0.0, 1.0, 0.0));
            Right = Right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Right.normalize();
            Muzzle = Gunner.getEyePosition().add(Look.scale(0.9)).add(Right.scale(0.22)).add(0.0, -0.18, 0.0);
        } else {
            Muzzle = GunMuzzle.thirdPersonWorld(Gunner, Stack, Kind, 1.0F);
        }
        World.addParticle(ParticleTypes.SMOKE, Muzzle.x, Muzzle.y, Muzzle.z, (Random.nextDouble() - 0.5) * 0.02, 0.03 + Random.nextDouble() * 0.02, (Random.nextDouble() - 0.5) * 0.02);
    }

    public static void clear() {
        Tracers.clear();
        Trails.clear();
        Beams.clear();
        Holes.clear();
        Nails.clear();
    }

    public static void onShot(GunShotPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        GunKind Kind = GunKind.byIndex(Payload.Kind());
        LocalPlayer User = Game.player;
        boolean Local = User != null && User.getId() == Payload.ShooterId();
        GunClientState.onShotSeen(Payload.ShooterId(), Kind, User);
        Vec3 Origin = Payload.Origin();
        long Now = GunClientState.ticks();
        RandomSource Random = World.getRandom();
        boolean Hit = false;
        boolean Kill = false;
        Vec3 Farthest = Origin;
        for (GunImpact Impact : Payload.Impacts()) {
            Vec3 Point = Impact.point(Origin);
            if (Point.distanceToSqr(Origin) > Farthest.distanceToSqr(Origin)) {
                Farthest = Point;
            }
            if (!Kind.getStats().isPiercing()) {
                Tracers.add(new Tracer(Payload.ShooterId(), Kind, Origin, Point, Now));
                if (Kind == GunKind.SniperRifle) {
                    Trails.add(new Tracer(Payload.ShooterId(), Kind, Origin, Point, Now));
                }
            }
            if (Impact.Type() == GunImpact.Surface) {
                blockImpact(World, Point, Impact.side(), Point.subtract(Origin).normalize(), Kind, Random, Now);
            } else if (Impact.isEntity()) {
                entityImpact(World, Point, Impact.Type() == GunImpact.Kill, Random);
                Hit = true;
                Kill |= Impact.Type() == GunImpact.Kill;
            }
        }
        if (Kind.getStats().isPiercing()) {
            Beams.add(new Beam(Payload.ShooterId(), Origin, Farthest, Now, Random.nextInt(1000)));
            beamParticles(World, Origin, Farthest, Random);
        }
        if (Local && Hit) {
            GunClientState.onHit(Kill);
            GunClientSounds.play(GunSounds.hitMarker(Kill), 1.0F, GunSounds.PitchJitter);
        }
        Entity Shooter = World.getEntity(Payload.ShooterId());
        if (!Local && Kind.getStats().isFirearm() && Shooter instanceof LivingEntity Gunner && GunItem.kindOf(Gunner.getMainHandItem()) == Kind
                && within(Game, Gunner.getEyePosition(), ParticleReach + MuzzleSlack)) {
            Vec3 Muzzle = GunMuzzle.thirdPersonWorld(Gunner, Gunner.getMainHandItem(), Kind, 1.0F);
            for (int Puff = 0; Puff < 2; Puff++) {
                World.addParticle(ParticleTypes.SMOKE, Muzzle.x, Muzzle.y, Muzzle.z, (Random.nextDouble() - 0.5) * 0.02, 0.02, (Random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    private static void blockImpact(ClientLevel World, Vec3 Point, Direction Face, Vec3 Heading, GunKind Kind, RandomSource Random, long Now) {
        Vec3 Normal = Vec3.atLowerCornerOf(Face.getNormal());
        BlockPos Pos = BlockPos.containing(Point.subtract(Normal.scale(0.05)));
        BlockState Struck = World.getBlockState(Pos);
        if (Struck.isAir()) {
            return;
        }
        boolean Pellets = Kind.getStats().getPellets() > 1;
        boolean Nailed = Kind == GunKind.Spiker;
        double Gap = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(Point);
        if (Gap <= ParticleReach * ParticleReach) {
            boolean Detailed = Gap <= DetailReach * DetailReach && spend(Now);
            int Chips = Pellets ? 3 : Nailed ? 2 : 7;
            int Sparks = Pellets ? 1 : Nailed ? 1 : 3;
            if (!Detailed) {
                Chips = Math.max(1, Chips / 2);
                Sparks = Math.max(1, Sparks / 2);
            }
            BlockParticleOption Debris = new BlockParticleOption(ParticleTypes.BLOCK, Struck);
            for (int Chip = 0; Chip < Chips; Chip++) {
                World.addParticle(Debris, Point.x, Point.y, Point.z,
                        Normal.x * 0.12 + (Random.nextDouble() - 0.5) * 0.1, Normal.y * 0.12 + Random.nextDouble() * 0.08, Normal.z * 0.12 + (Random.nextDouble() - 0.5) * 0.1);
            }
            World.addParticle(ParticleTypes.SMOKE, Point.x + Normal.x * 0.05, Point.y + Normal.y * 0.05, Point.z + Normal.z * 0.05, Normal.x * 0.015, 0.015, Normal.z * 0.015);
            for (int Spark = 0; Spark < Sparks; Spark++) {
                World.addParticle(GunsRegistry.Spark.get(), Point.x + Normal.x * 0.04, Point.y + Normal.y * 0.04, Point.z + Normal.z * 0.04,
                        Normal.x * 0.18 + (Random.nextDouble() - 0.5) * 0.22, Normal.y * 0.18 + Random.nextDouble() * 0.15, Normal.z * 0.18 + (Random.nextDouble() - 0.5) * 0.22);
            }
        }
        if (Nailed) {
            if (Nails.size() >= MaxNails) {
                Nails.pollFirst();
            }
            Nails.addLast(new Nail(Point, Heading, Now, Random.nextFloat() * Mth.TWO_PI));
            return;
        }
        if (Struck.isFaceSturdy(World, Pos, Face)) {
            if (Holes.size() >= MaxHoles) {
                Holes.pollFirst();
            }
            Holes.addLast(new Hole(Point, Face, Now, HoleSize * (0.85F + Random.nextFloat() * 0.3F), Random.nextFloat() * Mth.TWO_PI));
        }
    }

    private static boolean spend(long Now) {
        if (BudgetTick != Now) {
            BudgetTick = Now;
            ImpactsThisTick = 0;
        }
        return ImpactsThisTick++ < ImpactBudget;
    }

    private static boolean within(Minecraft Game, Vec3 Point, double Reach) {
        return Game.gameRenderer.getMainCamera().getPosition().distanceToSqr(Point) <= Reach * Reach;
    }

    private static void entityImpact(ClientLevel World, Vec3 Point, boolean Kill, RandomSource Random) {
        int Count = Kill ? 8 : 4;
        for (int Index = 0; Index < Count; Index++) {
            World.addParticle(ParticleTypes.CRIT, Point.x, Point.y, Point.z, (Random.nextDouble() - 0.5) * 0.4, Random.nextDouble() * 0.25, (Random.nextDouble() - 0.5) * 0.4);
        }
    }

    private static void beamParticles(ClientLevel World, Vec3 Start, Vec3 End, RandomSource Random) {
        Vec3 Span = End.subtract(Start);
        double Length = Span.length();
        int Count = Mth.clamp((int) (Length * 1.5), 4, 160);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Point = Start.add(Span.scale((Index + Random.nextDouble()) / Count));
            World.addParticle(GunsRegistry.EnergySpark.get(), Point.x, Point.y, Point.z, (Random.nextDouble() - 0.5) * 0.08, (Random.nextDouble() - 0.5) * 0.08, (Random.nextDouble() - 0.5) * 0.08);
        }
        World.addParticle(ParticleTypes.FLASH, End.x, End.y, End.z, 0.0, 0.0, 0.0);
        for (int Index = 0; Index < 28; Index++) {
            World.addParticle(GunsRegistry.EnergySpark.get(), End.x, End.y, End.z, (Random.nextDouble() - 0.5) * 0.6, (Random.nextDouble() - 0.5) * 0.6, (Random.nextDouble() - 0.5) * 0.6);
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera View = Event.getCamera();
        Vec3 Eye = View.getPosition();
        PoseStack.Pose Last = Event.getPoseStack().last();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        float Now = GunClientState.ticks() + Partial;
        if (!Holes.isEmpty()) {
            RenderType HoleType = RenderType.entityTranslucentCull(HoleTexture);
            VertexConsumer Consumer = Buffers.getBuffer(HoleType);
            for (Hole Mark : Holes) {
                drawHole(Consumer, Last, World, Mark, Eye, Now);
            }
            Buffers.endBatch(HoleType);
        }
        if (!Nails.isEmpty()) {
            RenderType NailType = RenderType.entityCutoutNoCull(NailTexture);
            VertexConsumer Consumer = Buffers.getBuffer(NailType);
            for (Nail Spike : Nails) {
                drawNail(Consumer, Last, World, Spike, Eye, Now);
            }
            Buffers.endBatch(NailType);
        }
        if (!Trails.isEmpty()) {
            VertexConsumer Consumer = Buffers.getBuffer(GunRenderTypes.Smoke);
            for (Tracer Shot : Trails) {
                Vec3 Start = resolve(Shot, Game, View, Event.getProjectionMatrix(), Partial);
                float Life = 1.0F - (Now - Shot.Born) / TrailLife;
                if (Life > 0.0F) {
                    float Width = 0.03F + 0.05F * (1.0F - Life);
                    segment(Consumer, Last, Start.subtract(Eye), Shot.End.subtract(Eye), Width, Width, 0.78F, 0.8F, 0.82F, 0.22F * Life, 0.22F * Life);
                }
            }
            Buffers.endBatch(GunRenderTypes.Smoke);
        }
        VertexConsumer Consumer = Buffers.getBuffer(GunRenderTypes.Glow);
        for (Tracer Shot : Tracers) {
            drawTracer(Consumer, Last, Shot, resolve(Shot, Game, View, Event.getProjectionMatrix(), Partial), Eye, Now);
        }
        for (Beam Shot : Beams) {
            drawBeam(Consumer, Last, Shot, resolveBeam(Shot, Game, View, Event.getProjectionMatrix(), Partial), Eye, Now);
        }
        boolean FirstPerson = Game.options.getCameraType().isFirstPerson();
        for (Player Gunner : World.players()) {
            if (Gunner == Game.player && FirstPerson) {
                continue;
            }
            float Flash = GunClientState.flash(Gunner.getId(), Partial);
            GunKind Kind = GunItem.kindOf(Gunner.getMainHandItem());
            if (Flash <= 0.0F || Kind == null || GunView.of(Kind).FlashSize() <= 0.0F) {
                continue;
            }
            Vec3 Muzzle = GunMuzzle.thirdPersonWorld(Gunner, Gunner.getMainHandItem(), Kind, Partial).subtract(Eye);
            Vec3 Forward = GunMuzzle.thirdPersonForward(Gunner, Partial);
            drawWorldFlash(Consumer, Last, Muzzle, Forward, Kind, Flash);
        }
        for (int ShooterId : GunClientState.shooters()) {
            if (!(World.getEntity(ShooterId) instanceof LivingEntity Gunner) || Gunner instanceof Player) {
                continue;
            }
            float Flash = GunClientState.flash(ShooterId, Partial);
            GunKind Kind = GunItem.kindOf(Gunner.getMainHandItem());
            if (Flash <= 0.0F || Kind == null || GunView.of(Kind).FlashSize() <= 0.0F) {
                continue;
            }
            Vec3 Muzzle = GunMuzzle.thirdPersonWorld(Gunner, Gunner.getMainHandItem(), Kind, Partial).subtract(Eye);
            drawWorldFlash(Consumer, Last, Muzzle, GunMuzzle.thirdPersonForward(Gunner, Partial), Kind, Flash);
        }
        Buffers.endBatch(GunRenderTypes.Glow);
    }

    private static Vec3 resolve(Tracer Shot, Minecraft Game, Camera View, Matrix4f Projection, float Partial) {
        if (Shot.Start == null) {
            Shot.Start = muzzleOf(Shot.ShooterId, Shot.Kind, Shot.Origin, Game, View, Projection, Partial);
        }
        return Shot.Start;
    }

    private static Vec3 resolveBeam(Beam Shot, Minecraft Game, Camera View, Matrix4f Projection, float Partial) {
        if (Shot.Start == null) {
            Shot.Start = muzzleOf(Shot.ShooterId, GunKind.ParticleDisruptor, Shot.Origin, Game, View, Projection, Partial);
        }
        return Shot.Start;
    }

    private static Vec3 muzzleOf(int ShooterId, GunKind Kind, Vec3 Origin, Minecraft Game, Camera View, Matrix4f Projection, float Partial) {
        if (Game.level == null) {
            return Origin;
        }
        Entity Shooter = Game.level.getEntity(ShooterId);
        if (!(Shooter instanceof LivingEntity Gunner)) {
            return Origin;
        }
        ItemStack Stack = Gunner.getMainHandItem();
        if (GunItem.kindOf(Stack) != Kind) {
            return Origin.add(Gunner.getLookAngle().scale(0.6)).add(0.0, -0.2, 0.0);
        }
        if (Gunner == Game.player && Game.options.getCameraType().isFirstPerson() && Gunner instanceof LocalPlayer Local) {
            return GunMuzzle.firstPersonWorld(Local, Stack, Kind, View, Projection, Partial);
        }
        return GunMuzzle.thirdPersonWorld(Gunner, Stack, Kind, Partial);
    }

    private static void drawTracer(VertexConsumer Consumer, PoseStack.Pose Last, Tracer Shot, Vec3 Start, Vec3 Eye, float Now) {
        Vec3 Span = Shot.End.subtract(Start);
        double Total = Span.length();
        if (Total < 0.3) {
            return;
        }
        double Head = TracerSpeed * Math.max(0.0F, Now - Shot.Born + 0.35F);
        double Length = tracerLength(Shot.Kind);
        double Tail = Head - Length;
        if (Tail >= Total) {
            return;
        }
        double From = Math.max(0.0, Tail);
        double To = Math.min(Total, Head);
        if (To - From < 0.05) {
            return;
        }
        Vec3 Heading = Span.scale(1.0 / Total);
        Vec3 A = Start.add(Heading.scale(From)).subtract(Eye);
        Vec3 B = Start.add(Heading.scale(To)).subtract(Eye);
        float TailAlpha = Tail <= 0.0 ? (float) (1.0 - Math.min(1.0, From / Length)) * 0.4F : 0.0F;
        if (Shot.Kind == GunKind.Spiker) {
            segment(Consumer, Last, A, B, 0.006F, 0.012F, 0.82F, 0.88F, 0.95F, TailAlpha * 0.5F, 0.55F);
            return;
        }
        if (Shot.Kind == GunKind.ScarletRifle) {
            segment(Consumer, Last, A, B, 0.075F, 0.075F, 0.86F, 0.06F, 0.1F, TailAlpha * 0.5F, 0.42F);
            segment(Consumer, Last, A, B, 0.018F, 0.028F, 1.0F, 0.52F, 0.42F, TailAlpha, 0.95F);
            return;
        }
        float Width = Shot.Kind == GunKind.SniperRifle ? 0.05F : Shot.Kind.getStats().getPellets() > 1 ? 0.018F : 0.028F;
        segment(Consumer, Last, A, B, Width * 2.4F, Width * 2.4F, 1.0F, 0.62F, 0.25F, TailAlpha * 0.4F, 0.28F);
        segment(Consumer, Last, A, B, Width * 0.6F, Width, 1.0F, 0.93F, 0.72F, TailAlpha, 0.95F);
    }

    private static double tracerLength(GunKind Kind) {
        return Kind == GunKind.SniperRifle ? 9.0 : Kind == GunKind.Spiker ? 1.2 : 4.5;
    }

    private static void drawBeam(VertexConsumer Consumer, PoseStack.Pose Last, Beam Shot, Vec3 Start, Vec3 Eye, float Now) {
        float Age = Now - Shot.Born;
        float Life = Mth.clamp(1.0F - Age / BeamLife, 0.0F, 1.0F);
        if (Life <= 0.0F) {
            return;
        }
        float Burst = Age < 2.0F ? 1.0F + (2.0F - Age) * 0.35F : 1.0F;
        float Intensity = Life * Life * (3.0F - 2.0F * Life);
        Vec3 A = Start.subtract(Eye);
        Vec3 B = Shot.End.subtract(Eye);
        Vec3 Span = B.subtract(A);
        double Length = Span.length();
        if (Length < 0.1) {
            return;
        }
        float Swell = 1.0F + 0.6F * (1.0F - Life);
        segment(Consumer, Last, A, B, 0.34F * Swell * Burst, 0.42F * Swell * Burst, 0.16F, 0.9F, 0.8F, 0.32F * Intensity, 0.32F * Intensity);
        segment(Consumer, Last, A, B, 0.14F * Burst, 0.17F * Burst, 0.45F, 1.0F, 0.95F, 0.7F * Intensity, 0.7F * Intensity);
        segment(Consumer, Last, A, B, 0.05F * Burst, 0.06F * Burst, 0.92F, 1.0F, 1.0F, Intensity, Intensity);
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Helper = Math.abs(Forward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 AxisA = Forward.cross(Helper).normalize();
        Vec3 AxisB = Forward.cross(AxisA).normalize();
        int Steps = Mth.clamp((int) (Length * 6.0), 12, 720);
        for (int Strand = 0; Strand < 2; Strand++) {
            float Red = Strand == 0 ? 0.55F : 0.35F;
            float Green = Strand == 0 ? 0.45F : 1.0F;
            float Blue = 1.0F;
            Vec3 Previous = null;
            for (int Index = 0; Index <= Steps; Index++) {
                double Along = Length * Index / Steps;
                double Phase = Along * 4.2 - Age * 1.1 + Strand * Math.PI + Shot.Seed;
                double Radius = (0.16 + 0.12 * (1.0 - Life)) * Math.min(1.0, Along / 0.8);
                Vec3 Point = A.add(Forward.scale(Along)).add(AxisA.scale(Math.cos(Phase) * Radius)).add(AxisB.scale(Math.sin(Phase) * Radius));
                if (Previous != null) {
                    segment(Consumer, Last, Previous, Point, 0.035F, 0.035F, Red, Green, Blue, 0.75F * Intensity, 0.75F * Intensity);
                }
                Previous = Point;
            }
        }
        glow(Consumer, Last, A, 0.3F * Burst, 0.5F, 1.0F, 0.95F, 0.8F * Intensity);
        glow(Consumer, Last, B, 0.9F * Swell, 0.2F, 0.95F, 0.85F, 0.6F * Intensity);
        glow(Consumer, Last, B, 0.3F, 0.95F, 1.0F, 1.0F, 0.9F * Intensity);
    }

    private static void drawWorldFlash(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Muzzle, Vec3 Forward, GunKind Kind, float Flash) {
        GunView View = GunView.of(Kind);
        float Size = View.FlashSize() * (0.75F + 0.25F * Flash);
        boolean Energy = Kind.getStats().isPiercing();
        float Red = Energy ? 0.45F : 1.0F;
        float Green = Energy ? 1.0F : 0.78F;
        float Blue = Energy ? 0.95F : 0.35F;
        if (Kind == GunKind.ScarletRifle) {
            Green = 0.18F;
            Blue = 0.16F;
        }
        star(Consumer, Last, Muzzle, Size, Red, Green, Blue, Flash);
        Vec3 Tip = Muzzle.add(Forward.scale(Size * 2.2));
        segment(Consumer, Last, Muzzle, Tip, Size * 0.55F, 0.0F, Red, Green, Blue, 0.85F * Flash, 0.0F);
    }

    public static void renderHandFlash(PoseStack Pose, MultiBufferSource Buffers, GunKind Kind, float Flash, float Scale) {
        GunView View = GunView.of(Kind);
        VertexConsumer Consumer = Buffers.getBuffer(GunRenderTypes.HandGlow);
        PoseStack.Pose Last = Pose.last();
        float Size = View.FlashSize() / Math.max(0.05F, Scale) * (0.7F + 0.3F * Flash);
        boolean Energy = Kind.getStats().isPiercing();
        float Red = Energy ? 0.45F : 1.0F;
        float Green = Energy ? 1.0F : 0.8F;
        float Blue = Energy ? 0.95F : 0.38F;
        if (Kind == GunKind.ScarletRifle) {
            Green = 0.2F;
            Blue = 0.18F;
        }
        float X = 0.5F;
        float Y = View.MuzzleY() / 16.0F;
        float Z = View.MuzzleZ() / 16.0F - 0.02F;
        Vec3 Centre = new Vec3(X, Y, Z);
        planeStar(Consumer, Last, Centre, Size, Red, Green, Blue, Flash);
        float Reach = Size * 2.4F;
        quad(Consumer, Last, X - Size * 0.35F, Y, Z, X + Size * 0.35F, Y, Z, X, Y, Z - Reach, X, Y, Z - Reach, Red, Green, Blue, Flash * 0.85F, 0.0F);
        quad(Consumer, Last, X, Y - Size * 0.35F, Z, X, Y + Size * 0.35F, Z, X, Y, Z - Reach, X, Y, Z - Reach, Red, Green, Blue, Flash * 0.85F, 0.0F);
    }

    private static void planeStar(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, float Size, float Red, float Green, float Blue, float Alpha) {
        for (int Index = 0; Index < FlashSpikes * 2; Index++) {
            double First = Mth.TWO_PI * Index / (FlashSpikes * 2);
            double Second = Mth.TWO_PI * (Index + 1) / (FlashSpikes * 2);
            double RadiusFirst = Index % 2 == 0 ? Size : Size * 0.42;
            double RadiusSecond = Index % 2 == 0 ? Size * 0.42 : Size;
            float Ax = (float) (Centre.x + Math.cos(First) * RadiusFirst);
            float Ay = (float) (Centre.y + Math.sin(First) * RadiusFirst);
            float Bx = (float) (Centre.x + Math.cos(Second) * RadiusSecond);
            float By = (float) (Centre.y + Math.sin(Second) * RadiusSecond);
            float Cz = (float) Centre.z;
            Consumer.addVertex(Last, (float) Centre.x, (float) Centre.y, Cz).setColor(1.0F, mixWhite(Green, 0.6F), mixWhite(Blue, 0.5F), Alpha);
            Consumer.addVertex(Last, Ax, Ay, Cz).setColor(Red, Green, Blue, 0.0F);
            Consumer.addVertex(Last, Bx, By, Cz).setColor(Red, Green, Blue, 0.0F);
            Consumer.addVertex(Last, (float) Centre.x, (float) Centre.y, Cz).setColor(1.0F, mixWhite(Green, 0.6F), mixWhite(Blue, 0.5F), Alpha);
        }
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, float Ax, float Ay, float Az, float Bx, float By, float Bz, float Cx, float Cy, float Cz, float Dx, float Dy, float Dz,
                             float Red, float Green, float Blue, float Near, float Far) {
        Consumer.addVertex(Last, Ax, Ay, Az).setColor(Red, Green, Blue, Near);
        Consumer.addVertex(Last, Bx, By, Bz).setColor(Red, Green, Blue, Near);
        Consumer.addVertex(Last, Cx, Cy, Cz).setColor(Red, Green, Blue, Far);
        Consumer.addVertex(Last, Dx, Dy, Dz).setColor(Red, Green, Blue, Far);
    }

    private static void star(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, float Size, float Red, float Green, float Blue, float Alpha) {
        Vec3 Facing = Centre.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : Centre.scale(-1.0).normalize();
        Vec3 Across = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Across = Across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        Vec3 Over = Across.cross(Facing).normalize();
        for (int Index = 0; Index < FlashSpikes * 2; Index++) {
            double First = Mth.TWO_PI * Index / (FlashSpikes * 2);
            double Second = Mth.TWO_PI * (Index + 1) / (FlashSpikes * 2);
            double RadiusFirst = Index % 2 == 0 ? Size : Size * 0.42;
            double RadiusSecond = Index % 2 == 0 ? Size * 0.42 : Size;
            Vec3 RimFrom = Centre.add(Across.scale(Math.cos(First) * RadiusFirst)).add(Over.scale(Math.sin(First) * RadiusFirst));
            Vec3 RimTo = Centre.add(Across.scale(Math.cos(Second) * RadiusSecond)).add(Over.scale(Math.sin(Second) * RadiusSecond));
            vertex(Consumer, Last, Centre, 1.0F, mixWhite(Green, 0.6F), mixWhite(Blue, 0.5F), Alpha);
            vertex(Consumer, Last, RimFrom, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, RimTo, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, Centre, 1.0F, mixWhite(Green, 0.6F), mixWhite(Blue, 0.5F), Alpha);
        }
    }

    private static void glow(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, float Radius, float Red, float Green, float Blue, float Alpha) {
        Vec3 Facing = Centre.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : Centre.scale(-1.0).normalize();
        Vec3 Across = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Across = Across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        Vec3 Over = Across.cross(Facing).normalize();
        int Sides = 14;
        for (int Index = 0; Index < Sides; Index++) {
            double First = Mth.TWO_PI * Index / Sides;
            double Second = Mth.TWO_PI * (Index + 1) / Sides;
            Vec3 RimFrom = Centre.add(Across.scale(Math.cos(First) * Radius)).add(Over.scale(Math.sin(First) * Radius));
            Vec3 RimTo = Centre.add(Across.scale(Math.cos(Second) * Radius)).add(Over.scale(Math.sin(Second) * Radius));
            vertex(Consumer, Last, Centre, Red, Green, Blue, Alpha);
            vertex(Consumer, Last, RimFrom, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, RimTo, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, Centre, Red, Green, Blue, Alpha);
        }
    }

    private static void segment(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 From, Vec3 To, float WidthFrom, float WidthTo, float Red, float Green, float Blue,
                                float AlphaFrom, float AlphaTo) {
        Vec3 Tangent = To.subtract(From);
        Vec3 Middle = From.add(To).scale(0.5);
        Vec3 Side = Tangent.cross(Middle.scale(-1.0));
        double Length = Side.length();
        Side = Length < 1.0E-7 ? new Vec3(0.0, 1.0, 0.0) : Side.scale(1.0 / Length);
        Vec3 SideFrom = Side.scale(WidthFrom * 0.5);
        Vec3 SideTo = Side.scale(WidthTo * 0.5);
        vertex(Consumer, Last, From.subtract(SideFrom), Red, Green, Blue, 0.0F);
        vertex(Consumer, Last, From, Red, Green, Blue, AlphaFrom);
        vertex(Consumer, Last, To, Red, Green, Blue, AlphaTo);
        vertex(Consumer, Last, To.subtract(SideTo), Red, Green, Blue, 0.0F);
        vertex(Consumer, Last, From, Red, Green, Blue, AlphaFrom);
        vertex(Consumer, Last, From.add(SideFrom), Red, Green, Blue, 0.0F);
        vertex(Consumer, Last, To.add(SideTo), Red, Green, Blue, 0.0F);
        vertex(Consumer, Last, To, Red, Green, Blue, AlphaTo);
    }

    private static void drawHole(VertexConsumer Consumer, PoseStack.Pose Last, ClientLevel World, Hole Mark, Vec3 Eye, float Now) {
        float Age = Now - Mark.Born();
        float Alpha = Mth.clamp((HoleLife - Age) / HoleFade, 0.0F, 1.0F);
        if (Alpha <= 0.0F) {
            return;
        }
        Vec3 Normal = Vec3.atLowerCornerOf(Mark.Face().getNormal());
        Vec3 Helper = Math.abs(Normal.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 AxisU = Normal.cross(Helper).normalize();
        Vec3 AxisV = Normal.cross(AxisU).normalize();
        double Cos = Math.cos(Mark.Turn()) * Mark.Size();
        double Sin = Math.sin(Mark.Turn()) * Mark.Size();
        Vec3 U = AxisU.scale(Cos).add(AxisV.scale(Sin));
        Vec3 V = AxisV.scale(Cos).subtract(AxisU.scale(Sin));
        Vec3 Centre = Mark.Point().add(Normal.scale(0.004)).subtract(Eye);
        int Light = LevelRenderer.getLightColor(World, BlockPos.containing(Mark.Point().add(Normal.scale(0.1))));
        float Nx = (float) Normal.x;
        float Ny = (float) Normal.y;
        float Nz = (float) Normal.z;
        holeVertex(Consumer, Last, Centre.subtract(U).subtract(V), 0.0F, 1.0F, Alpha, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Centre.add(U).subtract(V), 1.0F, 1.0F, Alpha, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Centre.add(U).add(V), 1.0F, 0.0F, Alpha, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Centre.subtract(U).add(V), 0.0F, 0.0F, Alpha, Light, Nx, Ny, Nz);
    }

    private static void drawNail(VertexConsumer Consumer, PoseStack.Pose Last, ClientLevel World, Nail Spike, Vec3 Eye, float Now) {
        float Age = Now - Spike.Born();
        float Out = NailLength * Mth.clamp((NailLife - Age) / NailSink, 0.0F, 1.0F);
        if (Out <= 0.005F) {
            return;
        }
        Vec3 Back = Spike.Axis().scale(-1.0);
        Vec3 Helper = Math.abs(Back.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 SideA = Back.cross(Helper).normalize();
        Vec3 SideB = Back.cross(SideA).normalize();
        Vec3 U = SideA.scale(Math.cos(Spike.Turn())).add(SideB.scale(Math.sin(Spike.Turn())));
        Vec3 V = SideB.scale(Math.cos(Spike.Turn())).subtract(SideA.scale(Math.sin(Spike.Turn())));
        Vec3 Base = Spike.Point().subtract(Back.scale(NailDepth)).subtract(Eye);
        Vec3 Tip = Spike.Point().add(Back.scale(Out)).subtract(Eye);
        int Light = LevelRenderer.getLightColor(World, BlockPos.containing(Spike.Point().add(Back.scale(0.2))));
        shaft(Consumer, Last, Base, Tip, U.scale(NailWidth), V, Light);
        shaft(Consumer, Last, Base, Tip, V.scale(NailWidth), U, Light);
        Vec3 HeadU = U.scale(NailHead);
        Vec3 HeadV = V.scale(NailHead);
        float Nx = (float) Back.x;
        float Ny = (float) Back.y;
        float Nz = (float) Back.z;
        holeVertex(Consumer, Last, Tip.subtract(HeadU).subtract(HeadV), NailHeadU, NailHeadV, 1.0F, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Tip.add(HeadU).subtract(HeadV), 1.0F, NailHeadV, 1.0F, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Tip.add(HeadU).add(HeadV), 1.0F, 0.0F, 1.0F, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Tip.subtract(HeadU).add(HeadV), NailHeadU, 0.0F, 1.0F, Light, Nx, Ny, Nz);
    }

    private static void shaft(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Base, Vec3 Tip, Vec3 Across, Vec3 Facing, int Light) {
        float Nx = (float) Facing.x;
        float Ny = (float) Facing.y;
        float Nz = (float) Facing.z;
        holeVertex(Consumer, Last, Base.subtract(Across), 0.0F, 1.0F, 1.0F, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Base.add(Across), NailShaftU, 1.0F, 1.0F, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Tip.add(Across), NailShaftU, 0.0F, 1.0F, Light, Nx, Ny, Nz);
        holeVertex(Consumer, Last, Tip.subtract(Across), 0.0F, 0.0F, 1.0F, Light, Nx, Ny, Nz);
    }

    private static void holeVertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float U, float V, float Alpha, int Light, float Nx, float Ny, float Nz) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(1.0F, 1.0F, 1.0F, Alpha).setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, Nx, Ny, Nz);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float Red, float Green, float Blue, float Alpha) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Red, Green, Blue, Mth.clamp(Alpha, 0.0F, 1.0F));
    }

    private static float mixWhite(float Channel, float Amount) {
        return Channel + (1.0F - Channel) * Amount;
    }

    private static final class Tracer {
        private final int ShooterId;
        private final GunKind Kind;
        private final Vec3 Origin;
        private final Vec3 End;
        private final long Born;
        @Nullable
        private Vec3 Start;

        private Tracer(int ShooterId, GunKind Kind, Vec3 Origin, Vec3 End, long Born) {
            this.ShooterId = ShooterId;
            this.Kind = Kind;
            this.Origin = Origin;
            this.End = End;
            this.Born = Born;
        }

        private boolean spent(long Now) {
            double Tail = TracerSpeed * (Now - Born + 0.35) - tracerLength(Kind);
            return Tail >= Origin.distanceTo(End) + TracerSlack;
        }
    }

    private static final class Beam {
        private final int ShooterId;
        private final Vec3 Origin;
        private final Vec3 End;
        private final long Born;
        private final int Seed;
        @Nullable
        private Vec3 Start;

        private Beam(int ShooterId, Vec3 Origin, Vec3 End, long Born, int Seed) {
            this.ShooterId = ShooterId;
            this.Origin = Origin;
            this.End = End;
            this.Born = Born;
            this.Seed = Seed;
        }
    }

    private record Hole(Vec3 Point, Direction Face, long Born, float Size, float Turn) {
    }

    private record Nail(Vec3 Point, Vec3 Axis, long Born, float Turn) {
    }
}
