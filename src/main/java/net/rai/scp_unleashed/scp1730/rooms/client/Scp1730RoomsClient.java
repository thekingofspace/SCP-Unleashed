package net.rai.scp_unleashed.scp1730.rooms.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.util.TriState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomKind;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomLooks;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomsRegistry;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730SpecialRoom;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730SpecialRooms;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp1730RoomsClient {
    public static final ResourceLocation Grade = VeilPost.id("scp1730_rooms_grade");
    public static final ResourceLocation TintLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_rooms_tint");
    public static final float FadeStep = 0.05f;
    public static final float SilentStep = 0.1f;
    public static final float[] ServerFog = {0.07f, 0.15f, 0.27f};
    public static final float[] HallFog = {0.09f, 0.09f, 0.1f};
    public static final float MinGain = 0.0001f;
    public static final int MistReach = 10;
    public static final int MistCapAll = 120;
    public static final int MistCapSome = 50;
    public static final int DustCap = 60;
    public static final int ShaftCap = 140;
    public static final double ShaftRadius = 2.2;
    public static final int PaperCap = 22;
    public static final int LedCap = 8;
    private static final float[] Share = new float[Scp1730RoomKind.values().length];
    private static final float[] Old = new float[Scp1730RoomKind.values().length];
    @Nullable
    private static Scp1730SpecialRoom Current;
    private static float Applied = Float.NaN;
    private static boolean Drawn;
    @Nullable
    private static ClientLevel LastLevel;

    private Scp1730RoomsClient() {
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp1730RoomsRegistry.MistParticle.get(), Sprites -> new Scp1730RoomParticles.Provider(Sprites, Scp1730RoomParticles.Mist));
        Event.registerSpriteSet(Scp1730RoomsRegistry.DustParticle.get(), Sprites -> new Scp1730RoomParticles.Provider(Sprites, Scp1730RoomParticles.Dust));
        Event.registerSpriteSet(Scp1730RoomsRegistry.ConfettiParticle.get(), Sprites -> new Scp1730RoomParticles.Provider(Sprites, Scp1730RoomParticles.Confetti));
        Event.registerSpriteSet(Scp1730RoomsRegistry.PaperParticle.get(), Sprites -> new Scp1730RoomParticles.Provider(Sprites, Scp1730RoomParticles.Paper));
        Event.registerSpriteSet(Scp1730RoomsRegistry.LedParticle.get(), Sprites -> new Scp1730RoomParticles.Provider(Sprites, Scp1730RoomParticles.Led));
        Event.registerSpriteSet(Scp1730RoomsRegistry.StreamerParticle.get(), Sprites -> new Scp1730RoomParticles.Provider(Sprites, Scp1730RoomParticles.Streamer));
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, TintLayer, Scp1730RoomsClient::overlay);
    }

    public static float share(Scp1730RoomKind Kind, float Partial) {
        return Mth.lerp(Partial, Old[Kind.ordinal()], Share[Kind.ordinal()]);
    }

    public static float share(Scp1730RoomKind Kind) {
        return Share[Kind.ordinal()];
    }

    @Nullable
    public static Scp1730SpecialRoom current() {
        return Current;
    }

    public static Vec3 listener(Minecraft Client) {
        Camera View = Client.gameRenderer.getMainCamera();
        if (View.isInitialized()) {
            return View.getPosition();
        }
        return Client.player != null ? Client.player.getEyePosition() : Vec3.ZERO;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World != LastLevel) {
            LastLevel = World;
            Arrays.fill(Scp1730RoomParticles.Alive, 0);
            Scp1730EchoClient.reset();
        }
        System.arraycopy(Share, 0, Old, 0, Share.length);
        if (World == null || Client.player == null) {
            Arrays.fill(Share, 0.0f);
            Current = null;
            gain(Client);
            return;
        }
        Vec3 Eye = listener(Client);
        Current = Scp1730SpecialRooms.at(World, Eye);
        for (Scp1730RoomKind Kind : Scp1730RoomKind.values()) {
            float Want = Current != null && Current.Kind() == Kind ? 1.0f : 0.0f;
            float Step = Kind == Scp1730RoomKind.Silent ? SilentStep : FadeStep;
            int Index = Kind.ordinal();
            Share[Index] = Want > Share[Index] ? Math.min(Want, Share[Index] + Step) : Math.max(Want, Share[Index] - Step);
        }
        gain(Client);
        Scp1730EchoClient.tick(Client, Current != null && Current.Kind() == Scp1730RoomKind.Echo);
        if (Client.isPaused()) {
            return;
        }
        ParticleStatus Status = Client.options.particles().get();
        if (Status == ParticleStatus.MINIMAL || Current == null) {
            return;
        }
        RandomSource Random = World.random;
        switch (Current.Kind()) {
            case Server -> {
                int Cap = Status == ParticleStatus.ALL ? MistCapAll : MistCapSome;
                int Tries = Status == ParticleStatus.ALL ? 4 : 2;
                for (int Try = 0; Try < Tries && Scp1730RoomParticles.alive(Scp1730RoomParticles.Mist) < Cap; Try++) {
                    floor(World, Current, Eye, Random, Scp1730RoomsRegistry.MistParticle.get(), 0.05, 0.55, 0.008);
                }
                if (World.getGameTime() % 5 == 0) {
                    leds(World, Eye, Random);
                }
            }
            case Silent -> {
                for (int Try = 0; Try < 3; Try++) {
                    shaft(World, Current, Random);
                }
            }
            case VaultHall -> spread(World, Current, Eye, Random, Scp1730RoomsRegistry.DustParticle.get(), DustCap, Scp1730RoomParticles.Dust, 0.004, 9);
            case Slow -> spread(World, Current, Eye, Random, Scp1730RoomsRegistry.DustParticle.get(), DustCap, Scp1730RoomParticles.Dust, 0.002, 7);
            case Drift -> {
                if (Random.nextInt(4) == 0 && Scp1730RoomParticles.alive(Scp1730RoomParticles.Paper) < PaperCap) {
                    floor(World, Current, Eye, Random, Scp1730RoomsRegistry.PaperParticle.get(), 0.1, 0.4, 0.004);
                }
            }
            default -> {
            }
        }
    }

    static void floor(ClientLevel World, Scp1730SpecialRoom Room, Vec3 Eye, RandomSource Random, SimpleParticleType Type, double Low, double Span, double Drift) {
        double X = Eye.x + (Random.nextDouble() * 2.0 - 1.0) * MistReach;
        double Z = Eye.z + (Random.nextDouble() * 2.0 - 1.0) * MistReach;
        if (X < Room.MinX() + 1 || X >= Room.MaxX() || Z < Room.MinZ() + 1 || Z >= Room.MaxZ()) {
            return;
        }
        double Y = Room.Floor() + 1 + Low + Random.nextDouble() * Span;
        BlockState Here = World.getBlockState(BlockPos.containing(X, Room.Floor() + 1, Z));
        if (!Here.getCollisionShape(World, BlockPos.containing(X, Room.Floor() + 1, Z)).isEmpty()) {
            return;
        }
        double Up = Type == Scp1730RoomsRegistry.PaperParticle.get() ? 0.008 + Random.nextDouble() * 0.006 : 0.0;
        World.addParticle(Type, X, Y, Z, (Random.nextDouble() - 0.5) * 2.0 * Drift, Up, (Random.nextDouble() - 0.5) * 2.0 * Drift);
    }

    static void spread(ClientLevel World, Scp1730SpecialRoom Room, Vec3 Eye, RandomSource Random, SimpleParticleType Type, int Cap, int Style, double Drift, int Reach) {
        if (Scp1730RoomParticles.alive(Style) >= Cap) {
            return;
        }
        double X = Eye.x + (Random.nextDouble() * 2.0 - 1.0) * Reach;
        double Y = Eye.y + (Random.nextDouble() * 2.0 - 1.4) * 2.0;
        double Z = Eye.z + (Random.nextDouble() * 2.0 - 1.0) * Reach;
        if (!Room.contains(X, Y, Z) || Y < Room.Floor() + 1 || !World.getBlockState(BlockPos.containing(X, Y, Z)).isAir()) {
            return;
        }
        World.addParticle(Type, X, Y, Z, (Random.nextDouble() - 0.5) * Drift, (Random.nextDouble() - 0.6) * Drift, (Random.nextDouble() - 0.5) * Drift);
    }

    static void shaft(ClientLevel World, Scp1730SpecialRoom Room, RandomSource Random) {
        if (Scp1730RoomParticles.alive(Scp1730RoomParticles.Dust) >= ShaftCap) {
            return;
        }
        Vec3 Centre = Room.centre();
        boolean Column = Random.nextInt(4) > 0;
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Reach = Column ? Math.sqrt(Random.nextDouble()) * ShaftRadius : 1.0 + Random.nextDouble() * Math.max(1.0, (Room.MaxX() - Room.MinX()) / 2.0 - 1.5);
        double X = Centre.x + Math.cos(Angle) * Reach;
        double Z = Centre.z + Math.sin(Angle) * Reach;
        double Y = Room.Floor() + 1.2 + Random.nextDouble() * (Room.Height() - 0.6);
        if (!Room.contains(X, Y, Z) || !World.getBlockState(BlockPos.containing(X, Y, Z)).isAir()) {
            return;
        }
        World.addParticle(Scp1730RoomsRegistry.DustParticle.get(), X, Y, Z, 0.0, 0.0, 0.0);
    }

    static void leds(ClientLevel World, Vec3 Eye, RandomSource Random) {
        for (int Try = 0; Try < 6 && Scp1730RoomParticles.alive(Scp1730RoomParticles.Led) < LedCap; Try++) {
            BlockPos Pos = BlockPos.containing(Eye.x + Random.nextInt(17) - 8, Eye.y - 1 + Random.nextInt(2), Eye.z + Random.nextInt(17) - 8);
            BlockState State = World.getBlockState(Pos);
            ResourceLocation Id = State.getBlock().builtInRegistryHolder().key().location();
            if (!Id.getPath().equals("server_rack") || !State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                continue;
            }
            Direction Front = State.getValue(BlockStateProperties.HORIZONTAL_FACING);
            double Side = (Random.nextDouble() - 0.5) * 0.7;
            double X = Pos.getX() + 0.5 + Front.getStepX() * 0.52 + (Front.getAxis() == Direction.Axis.Z ? Side : 0.0);
            double Z = Pos.getZ() + 0.5 + Front.getStepZ() * 0.52 + (Front.getAxis() == Direction.Axis.X ? Side : 0.0);
            World.addParticle(Scp1730RoomsRegistry.LedParticle.get(), X, Pos.getY() + 0.15 + Random.nextDouble() * 0.75, Z, 0.0, 0.0, 0.0);
        }
    }

    static void gain(Minecraft Client) {
        float Master = Client.options.getSoundSourceVolume(SoundSource.MASTER);
        float Silence = share(Scp1730RoomKind.Silent);
        if (Master <= 0.0f) {
            Applied = Float.NaN;
            return;
        }
        if (Silence <= 0.0f || Client.isPaused()) {
            if (!Float.isNaN(Applied)) {
                Client.getSoundManager().updateSourceVolume(SoundSource.MASTER, Master);
                Applied = Float.NaN;
            }
            return;
        }
        float Want = Math.max(MinGain, Master * (1.0f - Silence));
        if (Float.isNaN(Applied) || Math.abs(Applied - Want) > 0.0005f) {
            Client.getSoundManager().updateSourceVolume(SoundSource.MASTER, Want);
            Applied = Want;
        }
    }

    @SubscribeEvent
    public static void playing(PlaySoundEvent Event) {
        SoundInstance Sound = Event.getSound();
        Minecraft Client = Minecraft.getInstance();
        if (Sound == null || Client.level == null) {
            return;
        }
        boolean Placed = !Sound.isRelative() && Sound.getAttenuation() != SoundInstance.Attenuation.NONE;
        if (Placed && Scp1730SpecialRooms.silentAt(Client.level, Sound.getX(), Sound.getY(), Sound.getZ())) {
            Event.setSound(null);
            return;
        }
        boolean Plain = !(Sound instanceof TickableSoundInstance) && !Sound.isLooping() && Sound.getSource() != SoundSource.MASTER && Sound.getSource() != SoundSource.MUSIC
                && Sound.getSource() != SoundSource.RECORDS;
        if (Plain && share(Scp1730RoomKind.Silent) >= 0.5f) {
            Event.setSound(null);
            return;
        }
        if (Plain && Placed && !(Sound instanceof Scp1730SlowedSound)) {
            Scp1730SpecialRoom Room = Scp1730SpecialRooms.at(Client.level, new Vec3(Sound.getX(), Sound.getY(), Sound.getZ()), Scp1730RoomKind.Slow);
            if (Room != null) {
                Event.setSound(new Scp1730SlowedSound(Sound));
            }
        }
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog Event) {
        if (Event.getType() != FogType.NONE) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        float Server = share(Scp1730RoomKind.Server, Partial);
        float Hall = share(Scp1730RoomKind.VaultHall, Partial);
        float Dark = share(Scp1730RoomKind.Lightless, Partial);
        if (Server < 0.005f && Hall < 0.005f && Dark < 0.005f) {
            return;
        }
        float[] Planes = Scp1730RoomLooks.fog(Event.getNearPlaneDistance(), Event.getFarPlaneDistance(), Server, Hall, Dark);
        Event.setFarPlaneDistance(Planes[1]);
        Event.setNearPlaneDistance(Planes[0]);
        Event.setFogShape(FogShape.SPHERE);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        if (Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        float Server = share(Scp1730RoomKind.Server, Partial);
        float Hall = share(Scp1730RoomKind.VaultHall, Partial);
        float Dark = share(Scp1730RoomKind.Lightless, Partial);
        float Red = Event.getRed();
        float Green = Event.getGreen();
        float Blue = Event.getBlue();
        Red = Mth.lerp(Server, Red, ServerFog[0]);
        Green = Mth.lerp(Server, Green, ServerFog[1]);
        Blue = Mth.lerp(Server, Blue, ServerFog[2]);
        Red = Mth.lerp(Hall, Red, HallFog[0]);
        Green = Mth.lerp(Hall, Green, HallFog[1]);
        Blue = Mth.lerp(Hall, Blue, HallFog[2]);
        Red = Mth.lerp(Dark, Red, 0.0f);
        Green = Mth.lerp(Dark, Green, 0.0f);
        Blue = Mth.lerp(Dark, Blue, 0.0f);
        Event.setRed(Red);
        Event.setGreen(Green);
        Event.setBlue(Blue);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            Scp1730EchoClient.render(Event.getPoseStack(), Event.getCamera().getPosition(), Event.getPartialTick().getGameTimeDeltaPartialTick(false));
            return;
        }
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float Comfort = (float) Mth.clamp(ScpClientConfig.realityDistortion(), 0.0, 1.0);
        float Cold = share(Scp1730RoomKind.Server, Partial);
        float Drain = share(Scp1730RoomKind.Silent, Partial) * 0.35f * Comfort + share(Scp1730RoomKind.Lightless, Partial) * 0.25f;
        float Warm = share(Scp1730RoomKind.Slow, Partial) * 0.55f * Comfort;
        float Dim = share(Scp1730RoomKind.Lightless, Partial) * 0.45f + share(Scp1730RoomKind.VaultHall, Partial) * 0.12f;
        Drawn = false;
        if (Cold + Drain + Warm + Dim < 0.003f || !VeilPost.available()) {
            return;
        }
        Drawn = VeilPost.run(Grade, Values -> Values.set("Cold", Cold).set("Drain", Drain).set("Warm", Warm).set("Dim", Dim));
    }

    public static void overlay(GuiGraphics Graphics, DeltaTracker Delta) {
        if (Drawn) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Cold = share(Scp1730RoomKind.Server, Partial);
        float Warm = share(Scp1730RoomKind.Slow, Partial) * (float) Mth.clamp(ScpClientConfig.realityDistortion(), 0.0, 1.0);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        if (Cold > 0.01f) {
            Graphics.fill(0, 0, Width, Height, (int) (Cold * 26.0f) << 24 | 0x0B2A5C);
        }
        if (Warm > 0.01f) {
            Graphics.fill(0, 0, Width, Height, (int) (Warm * 22.0f) << 24 | 0x5A3A12);
        }
    }

    @SubscribeEvent
    public static void nameTag(RenderNameTagEvent Event) {
        if (Scp1730EchoClient.isGhost(Event.getEntity())) {
            Event.setCanRender(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Arrays.fill(Share, 0.0f);
        Arrays.fill(Old, 0.0f);
        Current = null;
        gain(Minecraft.getInstance());
        Scp1730SpecialRooms.resetClient();
        Scp1730EchoClient.reset();
        Scp1730TelevisionClient.reset();
    }
}
