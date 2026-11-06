package net.rai.scp_unleashed.warhead.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.warhead.WarheadRegistry;
import net.rai.scp_unleashed.warhead.network.WarheadAlertPayload;
import net.rai.scp_unleashed.warhead.network.WarheadAnnouncePayload;

import java.util.ArrayList;
import java.util.List;

public final class WarheadAlarm {
    public static final int Timeout = 60;
    public static final float[] Rising = {1.0595f, 1.4142f, 1.7818f};
    public static final float[] Falling = {1.7818f, 1.4142f, 1.0595f};
    public static final int ChimeSpacing = 6;
    public static final int ChimeLead = 16;
    public static final int DingLead = 2;

    private static boolean Active;
    private static BlockPos Source = BlockPos.ZERO;
    private static float TicksLeft;
    private static boolean Locked;
    private static boolean Danger;
    private static int Silence;
    private static long Clock;
    private static int TitleSerial;
    private static final List<Pending> Queue = new ArrayList<>();

    private record Pending(long At, Runnable Action) {
    }

    private WarheadAlarm() {
    }

    public static void reset() {
        Active = false;
        Locked = false;
        Danger = false;
        TicksLeft = 0.0f;
        Queue.clear();
        WarheadSiren.stop();
    }

    public static boolean isActive() {
        return Active;
    }

    public static boolean isLocked() {
        return Locked;
    }

    public static boolean isDanger() {
        return Active && Danger;
    }

    public static float ticksLeft() {
        return TicksLeft;
    }

    public static long clock() {
        return Clock;
    }

    public static float sirenLevel() {
        LocalPlayer Viewer = Minecraft.getInstance().player;
        if (Viewer == null) {
            return 0.0f;
        }
        double Distance = Math.sqrt(Viewer.distanceToSqr(Vec3.atCenterOf(Source)));
        return (float) Mth.clamp(1.1 - Distance / 320.0, 0.35, 1.0);
    }

    public static void alert(WarheadAlertPayload Payload) {
        if (!Payload.Active()) {
            Active = false;
            return;
        }
        Active = true;
        Source = Payload.Source();
        TicksLeft = Payload.TicksLeft();
        Locked = Payload.Locked();
        Danger = Payload.Danger();
        Silence = 0;
    }

    public static void tick(Minecraft Client) {
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        for (Pending Item : List.copyOf(Queue)) {
            if (Item.At() <= Clock) {
                Queue.remove(Item);
                Item.Action().run();
            }
        }
        if (Active) {
            TicksLeft = Math.max(0.0f, TicksLeft - 1.0f);
            if (++Silence > Timeout || Client.player == null) {
                Active = false;
            }
        }
        WarheadSiren.tick(Client.getSoundManager(), Active && WarheadEffects.enabled(), Locked, sirenLevel());
    }

    private static void later(int Delay, Runnable Action) {
        Queue.add(new Pending(Clock + Delay, Action));
    }

    private static void chimeNote(int Delay, float Pitch) {
        Minecraft Client = Minecraft.getInstance();
        playFlat(Client, WarheadRegistry.Chime.get(), 0.9f, Pitch, Delay);
        playFlat(Client, WarheadRegistry.ChimeSoft.get(), 0.45f, Pitch, Delay);
    }

    private static void playFlat(Minecraft Client, SoundEvent Event, float Volume, float Pitch, int Delay) {
        SimpleSoundInstance Sound = new SimpleSoundInstance(Event.getLocation(), SoundSource.BLOCKS, Volume, Pitch, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        if (Delay > 0) {
            Client.getSoundManager().playDelayed(Sound, Delay);
        } else {
            Client.getSoundManager().play(Sound);
        }
    }

    private static int chime(float[] Notes) {
        for (int Index = 0; Index < Notes.length; Index++) {
            chimeNote(Index * ChimeSpacing, Notes[Index]);
        }
        return ChimeLead;
    }

    private static int ding() {
        chimeNote(0, Rising[2]);
        return DingLead;
    }

    private static MutableComponent cassie(Component Body) {
        return Component.empty().append(Component.translatable("announce.scp_unleashed.warhead.cassie").withStyle(ChatFormatting.GOLD)).append(" ").append(Body.copy().withStyle(ChatFormatting.WHITE));
    }

    private static void show(Component Title, Component Subtitle, Component Line, int FadeIn, int Stay, int FadeOut) {
        Minecraft Client = Minecraft.getInstance();
        if (Title != null || Subtitle != null) {
            int Serial = ++TitleSerial;
            Client.gui.setTimes(FadeIn, Stay, FadeOut);
            Client.gui.setSubtitle(Subtitle == null ? Component.empty() : Subtitle);
            Client.gui.setTitle(Title == null ? Component.empty() : Title);
            later(FadeIn + Stay + FadeOut + 1, () -> {
                if (Serial == TitleSerial) {
                    Minecraft.getInstance().gui.resetTitleTimes();
                }
            });
        }
        if (Line != null) {
            Client.gui.setOverlayMessage(Line, false);
        }
    }

    public static void announce(WarheadAnnouncePayload Payload) {
        int Seconds = Payload.Seconds();
        switch (Payload.Kind()) {
            case WarheadAnnouncePayload.Start -> later(chime(Rising), () -> show(
                    Component.translatable("announce.scp_unleashed.warhead.start_title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.translatable("announce.scp_unleashed.warhead.start_subtitle", Seconds).withStyle(ChatFormatting.GOLD),
                    cassie(Component.translatable("announce.scp_unleashed.warhead.start")), 6, 70, 20));
            case WarheadAnnouncePayload.Remaining -> later(chime(Rising), () -> show(null, null,
                    cassie(Component.translatable(Seconds == 60 ? "announce.scp_unleashed.warhead.evacuate" : "announce.scp_unleashed.warhead.remaining", Seconds)), 0, 0, 0));
            case WarheadAnnouncePayload.Final -> {
                if (Seconds == 10) {
                    chime(Rising);
                } else {
                    ding();
                }
                later(DingLead, () -> show(Component.literal(Integer.toString(Seconds)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        Seconds == 10 ? Component.translatable("announce.scp_unleashed.warhead.final_subtitle").withStyle(ChatFormatting.GOLD) : null,
                        cassie(Component.translatable("announce.scp_unleashed.warhead.count." + Mth.clamp(Seconds, 1, 10))), 0, 16, 4));
            }
            case WarheadAnnouncePayload.Cancelled -> later(chime(Falling), () -> show(Component.empty(),
                    Component.translatable("announce.scp_unleashed.warhead.cancelled_subtitle").withStyle(ChatFormatting.GREEN),
                    cassie(Component.translatable("announce.scp_unleashed.warhead.cancelled")), 4, 60, 20));
            case WarheadAnnouncePayload.Detonation -> show(Component.translatable("announce.scp_unleashed.warhead.detonation_title").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), null,
                    cassie(Component.translatable("announce.scp_unleashed.warhead.detonation")), 0, 40, 30);
            case WarheadAnnouncePayload.Failed -> later(chime(Falling), () -> show(null, null, cassie(Component.translatable("announce.scp_unleashed.warhead.failed")), 0, 0, 0));
            default -> {
            }
        }
    }
}
