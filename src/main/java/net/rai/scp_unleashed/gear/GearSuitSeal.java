package net.rai.scp_unleashed.gear;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GearSuitSeal {
    private static final List<Cue> Sequence = List.of(
            new Cue(0, GearSounds.HazmatSeal),
            new Cue(0, GearSounds.HazmatClasp),
            new Cue(9, GearSounds.HazmatValve),
            new Cue(9, GearSounds.HazmatHiss),
            new Cue(26, GearSounds.HazmatBreath),
            new Cue(26, GearSounds.HazmatIntake));
    private static final Map<UUID, Boolean> Sealed = new HashMap<>();
    private static final Map<UUID, Progress> Playing = new HashMap<>();

    private GearSuitSeal() {
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        Player User = Event.getEntity();
        if (User.level().isClientSide()) {
            return;
        }
        UUID Id = User.getUUID();
        boolean Full = GearTags.wearsFullHazmatSuit(User);
        Boolean Before = Sealed.put(Id, Full);
        if (!Full) {
            Playing.remove(Id);
            return;
        }
        if (Boolean.FALSE.equals(Before)) {
            Playing.put(Id, new Progress(User.level().getGameTime()));
        }
        Progress Current = Playing.get(Id);
        if (Current != null && advance(User, Current)) {
            Playing.remove(Id);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Sealed.remove(Event.getEntity().getUUID());
        Playing.remove(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Sealed.clear();
        Playing.clear();
    }

    private static boolean advance(Player User, Progress Current) {
        long Elapsed = User.level().getGameTime() - Current.Start;
        while (Current.Next < Sequence.size() && Sequence.get(Current.Next).Delay() <= Elapsed) {
            Cue Step = Sequence.get(Current.Next);
            User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), Step.Effect().get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            Current.Next++;
        }
        return Current.Next >= Sequence.size();
    }

    private record Cue(int Delay, DeferredHolder<SoundEvent, SoundEvent> Effect) {
    }

    private static final class Progress {
        private final long Start;
        private int Next;

        private Progress(long Start) {
            this.Start = Start;
        }
    }
}
