package net.rai.scp_unleashed.chunkloader.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderBlockEntity;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderRegistry;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ChunkLoaderClient {
    public static final int HumEvery = 100;
    public static final int PulseMin = 120;
    public static final int PulseSpread = 60;
    public static final float SparkRate = 0.06f;
    public static final double OutlineReach = 320.0;
    public static final int PostBelow = 8;
    public static final int PostAbove = 24;
    public static final int RingStep = 8;
    public static final float EdgeAlpha = 0.85f;
    public static final float GridAlpha = 0.3f;
    public static final double Lift = 0.02;

    private static final Map<ChunkLoaderBlockEntity, Voice> Known = new WeakHashMap<>();
    private static boolean OutlineShown = true;

    private ChunkLoaderClient() {
    }

    private static final class Voice {
        private long NextHum;
        private long NextPulse;
    }

    public static boolean isOutlineShown() {
        return OutlineShown;
    }

    public static void toggleOutline() {
        OutlineShown = !OutlineShown;
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        ChunkLoaderBlockEntity.setClientEffects(ChunkLoaderClient::loaderTick);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(ChunkLoaderRegistry.ChunkLoaderMenuType.get(), ChunkLoaderScreen::new);
    }

    private static void loaderTick(ChunkLoaderBlockEntity Loader) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null || Loader.getLevel() != World) {
            return;
        }
        Voice Tracker = Known.computeIfAbsent(Loader, Key -> new Voice());
        if (ChunkLoaderHum.strength(Loader) <= 0.0f) {
            Tracker.NextHum = 0L;
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        RandomSource Random = World.getRandom();
        long Now = World.getGameTime();
        if (Now >= Tracker.NextHum) {
            Tracker.NextHum = Now + HumEvery;
            Sounds.play(new ChunkLoaderHum(Loader, ChunkLoaderRegistry.Hum.get(), 0.55f, 1.0f));
        }
        if (Now >= Tracker.NextPulse) {
            Tracker.NextPulse = Now + PulseMin + Random.nextInt(PulseSpread);
            Sounds.play(new ChunkLoaderHum(Loader, ChunkLoaderRegistry.Pulse.get(), 0.4f, 0.95f + Random.nextFloat() * 0.1f));
        }
        if (Random.nextFloat() < SparkRate) {
            BlockPos Pos = Loader.getBlockPos();
            double X = Pos.getX() + 0.3 + Random.nextDouble() * 0.4;
            double Z = Pos.getZ() + 0.3 + Random.nextDouble() * 0.4;
            World.addParticle(ParticleTypes.END_ROD, X, Pos.getY() + 1.05, Z, 0.0, 0.01 + Random.nextDouble() * 0.015, 0.0);
        }
    }

    private static boolean holdsLoader(LocalPlayer User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            if (User.getItemInHand(Hand).is(ChunkLoaderRegistry.ChunkLoaderItem.get())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !OutlineShown) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        ClientLevel World = Game.level;
        if (User == null || World == null) {
            return;
        }
        BlockPos Focus = Game.screen instanceof ChunkLoaderScreen Open ? Open.getMenu().getPos() : null;
        boolean Holding = holdsLoader(User);
        if (Focus == null && !Holding) {
            return;
        }
        Vec3 Eye = Event.getCamera().getPosition();
        List<ChunkLoaderBlockEntity> Shown = new ArrayList<>();
        for (ChunkLoaderBlockEntity Loader : Known.keySet()) {
            if (Loader.isRemoved() || Loader.getLevel() != World) {
                continue;
            }
            BlockPos Pos = Loader.getBlockPos();
            boolean Wanted = Holding || Pos.equals(Focus);
            double Reach = OutlineReach + Loader.getRadius() * 16.0;
            if (Wanted && Math.abs(Pos.getX() + 0.5 - Eye.x) < Reach && Math.abs(Pos.getZ() + 0.5 - Eye.z) < Reach) {
                Shown.add(Loader);
            }
        }
        BlockPos Preview = null;
        if (Holding && Game.hitResult instanceof BlockHitResult Hit && Hit.getType() == HitResult.Type.BLOCK) {
            Preview = Hit.getBlockPos().relative(Hit.getDirection());
        }
        if (Shown.isEmpty() && Preview == null) {
            return;
        }
        PoseStack Pose = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        VertexConsumer Lines = Buffers.getBuffer(RenderType.lines());
        PoseStack.Pose Last = Pose.last();
        for (ChunkLoaderBlockEntity Loader : Shown) {
            ChunkLoaderStatus Status = Loader.getStatus();
            drawArea(Last, Lines, World, Eye, Loader.getBlockPos(), Loader.getRadius(), Status.getRed(), Status.getGreen(), Status.getBlue(), true);
        }
        if (Preview != null) {
            drawArea(Last, Lines, World, Eye, Preview, 0, 1.0f, 1.0f, 1.0f, false);
        }
        Buffers.endBatch(RenderType.lines());
    }

    private static void drawArea(PoseStack.Pose Last, VertexConsumer Lines, ClientLevel World, Vec3 Eye, BlockPos Pos, int Radius, float Red, float Green, float Blue, boolean Grid) {
        int ChunkX = SectionPos.blockToSectionCoord(Pos.getX());
        int ChunkZ = SectionPos.blockToSectionCoord(Pos.getZ());
        int Side = Radius * 2 + 1;
        double MinX = SectionPos.sectionToBlockCoord(ChunkX - Radius);
        double MinZ = SectionPos.sectionToBlockCoord(ChunkZ - Radius);
        double MaxX = MinX + Side * 16.0;
        double MaxZ = MinZ + Side * 16.0;
        double Floor = Pos.getY() + Lift;
        double Bottom = Math.max(World.getMinBuildHeight(), Pos.getY() - PostBelow);
        double Top = Math.min(World.getMaxBuildHeight(), Pos.getY() + PostAbove);
        for (int Step = 0; Step <= Side; Step++) {
            double X = MinX + Step * 16.0;
            double Z = MinZ + Step * 16.0;
            line(Last, Lines, Eye, X, Bottom, MinZ, X, Top, MinZ, Red, Green, Blue, EdgeAlpha);
            line(Last, Lines, Eye, X, Bottom, MaxZ, X, Top, MaxZ, Red, Green, Blue, EdgeAlpha);
            if (Step > 0 && Step < Side) {
                line(Last, Lines, Eye, MinX, Bottom, Z, MinX, Top, Z, Red, Green, Blue, EdgeAlpha);
                line(Last, Lines, Eye, MaxX, Bottom, Z, MaxX, Top, Z, Red, Green, Blue, EdgeAlpha);
            }
        }
        for (double Y = Bottom; Y <= Top; Y += RingStep) {
            line(Last, Lines, Eye, MinX, Y, MinZ, MaxX, Y, MinZ, Red, Green, Blue, EdgeAlpha);
            line(Last, Lines, Eye, MinX, Y, MaxZ, MaxX, Y, MaxZ, Red, Green, Blue, EdgeAlpha);
            line(Last, Lines, Eye, MinX, Y, MinZ, MinX, Y, MaxZ, Red, Green, Blue, EdgeAlpha);
            line(Last, Lines, Eye, MaxX, Y, MinZ, MaxX, Y, MaxZ, Red, Green, Blue, EdgeAlpha);
        }
        if (Grid) {
            for (int Step = 1; Step < Side; Step++) {
                double X = MinX + Step * 16.0;
                double Z = MinZ + Step * 16.0;
                line(Last, Lines, Eye, X, Floor, MinZ, X, Floor, MaxZ, Red, Green, Blue, GridAlpha);
                line(Last, Lines, Eye, MinX, Floor, Z, MaxX, Floor, Z, Red, Green, Blue, GridAlpha);
            }
        }
    }

    private static void line(PoseStack.Pose Last, VertexConsumer Lines, Vec3 Eye, double FromX, double FromY, double FromZ, double ToX, double ToY, double ToZ,
                             float Red, float Green, float Blue, float Alpha) {
        float Dx = (float) (ToX - FromX);
        float Dy = (float) (ToY - FromY);
        float Dz = (float) (ToZ - FromZ);
        float Length = (float) Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
        if (Length < 1.0E-4f) {
            return;
        }
        Dx /= Length;
        Dy /= Length;
        Dz /= Length;
        Lines.addVertex(Last, (float) (FromX - Eye.x), (float) (FromY - Eye.y), (float) (FromZ - Eye.z)).setColor(Red, Green, Blue, Alpha).setNormal(Last, Dx, Dy, Dz);
        Lines.addVertex(Last, (float) (ToX - Eye.x), (float) (ToY - Eye.y), (float) (ToZ - Eye.z)).setColor(Red, Green, Blue, Alpha).setNormal(Last, Dx, Dy, Dz);
    }
}
