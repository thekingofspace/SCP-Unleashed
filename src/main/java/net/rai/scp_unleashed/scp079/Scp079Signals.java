package net.rai.scp_unleashed.scp079;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.RedstonePortBlockEntity;
import net.rai.scp_unleashed.block.entity.RedstoneReceiverBlockEntity;
import net.rai.scp_unleashed.wire.WireMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp079Signals {
    public static final int Refresh = 10;
    public static final int FlickerStep = 4;
    public static final int MaxSources = 64;
    public static final int FullSignal = 15;
    private static final Map<ServerLevel, List<Hold>> Holds = new WeakHashMap<>();

    private Scp079Signals() {
    }

    public static void phantom(ServerLevel World, BlockPos Receiver, BlockPos Self, int Ticks) {
        add(World, new Hold(Receiver.immutable(), Self.immutable(), List.of(), Mode.Phantom, World.getGameTime(), World.getGameTime() + Ticks));
    }

    public static void flicker(ServerLevel World, BlockPos Receiver, BlockPos Self, int Ticks) {
        add(World, new Hold(Receiver.immutable(), Self.immutable(), List.of(), Mode.Flicker, World.getGameTime(), World.getGameTime() + Ticks));
    }

    public static void jam(ServerLevel World, BlockPos Receiver, BlockPos Self, List<BlockPos> Sources, int Ticks) {
        List<BlockPos> Kept = new ArrayList<>(Sources.subList(0, Math.min(MaxSources, Sources.size())));
        add(World, new Hold(Receiver.immutable(), Self.immutable(), List.copyOf(Kept), Mode.Jam, World.getGameTime(), World.getGameTime() + Ticks));
    }

    private static void add(ServerLevel World, Hold Entry) {
        List<Hold> Active = Holds.computeIfAbsent(World, Key -> new ArrayList<>());
        Active.removeIf(Other -> Other.Receiver.equals(Entry.Receiver));
        Active.add(Entry);
        RedstoneReceiverBlockEntity Target = receiverAt(World, Entry.Receiver);
        if (Target != null) {
            step(World.getGameTime(), Entry, Target);
        }
    }

    public static void release(ServerLevel World, BlockPos Self) {
        List<Hold> Active = Holds.get(World);
        if (Active == null) {
            return;
        }
        Iterator<Hold> Each = Active.iterator();
        while (Each.hasNext()) {
            Hold Entry = Each.next();
            if (!Entry.Self.equals(Self)) {
                continue;
            }
            RedstoneReceiverBlockEntity Target = receiverAt(World, Entry.Receiver);
            if (Target != null && Entry.Kind != Mode.Jam) {
                send(Target, Entry.Self, 0);
            }
            Each.remove();
        }
    }

    public static int activeCount(ServerLevel World) {
        List<Hold> Active = Holds.get(World);
        return Active == null ? 0 : Active.size();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        List<Hold> Active = Holds.get(World);
        if (Active == null || Active.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        Iterator<Hold> Each = Active.iterator();
        while (Each.hasNext()) {
            Hold Entry = Each.next();
            RedstoneReceiverBlockEntity Target = receiverAt(World, Entry.Receiver);
            if (Target == null) {
                Each.remove();
                continue;
            }
            if (Now >= Entry.Until) {
                if (Entry.Kind != Mode.Jam) {
                    send(Target, Entry.Self, 0);
                }
                Each.remove();
                continue;
            }
            step(Now, Entry, Target);
        }
    }

    private static void step(long Now, Hold Entry, RedstoneReceiverBlockEntity Target) {
        long Age = Now - Entry.Started;
        switch (Entry.Kind) {
            case Phantom -> {
                if (Age % Refresh == 0) {
                    send(Target, Entry.Self, FullSignal);
                }
            }
            case Flicker -> {
                boolean On = (Age / FlickerStep) % 2 == 0;
                if (On != Entry.On || Age % Refresh == 0) {
                    Entry.On = On;
                    send(Target, Entry.Self, On ? FullSignal : 0);
                }
            }
            case Jam -> {
                for (BlockPos Source : Entry.Sources) {
                    send(Target, Source, 0);
                }
            }
        }
    }

    private static RedstoneReceiverBlockEntity receiverAt(ServerLevel World, BlockPos Pos) {
        return World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof RedstoneReceiverBlockEntity Receiver ? Receiver : null;
    }

    private static void send(RedstoneReceiverBlockEntity Target, BlockPos Source, int Signal) {
        CompoundTag Payload = new CompoundTag();
        Payload.putInt(RedstonePortBlockEntity.SignalKey, Signal);
        Target.receiveMessage(new WireMessage(Target.getChannel(), Payload, Source));
    }

    private enum Mode {
        Phantom,
        Flicker,
        Jam
    }

    private static final class Hold {
        private final BlockPos Receiver;
        private final BlockPos Self;
        private final List<BlockPos> Sources;
        private final Mode Kind;
        private final long Started;
        private final long Until;
        private boolean On;

        private Hold(BlockPos Receiver, BlockPos Self, List<BlockPos> Sources, Mode Kind, long Started, long Until) {
            this.Receiver = Receiver;
            this.Self = Self;
            this.Sources = Sources;
            this.Kind = Kind;
            this.Started = Started;
            this.Until = Until;
        }
    }
}
