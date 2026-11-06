package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Scp5001Hallucinations {
    public static final ResourceLocation LayerId = Scp5001EventRegistry.location("scp5001_hallucination");
    public static final ResourceLocation Pipeline = VeilPost.id("scp5001_ripple");
    public static final ResourceLocation Vignette = Scp5001EventRegistry.location("textures/gui/scp5001_event/vignette.png");
    public static final ResourceLocation Flesh = Scp5001EventRegistry.location("textures/gui/scp5001_event/flesh.png");
    public static final ResourceLocation Glitch = Scp5001EventRegistry.location("textures/gui/scp5001_event/glitch.png");
    public static final ResourceLocation Rings = Scp5001EventRegistry.location("textures/gui/scp5001_event/rings.png");
    public static final int OverlayWidth = 64;
    public static final int OverlayHeight = 36;
    public static final int FirstId = -50_010_000;
    public static final int LastId = -2_000_000_000;
    public static final float WaveSpeed = Scp5001VesselRenderer.WaveSpeed;
    public static final Set<ResourceLocation> HudLayers = Set.of(VanillaGuiLayers.HOTBAR, VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.AIR_LEVEL);
    private static final RandomSource Dice = RandomSource.create();
    private static final List<Scp5001Mimic> Phantoms = new ArrayList<>();
    private static float Ripple;
    private static float RippleO;
    private static int RippleIn = -1;
    private static float Flash;
    private static float Shake;
    private static int FleshTicks;
    private static int HudTicks;
    private static int StepIn = 200;
    private static int Steps;
    private static Vec3 StepFrom = Vec3.ZERO;
    private static int WhisperIn = 300;
    private static int HeartIn;
    private static int PhantomIn = 160;
    private static int NextId = FirstId;
    private static int Clock;
    @Nullable
    private static ClientLevel Current;

    private Scp5001Hallucinations() {
    }

    public static void pulse() {
        Minecraft Client = Minecraft.getInstance();
        Scp5001Vessel Vessel = Scp5001Beams.vessel();
        if (Client.player == null || Vessel == null) {
            RippleIn = 0;
            return;
        }
        double Dx = Client.player.getX() - Vessel.getX();
        double Dz = Client.player.getZ() - Vessel.getZ();
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        RippleIn = Mth.clamp((int) (Math.max(0.0, Distance - 2.5) / WaveSpeed), 0, Scp5001VesselRenderer.WaveLife);
    }

    public static void vesselTick(Scp5001Vessel Vessel) {
        if (Vessel.serial() != Vessel.SeenSerial) {
            boolean Fresh = Vessel.tickCount < 3;
            Vessel.SeenSerial = Vessel.serial();
            if (!Fresh) {
                Vessel.WaveStart = Vessel.tickCount;
            }
        }
    }

    public static void burst(double X, double Y, double Z, float Radius) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        double Distance = Client.player.position().distanceTo(new Vec3(X, Y, Z));
        float Near = (float) Mth.clamp(1.0 - Distance / 160.0, 0.25, 1.0);
        Flash = Math.max(Flash, Near);
        Shake = Math.max(Shake, Near);
        Ripple = 1.0f;
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        ClientLevel World = Client.level;
        if (User == null || World == null) {
            reset();
            return;
        }
        if (World != Current) {
            reset();
            Current = World;
        }
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        RippleO = Ripple;
        Ripple *= 0.94f;
        Flash *= 0.95f;
        Shake *= 0.9f;
        if (RippleIn >= 0 && RippleIn-- == 0) {
            Ripple = 1.0f;
            sound(World, User.position(), Scp5001EventRegistry.Wave, 0.35f, 1.3f);
        }
        FleshTicks = Math.max(0, FleshTicks - 1);
        HudTicks = Math.max(0, HudTicks - 1);
        float Level = Scp5001ClientState.exposure();
        updatePhantoms(World, User);
        if (Level < 0.1f) {
            return;
        }
        footsteps(World, User, Level);
        if (Level >= 0.35f && --WhisperIn <= 0) {
            WhisperIn = 160 + Dice.nextInt(Math.max(1, (int) (420 * (1.2f - Level))));
            float Side = Dice.nextBoolean() ? 1.0f : -1.0f;
            float Yaw = (User.getYRot() + 90.0f * Side) * Mth.DEG_TO_RAD;
            Vec3 Where = User.getEyePosition().add(-Mth.sin(Yaw) * 2.5, 0.0, Mth.cos(Yaw) * 2.5);
            sound(World, Where, Scp5001EventRegistry.Whisper, 0.5f + Level * 0.4f, 0.85f + Dice.nextFloat() * 0.2f);
        }
        if (Level >= 0.8f && --HeartIn <= 0) {
            HeartIn = Level >= 0.95f ? 14 : 20;
            sound(World, User.position(), Scp5001EventRegistry.Heartbeat, 0.6f + (Level - 0.8f) * 2.0f, 1.0f);
        }
        if (Level >= 0.45f && FleshTicks == 0 && Dice.nextFloat() < Level * 0.005f) {
            FleshTicks = 3 + Dice.nextInt(4);
            if (Dice.nextInt(3) == 0) {
                sound(World, User.position(), Scp5001EventRegistry.Sting, 0.35f, 0.9f + Dice.nextFloat() * 0.3f);
            }
        }
        if (Level >= 0.3f && HudTicks == 0 && Dice.nextFloat() < Level * 0.006f) {
            HudTicks = 4 + Dice.nextInt(8);
        }
        if (Level >= 0.25f && --PhantomIn <= 0) {
            PhantomIn = 140 + Dice.nextInt(Math.max(1, (int) (320 * (1.15f - Level))));
            phantom(Client, World, User);
        }
    }

    private static void footsteps(ClientLevel World, LocalPlayer User, float Level) {
        if (Steps > 0) {
            if (Clock % 9 == 0) {
                Vec3 Toward = User.position().subtract(StepFrom);
                StepFrom = StepFrom.add(Toward.normalize().scale(0.55));
                sound(World, StepFrom, Scp5001EventRegistry.Footstep, 0.55f, 0.8f + Dice.nextFloat() * 0.15f);
                Steps--;
            }
            return;
        }
        if (Level >= 0.15f && --StepIn <= 0) {
            StepIn = 140 + Dice.nextInt(Math.max(1, (int) (300 * (1.1f - Level))));
            Steps = 3 + Dice.nextInt(3);
            float Yaw = (User.getYRot() + 180.0f + (Dice.nextFloat() - 0.5f) * 60.0f) * Mth.DEG_TO_RAD;
            double Reach = 4.0 + Dice.nextDouble() * 3.0;
            StepFrom = User.position().add(-Mth.sin(Yaw) * Reach, 0.0, Mth.cos(Yaw) * Reach);
        }
    }

    private static void phantom(Minecraft Client, ClientLevel World, LocalPlayer User) {
        if (Phantoms.size() >= 2) {
            return;
        }
        for (int Attempt = 0; Attempt < 8; Attempt++) {
            float Side = Dice.nextBoolean() ? 1.0f : -1.0f;
            float Yaw = (User.getYRot() + Side * (62.0f + Dice.nextFloat() * 30.0f)) * Mth.DEG_TO_RAD;
            double Reach = 8.0 + Dice.nextDouble() * 8.0;
            BlockPos Feet = BlockPos.containing(User.getX() - Mth.sin(Yaw) * Reach, User.getY(), User.getZ() + Mth.cos(Yaw) * Reach);
            for (int Dy = 0; Dy <= 3; Dy++) {
                BlockPos Probe = Feet.above(Dy - 1);
                if (World.getBlockState(Probe).isAir() && World.getBlockState(Probe.above()).isAir() && World.getBlockState(Probe.below()).isFaceSturdy(World, Probe.below(), net.minecraft.core.Direction.UP)) {
                    Scp5001Mimic Made = Scp5001EventRegistry.MimicType.get().create(World);
                    if (Made == null) {
                        return;
                    }
                    Made.Phantom = true;
                    Made.PhantomLife = 30 + Dice.nextInt(60);
                    Made.copyProfile(profileFor(Client, User));
                    Made.setId(nextId());
                    Vec3 At = Vec3.atBottomCenterOf(Probe);
                    float Facing = (float) (Mth.atan2(User.getZ() - At.z, User.getX() - At.x) * Mth.RAD_TO_DEG) - 90.0f;
                    Made.moveTo(At.x, At.y, At.z, Facing, 0.0f);
                    Made.setYHeadRot(Facing);
                    Made.setYBodyRot(Facing);
                    World.addEntity(Made);
                    Phantoms.add(Made);
                    return;
                }
            }
        }
    }

    private static ResolvableProfile profileFor(Minecraft Client, LocalPlayer User) {
        ClientPacketListener Connection = Client.getConnection();
        if (Connection != null && Dice.nextInt(3) != 0) {
            List<PlayerInfo> Online = new ArrayList<>(Connection.getOnlinePlayers());
            if (!Online.isEmpty()) {
                return new ResolvableProfile(Online.get(Dice.nextInt(Online.size())).getProfile());
            }
        }
        return new ResolvableProfile(User.getGameProfile());
    }

    private static void updatePhantoms(ClientLevel World, LocalPlayer User) {
        Phantoms.removeIf(Each -> {
            if (Each.isRemoved()) {
                return true;
            }
            Vec3 Look = User.getViewVector(1.0f);
            Vec3 Toward = Each.position().add(0.0, 1.2, 0.0).subtract(User.getEyePosition()).normalize();
            if (Look.dot(Toward) > 0.93) {
                for (int Index = 0; Index < 12; Index++) {
                    World.addParticle(ParticleTypes.SMOKE, Each.getX() + (Dice.nextDouble() - 0.5) * 0.6, Each.getY() + Dice.nextDouble() * 1.8,
                            Each.getZ() + (Dice.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
                }
                Each.discard();
                return true;
            }
            return false;
        });
    }

    private static int nextId() {
        int Id = NextId;
        NextId = NextId <= LastId ? FirstId : NextId - 1;
        return Id;
    }

    private static void sound(ClientLevel World, Vec3 Where, DeferredHolder<SoundEvent, SoundEvent> Layer, float Volume, float Pitch) {
        World.playLocalSound(Where.x, Where.y, Where.z, Layer.get(), SoundSource.AMBIENT, Volume, Pitch, false);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui && Flash < 0.01f) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Level = Scp5001ClientState.exposure();
        float Wave = Mth.lerp(Partial, RippleO, Ripple);
        if (Level < 0.02f && Wave < 0.01f && Flash < 0.01f) {
            return;
        }
        float Time = Clock + Partial;
        float Distort = Math.max(Wave, Level > 0.5f ? (Level - 0.5f) * 0.5f : 0.0f) * Client.options.screenEffectScale().get().floatValue();
        boolean Drawn = false;
        if (Distort > 0.01f && VeilPost.ready(Pipeline)) {
            Graphics.flush();
            Drawn = VeilPost.run(Pipeline, Values -> Values.set("Strength", Distort).set("Progress", 1.0f - Wave).set("Seconds", Time / 20.0f).set("Exposure", Level));
        }
        if (!Drawn && Wave > 0.05f) {
            veil(Graphics, Rings, Wave * 0.45f, 0x9C8CF0, OverlayWidth, OverlayHeight);
        }
        if (Level >= 0.1f) {
            float Stepped = Math.round(Level * 4.0f) / 4.0f;
            float Breath = 0.85f + 0.15f * Mth.sin(Time * (0.05f + Level * 0.1f));
            veil(Graphics, Vignette, Math.min(0.9f, Stepped * 0.8f * Breath), 0xFFFFFF, OverlayWidth, OverlayHeight);
        }
        if (FleshTicks > 0) {
            veil(Graphics, Flesh, 0.45f + 0.25f * Dice.nextFloat(), 0xFFFFFF, OverlayWidth, OverlayHeight);
        }
        if (HudTicks > 0) {
            tears(Graphics, Time);
        }
        if (Flash > 0.01f) {
            int Alpha = Mth.clamp(Math.round(Flash * 235.0f), 0, 255);
            int Colour = Flash > 0.6f ? 0xF4F0FF : 0xC86058;
            Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), Alpha << 24 | Colour);
        }
    }

    private static void veil(GuiGraphics Graphics, ResourceLocation Texture, float Alpha, int Colour, int Width, int Height) {
        if (Alpha <= 0.0f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f, Mth.clamp(Alpha, 0.0f, 1.0f));
        Graphics.blit(Texture, 0, 0, Graphics.guiWidth(), Graphics.guiHeight(), 0.0f, 0.0f, Width, Height, Width, Height);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void tears(GuiGraphics Graphics, float Time) {
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        RandomSource Bands = RandomSource.create((long) (Time / 2.0f));
        RenderSystem.enableBlend();
        for (int Index = 0; Index < 4; Index++) {
            int Y = Height - 60 + Bands.nextInt(56) - (Index == 0 ? Bands.nextInt(Height / 2) : 0);
            int Tall = 1 + Bands.nextInt(4);
            int Shift = Bands.nextInt(24) - 12;
            Graphics.blit(Glitch, Shift, Y, Width, Tall, 0.0f, Bands.nextInt(14), 64, Tall, 64, 16);
        }
        RenderSystem.disableBlend();
    }

    public static void hud(RenderGuiLayerEvent.Pre Event) {
        if (HudTicks > 0 && HudTicks % 3 != 0 && HudLayers.contains(Event.getName())) {
            Event.setCanceled(true);
        }
    }

    public static void camera(ViewportEvent.ComputeCameraAngles Event) {
        float Level = Scp5001ClientState.exposure();
        float Time = Clock + (float) Event.getPartialTick();
        float Roll = Mth.sin(Time * 0.07f) * Level * Level * 3.0f + Mth.sin(Time * 0.9f) * Ripple * 1.5f;
        Event.setRoll(Event.getRoll() + Roll);
        if (Shake > 0.01f) {
            Event.setYaw(Event.getYaw() + (Dice.nextFloat() - 0.5f) * Shake * 3.0f);
            Event.setPitch(Event.getPitch() + (Dice.nextFloat() - 0.5f) * Shake * 3.0f);
        }
    }

    public static void reset() {
        for (Scp5001Mimic Each : Phantoms) {
            if (!Each.isRemoved()) {
                Each.discard();
            }
        }
        Phantoms.clear();
        Ripple = 0.0f;
        RippleO = 0.0f;
        RippleIn = -1;
        Flash = 0.0f;
        Shake = 0.0f;
        FleshTicks = 0;
        HudTicks = 0;
        Steps = 0;
        Current = null;
    }
}
