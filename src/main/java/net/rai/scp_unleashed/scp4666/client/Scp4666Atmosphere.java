package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp4666Atmosphere {
    public static final float IndoorVolume = 0.3f;
    public static final float IndoorPitch = 0.86f;
    public static final int FadeTicks = 20;
    public static final int CheckEvery = 5;
    public static final int RoofReach = 14;
    public static final double WallReach = 10.0;
    public static final int Rays = 8;
    public static final int WallsNeeded = 6;
    public static final float GustChance = 0.006f;
    public static final Set<String> Blizzard = Set.of("scp4666_wind_loop", "scp4666_wind_gust", "scp4666_wind_howl");
    public static final float HushVolume = 0.15f;
    private static float Muffle;
    private static float Hush;
    private static boolean Indoors;
    private static int Ticks;
    private static Scp4666MuffledSound Loop;

    private Scp4666Atmosphere() {
    }

    public static float muffle() {
        float Value = Mth.clamp(Muffle, 0.0f, 1.0f);
        return Value * Value * (3.0f - 2.0f * Value);
    }

    public static float volume() {
        return Mth.lerp(muffle(), 1.0f, IndoorVolume) * Mth.lerp(Mth.clamp(Hush, 0.0f, 1.0f), 1.0f, HushVolume);
    }

    public static float pitch() {
        return Mth.lerp(muffle(), 1.0f, IndoorPitch);
    }

    public static boolean indoors() {
        return Indoors;
    }

    public static void tick(Minecraft Client) {
        if (!Scp4666ClientState.inside(Client) || Client.player == null) {
            stop();
            Muffle = 0.0f;
            Hush = 0.0f;
            Indoors = false;
            return;
        }
        Ticks++;
        if (Ticks % CheckEvery == 0) {
            Indoors = sheltered(Client.level, Client.player);
        }
        Muffle = Mth.clamp(Muffle + Mth.clamp((Indoors ? 1.0f : 0.0f) - Muffle, -1.0f / FadeTicks, 1.0f / FadeTicks), 0.0f, 1.0f);
        float Quiet = Scp4666ClientState.held() || Scp4666ClientState.bury(0.0f) > 0.0f ? 1.0f : 0.0f;
        Hush = Mth.clamp(Hush + Mth.clamp(Quiet - Hush, -1.0f / FadeTicks, 1.0f / (FadeTicks * 2)), 0.0f, 1.0f);
        if (Loop == null || !Client.getSoundManager().isActive(Loop)) {
            Loop = new Scp4666MuffledSound(new SimpleSoundInstance(Scp4666Registry.WindLoop.get().getLocation(), SoundSource.AMBIENT, 1.0f, 1.0f,
                    SoundInstance.createUnseededRandom(), true, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true), true);
            Client.getSoundManager().play(Loop);
        }
        if (!Client.isPaused() && Client.level.getRandom().nextFloat() < GustChance) {
            Client.getSoundManager().play(new Scp4666MuffledSound(new SimpleSoundInstance(Scp4666Registry.WindGust.get().getLocation(),
                    SoundSource.AMBIENT, 0.8f, 0.9f + Client.level.getRandom().nextFloat() * 0.2f, SoundInstance.createUnseededRandom(), false, 0,
                    SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true), false));
        }
    }

    public static void stop() {
        if (Loop != null) {
            Loop.fadeOut();
            Loop = null;
        }
    }

    public static boolean sheltered(ClientLevel World, Player User) {
        Vec3 Eye = User.getEyePosition();
        BlockPos Head = BlockPos.containing(Eye);
        if (World.canSeeSky(Head)) {
            return false;
        }
        boolean Roof = false;
        for (int Step = 1; Step <= RoofReach && !Roof; Step++) {
            BlockPos Above = Head.above(Step);
            Roof = !World.getBlockState(Above).getCollisionShape(World, Above).isEmpty();
        }
        if (!Roof) {
            return false;
        }
        if (Scp4666ClientState.inCabin(Eye)) {
            return true;
        }
        int Walls = 0;
        for (int Index = 0; Index < Rays; Index++) {
            double Angle = Index * Math.PI * 2.0 / Rays;
            Vec3 End = Eye.add(Math.cos(Angle) * WallReach, 0.0, Math.sin(Angle) * WallReach);
            if (World.clip(new ClipContext(Eye, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User)).getType() == HitResult.Type.BLOCK) {
                Walls++;
            }
        }
        return Walls >= WallsNeeded;
    }

    @SubscribeEvent
    public static void playing(PlaySoundEvent Event) {
        SoundInstance Sound = Event.getSound();
        if (Sound == null || Sound instanceof Scp4666MuffledSound || !Scp_unleashed.ModId.equals(Sound.getLocation().getNamespace())
                || !Blizzard.contains(Sound.getLocation().getPath()) || !Scp4666ClientState.inside(Minecraft.getInstance())) {
            return;
        }
        Event.setSound(new Scp4666MuffledSound(Sound, false));
    }
}
