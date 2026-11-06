package net.rai.scp_unleashed.scp3008.staff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp3008.Scp3008Hours;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp3008StaffAnnouncer {
    public static final int Interval = 3000;
    public static final int FirstDelay = 60;
    public static final int CheckInterval = 20;
    public static final double Range = 48.0;
    public static final int SecondChime = 9;
    public static final int VoiceDelay = 24;
    public static final float ChimeHigh = 1.19f;
    public static final float ChimeLow = 0.94f;
    public static final float ChimeVolume = 0.8f;
    public static final float VoiceVolume = 0.9f;
    public static final String MessageKey = "message.scp_unleashed.scp3008_staff.closing";
    private static final Map<ResourceKey<Level>, Shift> Shifts = new HashMap<>();
    private static final Map<UUID, Heard> Listeners = new HashMap<>();
    private static final List<Pending> Queue = new ArrayList<>();

    private static final class Shift {
        private boolean Night;
        private int Wave;
        private long Started;
        private long Next;
    }

    private record Heard(ResourceKey<Level> Where, int Wave) {
    }

    private record Pending(UUID Listener, long Due, SoundEvent Sound, float Volume, float Pitch) {
    }

    private Scp3008StaffAnnouncer() {
    }

    public static void tick(ServerLevel World) {
        long Time = World.getGameTime();
        advance(World, Scp3008Hours.night(World), Time);
        flush(World, Time);
        Shift Current = shift(World);
        if (Current.Night && Time - Current.Started >= FirstDelay && Time % CheckInterval == 0) {
            deliver(World, Current.Wave, Time);
        }
    }

    private static Shift shift(ServerLevel World) {
        return Shifts.computeIfAbsent(World.dimension(), Key -> new Shift());
    }

    public static int advance(ServerLevel World, boolean Night, long Time) {
        Shift Current = shift(World);
        if (Night && (!Current.Night || Time >= Current.Next)) {
            Current.Wave++;
            Current.Started = Time;
            Current.Next = Time + Interval;
        }
        Current.Night = Night;
        return Current.Wave;
    }

    public static int wave(ServerLevel World) {
        return shift(World).Wave;
    }

    public static int deliver(ServerLevel World, int Wave, long Time) {
        int Count = 0;
        for (ServerPlayer User : new ArrayList<>(World.players())) {
            if (User.isSpectator() || heard(World, User, Wave)) {
                continue;
            }
            Scp3008Staff Speaker = nearestStaff(World, User);
            if (Speaker == null) {
                continue;
            }
            Listeners.put(User.getUUID(), new Heard(World.dimension(), Wave));
            announce(World, User, Speaker, Time);
            Count++;
        }
        return Count;
    }

    public static boolean heard(ServerLevel World, ServerPlayer User, int Wave) {
        Heard Last = Listeners.get(User.getUUID());
        return Last != null && Last.Where().equals(World.dimension()) && Last.Wave() >= Wave;
    }

    @Nullable
    public static Scp3008Staff nearestStaff(ServerLevel World, ServerPlayer User) {
        return World.getEntitiesOfClass(Scp3008Staff.class, User.getBoundingBox().inflate(Range), Each -> Each.isAlive() && Each.distanceToSqr(User) <= Range * Range).stream()
                .min(Comparator.comparingDouble(Each -> Each.distanceToSqr(User))).orElse(null);
    }

    private static void announce(ServerLevel World, ServerPlayer User, Scp3008Staff Speaker, long Time) {
        Speaker.announce();
        User.displayClientMessage(Component.translatable(MessageKey).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        queue(User, Time, Scp3008StaffRegistry.ChimeLayers, ChimeVolume, ChimeHigh);
        queue(User, Time + SecondChime, Scp3008StaffRegistry.ChimeLayers, ChimeVolume, ChimeLow);
        queue(User, Time + VoiceDelay, Scp3008StaffRegistry.VoiceLayers, VoiceVolume, 0.92f + World.getRandom().nextFloat() * 0.08f);
        flush(World, Time);
    }

    private static void queue(ServerPlayer User, long Due, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Queue.add(new Pending(User.getUUID(), Due, Layer.get(), Volume, Pitch));
        }
    }

    private static void flush(ServerLevel World, long Time) {
        Iterator<Pending> Each = Queue.iterator();
        while (Each.hasNext()) {
            Pending Item = Each.next();
            if (Item.Due() > Time) {
                continue;
            }
            Entity Found = World.getEntity(Item.Listener());
            if (Found instanceof ServerPlayer User) {
                User.playNotifySound(Item.Sound(), SoundSource.AMBIENT, Item.Volume(), Item.Pitch());
                Each.remove();
            } else if (Item.Due() < Time - CheckInterval * 10L) {
                Each.remove();
            }
        }
    }

    public static void forget(ServerLevel World) {
        Shifts.remove(World.dimension());
        Listeners.values().removeIf(Last -> Last.Where().equals(World.dimension()));
    }

    public static void forget() {
        Shifts.clear();
        Listeners.clear();
        Queue.clear();
    }
}
