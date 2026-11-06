package net.rai.scp_unleashed.scp140;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;
import net.rai.scp_unleashed.scp140.network.Scp140ChroniclePayload;
import net.rai.scp_unleashed.senses.Senses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Scp140Reading {
    public static final long ProgressInterval = 10L;
    private static final Set<UUID> Readers = new HashSet<>();
    private static long LastProgress = Long.MIN_VALUE / 2L;

    private Scp140Reading() {
    }

    public static void open(ServerPlayer User) {
        if (Senses.refuseSight(User)) {
            return;
        }
        Readers.add(User.getUUID());
        PacketDistributor.sendToPlayer(User, payload(User, true));
    }

    public static void closed(ServerPlayer User) {
        Readers.remove(User.getUUID());
    }

    public static void forget(UUID Reader) {
        Readers.remove(Reader);
    }

    public static void clear() {
        Readers.clear();
        LastProgress = Long.MIN_VALUE / 2L;
    }

    public static void seen(ServerPlayer User, int Page) {
        Scp140Chronicle.get(User.server).markSeen(User.getUUID(), Page);
    }

    public static void welcome(ServerPlayer User) {
        PacketDistributor.sendToPlayer(User, payload(User, false));
    }

    public static void broadcast(MinecraftServer Server) {
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(User, payload(User, false));
        }
    }

    public static void progress(MinecraftServer Server) {
        long Now = Server.overworld().getGameTime();
        if (Readers.isEmpty() || Now - LastProgress < ProgressInterval) {
            return;
        }
        LastProgress = Now;
        for (UUID Reader : List.copyOf(Readers)) {
            ServerPlayer User = Server.getPlayerList().getPlayer(Reader);
            if (User == null) {
                Readers.remove(Reader);
                continue;
            }
            PacketDistributor.sendToPlayer(User, payload(User, false));
        }
    }

    public static Scp140ChroniclePayload payload(ServerPlayer User, boolean Open) {
        Scp140Chronicle Chronicle = Scp140Chronicle.get(User.server);
        List<Scp140ChroniclePayload.PageEntry> Pages = new ArrayList<>();
        for (int Page = 1; Page <= Math.min(Scp140Pages.Total, Chronicle.written()); Page++) {
            Scp140Page Entry = Chronicle.page(Page);
            if (Entry == null) {
                Pages.add(new Scp140ChroniclePayload.PageEntry(Page, false, false, 0, 0, 0L));
                continue;
            }
            Pages.add(new Scp140ChroniclePayload.PageEntry(Page, Entry.hasSite(), Entry.sameRealm(), Entry.offsetX(), Entry.offsetZ(), Entry.Day()));
        }
        Scp140Mood Mood = Chronicle.mood(User.serverLevel().getGameTime());
        return new Scp140ChroniclePayload(Open, Chronicle.written(), (float) Chronicle.progress(), Chronicle.seenMask(User.getUUID()), Mood.ordinal(), Pages);
    }
}
