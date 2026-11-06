package net.rai.scp_unleashed.scp444;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp444.network.Scp444OpenPayload;
import net.rai.scp_unleashed.senses.Senses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp444Reading {
    public static final int ReadTicks = 60;
    private static final Map<UUID, Session> Sessions = new HashMap<>();

    private Scp444Reading() {
    }

    public static final class Session {
        private final ServerPlayer Reader;
        private final InteractionHand Hand;
        private int Ticks;
        private boolean Taken;

        private Session(ServerPlayer Reader, InteractionHand Hand) {
            this.Reader = Reader;
            this.Hand = Hand;
        }

        public int ticks() {
            return Ticks;
        }

        public boolean taken() {
            return Taken;
        }
    }

    public static boolean open(ServerPlayer User, InteractionHand Hand) {
        if (Senses.refuseSight(User)) {
            return false;
        }
        Sessions.put(User.getUUID(), new Session(User, Hand));
        Scp444Affliction.playExcept(User, Scp444Registry.OpenLayers, 0.7f, 0.9f + User.getRandom().nextFloat() * 0.1f);
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, Scp444OpenPayload.Instance);
        }
        return true;
    }

    public static boolean isReading(ServerPlayer User) {
        Session Current = Sessions.get(User.getUUID());
        return Current != null && Current.Reader == User;
    }

    public static Session session(ServerPlayer User) {
        return Sessions.get(User.getUUID());
    }

    public static void turned(ServerPlayer User, int Page) {
        Session Current = Sessions.get(User.getUUID());
        if (Current == null || Current.Reader != User || !holding(Current)) {
            return;
        }
        Scp444Affliction.playExcept(User, Scp444Registry.TurnLayers, 0.45f, 0.95f + User.getRandom().nextFloat() * 0.1f);
        if (Page > 0) {
            take(Current);
        }
    }

    public static void closed(ServerPlayer User) {
        Session Current = Sessions.remove(User.getUUID());
        if (Current != null) {
            Scp444Affliction.playExcept(User, Scp444Registry.CloseLayers, 0.7f, 0.9f + User.getRandom().nextFloat() * 0.1f);
        }
    }

    public static void forget(UUID Reader) {
        Sessions.remove(Reader);
    }

    public static void clear() {
        Sessions.clear();
    }

    private static boolean holding(Session Current) {
        return Scp444BookItem.is(Current.Reader.getItemInHand(Current.Hand));
    }

    private static void take(Session Current) {
        if (Current.Taken) {
            return;
        }
        Current.Taken = true;
        Scp444Affliction.read(Current.Reader);
    }

    public static void tick() {
        for (Map.Entry<UUID, Session> Entry : List.copyOf(Sessions.entrySet())) {
            Session Current = Entry.getValue();
            ServerPlayer Reader = Current.Reader;
            if (Reader.isRemoved() || !Reader.isAlive() || !holding(Current) || Senses.isBlindfolded(Reader)) {
                Sessions.remove(Entry.getKey(), Current);
                continue;
            }
            Current.Ticks++;
            if (Current.Ticks >= ReadTicks) {
                take(Current);
            }
        }
    }
}
