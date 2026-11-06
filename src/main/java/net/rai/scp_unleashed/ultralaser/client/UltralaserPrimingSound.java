package net.rai.scp_unleashed.ultralaser.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.ultralaser.UltralaserPriming;
import net.rai.scp_unleashed.ultralaser.UltralaserRegistry;
import net.rai.scp_unleashed.ultralaser.UltralaserScore;
import net.rai.scp_unleashed.ultralaser.UltralaserSounds;
import net.rai.scp_unleashed.ultralaser.network.UltralaserPrimingPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class UltralaserPrimingSound {
    public static final int StartGrace = 20;
    public static final double NanosPerTick = 5.0E7;
    public static final float ClockTail = 40.0F;
    private static long LocalStart;
    private static boolean LocalActive;
    private static int LocalShown = -1;
    private static final Map<Integer, Clip> Playing = new HashMap<>();
    private static long Ticks;

    private UltralaserPrimingSound() {
    }

    private record Clip(SoundInstance Sound, long Started) {
    }

    public static void handle(UltralaserPrimingPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        boolean Local = Game.player != null && Game.player.getId() == Payload.ShooterId();
        if (Local) {
            LocalActive = false;
        }
        Clip Previous = Playing.remove(Payload.ShooterId());
        if (Previous != null) {
            Game.getSoundManager().stop(Previous.Sound());
        }
        if (!Payload.Playing() || Game.level == null) {
            return;
        }
        Entity Shooter = Game.level.getEntity(Payload.ShooterId());
        if (Shooter == null) {
            return;
        }
        SoundInstance Sound = new EntityBoundSoundInstance(UltralaserRegistry.Priming.get(), SoundSource.PLAYERS, UltralaserSounds.PrimingVolume, 1.0F, Shooter,
                Game.level.getRandom().nextLong());
        Playing.put(Payload.ShooterId(), new Clip(Sound, Ticks));
        Game.getSoundManager().play(Sound);
        if (Local) {
            LocalStart = System.nanoTime();
            LocalShown = -1;
            LocalActive = true;
            showCues(Game.player);
        }
    }

    public static float localTicks() {
        return (float) ((System.nanoTime() - LocalStart) / NanosPerTick);
    }

    public static float primeTicks(Player Holder, GunTimer Charging, float Partial) {
        if (LocalActive && Holder == Minecraft.getInstance().player) {
            return Math.min(localTicks(), UltralaserScore.PrimeTicks);
        }
        return Holder.level().getGameTime() - Charging.Start() + Partial;
    }

    public static float primeProgress(Player Holder, GunTimer Charging, float Partial) {
        return Mth.clamp(primeTicks(Holder, Charging, Partial) / UltralaserScore.PrimeTicks, 0.0F, 1.0F);
    }

    private static void showCues(LocalPlayer User) {
        List<UltralaserScore.Cue> Cues = UltralaserScore.cues();
        float Due = localTicks() + UltralaserScore.TextLeadTicks;
        while (LocalShown + 1 < Cues.size() && Cues.get(LocalShown + 1).Tick() <= Due) {
            LocalShown++;
            User.displayClientMessage(UltralaserPriming.cue(Cues.get(LocalShown)), true);
        }
        if (LocalShown + 1 >= Cues.size() && localTicks() > UltralaserScore.PrimeTicks + ClockTail) {
            LocalActive = false;
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        LocalPlayer User = Minecraft.getInstance().player;
        if (LocalActive && User != null) {
            showCues(User);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Ticks++;
        if (Playing.isEmpty()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            Playing.clear();
            return;
        }
        Playing.values().removeIf(Entry -> Ticks - Entry.Started() > StartGrace && !Game.getSoundManager().isActive(Entry.Sound()));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Playing.clear();
        LocalActive = false;
    }
}
