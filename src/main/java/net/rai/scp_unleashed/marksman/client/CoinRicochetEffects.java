package net.rai.scp_unleashed.marksman.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.client.GunClientSounds;
import net.rai.scp_unleashed.guns.client.GunClientState;
import net.rai.scp_unleashed.guns.client.GunEffects;
import net.rai.scp_unleashed.guns.client.GunMuzzle;
import net.rai.scp_unleashed.guns.client.GunRenderTypes;
import net.rai.scp_unleashed.marksman.MarksmanRegistry;
import net.rai.scp_unleashed.marksman.network.CoinRicochetPayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class CoinRicochetEffects {
    public static final float LinkPause = 1.0F;
    public static final float TrailLife = 12.0F;
    public static final float FlashLife = 6.0F;
    public static final float PopLife = 5.0F;
    public static final double StreakLength = 6.0;
    public static final float CoreWidth = 0.045F;
    public static final float HaloWidth = 0.14F;
    public static final float TrailWidth = 0.03F;
    public static final float TrailAlpha = 0.7F;
    public static final float LeadAlpha = 0.45F;
    public static final float FlashSize = 0.7F;
    public static final int FlashSpikes = 4;
    public static final int MaxShots = 32;
    private static final List<Ricochet> Shots = new ArrayList<>();

    private CoinRicochetEffects() {
    }

    public static void onRicochet(CoinRicochetPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null || Payload.Path().size() < 2) {
            return;
        }
        List<Vec3> Path = List.copyOf(Payload.Path());
        Entity Shooter = World.getEntity(Payload.ShooterId());
        Vec3 Origin = Shooter == null ? Path.get(0) : Shooter.getEyePosition();
        float Lead = (float) (Origin.distanceTo(Path.get(0)) / GunEffects.TracerSpeed);
        if (Shots.size() >= MaxShots) {
            Shots.remove(0);
        }
        Shots.add(new Ricochet(Payload.ShooterId(), Origin, Path, Payload.coins(), Payload.Outcome(), GunClientState.ticks(), Lead));
        RandomSource Random = World.getRandom();
        for (int Index = 0; Index < Payload.coins(); Index++) {
            Vec3 Point = Path.get(Index);
            burst(World, Point, Random);
        }
        Vec3 End = Path.get(Path.size() - 1);
        Vec3 Heading = End.subtract(Path.get(Path.size() - 2)).normalize();
        if (Payload.struck()) {
            int Count = Payload.Outcome() == CoinRicochetPayload.Kill ? 10 : 6;
            for (int Index = 0; Index < Count; Index++) {
                World.addParticle(ParticleTypes.CRIT, End.x, End.y, End.z, (Random.nextDouble() - 0.5) * 0.4, Random.nextDouble() * 0.25, (Random.nextDouble() - 0.5) * 0.4);
            }
        } else if (Payload.Outcome() == CoinRicochetPayload.StrayBlock) {
            surface(World, End, Direction.from3DDataValue(Payload.Face()), Random);
        } else if (Payload.Outcome() == CoinRicochetPayload.Shrugged) {
            for (int Index = 0; Index < 5; Index++) {
                World.addParticle(GunsRegistry.Spark.get(), End.x, End.y, End.z, -Heading.x * 0.2 + (Random.nextDouble() - 0.5) * 0.3, Random.nextDouble() * 0.2,
                        -Heading.z * 0.2 + (Random.nextDouble() - 0.5) * 0.3);
            }
        }
        LocalPlayer User = Game.player;
        if (User != null && User.getId() == Payload.ShooterId() && Payload.struck()) {
            boolean Kill = Payload.Outcome() == CoinRicochetPayload.Kill;
            GunClientState.onHit(Kill);
            GunClientSounds.play(GunSounds.hitMarker(Kill), 1.0F, GunSounds.PitchJitter);
        }
    }

    private static void burst(ClientLevel World, Vec3 Point, RandomSource Random) {
        for (int Index = 0; Index < 10; Index++) {
            World.addParticle(MarksmanRegistry.Glint.get(), Point.x, Point.y, Point.z, (Random.nextDouble() - 0.5) * 0.35, (Random.nextDouble() - 0.5) * 0.35,
                    (Random.nextDouble() - 0.5) * 0.35);
        }
        for (int Index = 0; Index < 6; Index++) {
            World.addParticle(GunsRegistry.Spark.get(), Point.x, Point.y, Point.z, (Random.nextDouble() - 0.5) * 0.45, Random.nextDouble() * 0.25, (Random.nextDouble() - 0.5) * 0.45);
        }
    }

    private static void surface(ClientLevel World, Vec3 Point, Direction Face, RandomSource Random) {
        Vec3 Normal = Vec3.atLowerCornerOf(Face.getNormal());
        BlockPos Pos = BlockPos.containing(Point.subtract(Normal.scale(0.05)));
        BlockState Struck = World.getBlockState(Pos);
        if (!Struck.isAir()) {
            for (int Chip = 0; Chip < 6; Chip++) {
                World.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Struck), Point.x, Point.y, Point.z,
                        Normal.x * 0.12 + (Random.nextDouble() - 0.5) * 0.1, Normal.y * 0.12 + Random.nextDouble() * 0.08, Normal.z * 0.12 + (Random.nextDouble() - 0.5) * 0.1);
            }
        }
        World.addParticle(ParticleTypes.SMOKE, Point.x + Normal.x * 0.05, Point.y + Normal.y * 0.05, Point.z + Normal.z * 0.05, Normal.x * 0.015, 0.015, Normal.z * 0.015);
        for (int Spark = 0; Spark < 3; Spark++) {
            World.addParticle(GunsRegistry.Spark.get(), Point.x + Normal.x * 0.04, Point.y + Normal.y * 0.04, Point.z + Normal.z * 0.04,
                    Normal.x * 0.18 + (Random.nextDouble() - 0.5) * 0.22, Normal.y * 0.18 + Random.nextDouble() * 0.15, Normal.z * 0.18 + (Random.nextDouble() - 0.5) * 0.22);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        if (Minecraft.getInstance().level == null) {
            Shots.clear();
            return;
        }
        long Now = GunClientState.ticks();
        Shots.removeIf(Shot -> Now - Shot.Born > Shot.Lifetime);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Shots.clear();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || Shots.isEmpty()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera View = Event.getCamera();
        Vec3 Eye = View.getPosition();
        PoseStack.Pose Last = Event.getPoseStack().last();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        float Now = GunClientState.ticks() + Partial;
        VertexConsumer Consumer = Buffers.getBuffer(GunRenderTypes.Glow);
        for (Ricochet Shot : Shots) {
            draw(Consumer, Last, Shot, muzzle(Shot, Game, View, Event.getProjectionMatrix(), Partial), Eye, Now);
        }
        Buffers.endBatch(GunRenderTypes.Glow);
    }

    private static Vec3 muzzle(Ricochet Shot, Minecraft Game, Camera View, Matrix4f Projection, float Partial) {
        if (Shot.Muzzle != null) {
            return Shot.Muzzle;
        }
        Vec3 Result = Shot.Origin;
        Entity Shooter = Game.level == null ? null : Game.level.getEntity(Shot.ShooterId);
        if (Shooter instanceof Player Gunner) {
            ItemStack Stack = Gunner.getMainHandItem();
            GunKind Kind = GunItem.kindOf(Stack);
            if (Kind == null) {
                Result = Shot.Origin.add(Gunner.getLookAngle().scale(0.6)).add(0.0, -0.2, 0.0);
            } else if (Gunner == Game.player && Game.options.getCameraType().isFirstPerson() && Gunner instanceof LocalPlayer Local) {
                Result = GunMuzzle.firstPersonWorld(Local, Stack, Kind, View, Projection, Partial);
            } else {
                Result = GunMuzzle.thirdPersonWorld(Gunner, Stack, Kind, Partial);
            }
        }
        Shot.Muzzle = Result;
        return Result;
    }

    private static void draw(VertexConsumer Consumer, PoseStack.Pose Last, Ricochet Shot, Vec3 Muzzle, Vec3 Eye, float Now) {
        float Age = Now - Shot.Born;
        if (Age < 0.0F) {
            return;
        }
        List<Vec3> Path = Shot.Path;
        trail(Consumer, Last, Muzzle, Path.get(0), Age * GunEffects.TracerSpeed, Age, LeadAlpha, Eye);
        float Arrival = Shot.Lead;
        for (int Index = 0; Index < Path.size() - 1; Index++) {
            Vec3 From = Path.get(Index);
            Vec3 To = Path.get(Index + 1);
            float SinceHit = Age - Arrival;
            if (SinceHit < 0.0F) {
                return;
            }
            if (Index < Shot.Coins && SinceHit <= FlashLife) {
                flash(Consumer, Last, From.subtract(Eye), 1.0F - SinceHit / FlashLife, SinceHit);
            }
            float Departed = SinceHit - LinkPause;
            double Total = From.distanceTo(To);
            if (Departed > 0.0F) {
                double Head = Departed * GunEffects.TracerSpeed;
                trail(Consumer, Last, From, To, Head, Departed, TrailAlpha, Eye);
                streak(Consumer, Last, From, To, Head, Total, Eye);
                float Landed = Departed - (float) (Total / GunEffects.TracerSpeed);
                if (Index == Path.size() - 2 && Landed >= 0.0F && Landed <= PopLife && Shot.Outcome != CoinRicochetPayload.Stray) {
                    float Strength = 1.0F - Landed / PopLife;
                    boolean Struck = Shot.Outcome == CoinRicochetPayload.Wound || Shot.Outcome == CoinRicochetPayload.Kill;
                    glow(Consumer, Last, To.subtract(Eye), 0.35F * Strength + 0.1F, 1.0F, Struck ? 0.45F : 0.75F, Struck ? 0.3F : 0.35F, 0.7F * Strength);
                }
            }
            Arrival += LinkPause + (float) (Total / GunEffects.TracerSpeed);
        }
    }

    private static void trail(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 From, Vec3 To, double Head, float Since, float Alpha, Vec3 Eye) {
        double Total = From.distanceTo(To);
        float Fade = Mth.clamp(1.0F - Since / TrailLife, 0.0F, 1.0F);
        if (Total < 0.05 || Head <= 0.0 || Fade <= 0.0F) {
            return;
        }
        Vec3 Heading = To.subtract(From).scale(1.0 / Total);
        Vec3 Reach = From.add(Heading.scale(Math.min(Total, Head)));
        float Shade = Alpha * Fade * Fade;
        segment(Consumer, Last, From.subtract(Eye), Reach.subtract(Eye), TrailWidth * 3.0F, TrailWidth * 3.0F, 1.0F, 0.62F, 0.18F, Shade * 0.35F, Shade * 0.35F);
        segment(Consumer, Last, From.subtract(Eye), Reach.subtract(Eye), TrailWidth, TrailWidth, 1.0F, 0.9F, 0.6F, Shade, Shade);
    }

    private static void streak(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 From, Vec3 To, double Head, double Total, Vec3 Eye) {
        double Tail = Head - StreakLength;
        if (Total < 0.05 || Tail >= Total) {
            return;
        }
        double Near = Math.max(0.0, Tail);
        double Far = Math.min(Total, Head);
        if (Far - Near < 0.05) {
            return;
        }
        Vec3 Heading = To.subtract(From).scale(1.0 / Total);
        Vec3 A = From.add(Heading.scale(Near)).subtract(Eye);
        Vec3 B = From.add(Heading.scale(Far)).subtract(Eye);
        segment(Consumer, Last, A, B, HaloWidth, HaloWidth, 1.0F, 0.66F, 0.2F, 0.1F, 0.45F);
        segment(Consumer, Last, A, B, CoreWidth * 0.6F, CoreWidth, 1.0F, 0.95F, 0.75F, 0.35F, 1.0F);
    }

    private static void flash(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, float Strength, float SinceHit) {
        float Pop = SinceHit < 1.5F ? 1.0F + (1.5F - SinceHit) * 0.4F : 1.0F;
        glow(Consumer, Last, Centre, 0.55F * Pop * (0.5F + 0.5F * Strength), 1.0F, 0.7F, 0.25F, 0.6F * Strength);
        glow(Consumer, Last, Centre, 0.2F * Pop, 1.0F, 0.97F, 0.85F, Strength);
        star(Consumer, Last, Centre, FlashSize * Pop * (0.4F + 0.6F * Strength), SinceHit * 0.35F, 1.0F, 0.92F, 0.62F, Strength);
    }

    private static void star(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, float Size, float Twist, float Red, float Green, float Blue, float Alpha) {
        Vec3 Facing = Centre.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : Centre.scale(-1.0).normalize();
        Vec3 Across = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Across = Across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        Vec3 Over = Across.cross(Facing).normalize();
        int Points = FlashSpikes * 2;
        for (int Index = 0; Index < Points; Index++) {
            double First = Mth.TWO_PI * Index / Points + Twist;
            double Second = Mth.TWO_PI * (Index + 1) / Points + Twist;
            double RadiusFirst = Index % 2 == 0 ? Size : Size * 0.14;
            double RadiusSecond = Index % 2 == 0 ? Size * 0.14 : Size;
            Vec3 RimFrom = Centre.add(Across.scale(Math.cos(First) * RadiusFirst)).add(Over.scale(Math.sin(First) * RadiusFirst));
            Vec3 RimTo = Centre.add(Across.scale(Math.cos(Second) * RadiusSecond)).add(Over.scale(Math.sin(Second) * RadiusSecond));
            vertex(Consumer, Last, Centre, Red, Green, Blue, Alpha);
            vertex(Consumer, Last, RimFrom, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, RimTo, Red, Green, Blue, 0.0F);
            vertex(Consumer, Last, Centre, Red, Green, Blue, Alpha);
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

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float Red, float Green, float Blue, float Alpha) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Red, Green, Blue, Mth.clamp(Alpha, 0.0F, 1.0F));
    }

    private static final class Ricochet {
        private final int ShooterId;
        private final Vec3 Origin;
        private final List<Vec3> Path;
        private final int Coins;
        private final int Outcome;
        private final long Born;
        private final float Lead;
        private final long Lifetime;
        @Nullable
        private Vec3 Muzzle;

        private Ricochet(int ShooterId, Vec3 Origin, List<Vec3> Path, int Coins, int Outcome, long Born, float Lead) {
            this.ShooterId = ShooterId;
            this.Origin = Origin;
            this.Path = Path;
            this.Coins = Coins;
            this.Outcome = Outcome;
            this.Born = Born;
            this.Lead = Lead;
            double Length = 0.0;
            for (int Index = 0; Index < Path.size() - 1; Index++) {
                Length += Path.get(Index).distanceTo(Path.get(Index + 1));
            }
            this.Lifetime = (long) Math.ceil(Lead + Coins * LinkPause + Length / GunEffects.TracerSpeed + TrailLife + FlashLife) + 2L;
        }
    }
}
