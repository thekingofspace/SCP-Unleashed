package net.rai.scp_unleashed.scp024;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp024.network.Scp024BarPayload;
import net.rai.scp_unleashed.scp024.network.Scp024ClearPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp024Hud {
    private static final Map<UUID, ServerBossEvent> Bars = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> Shown = new ConcurrentHashMap<>();

    private Scp024Hud() {
    }

    @Nullable
    public static ServerBossEvent bar(Scp024Run Run, ServerPlayer User) {
        if (Run.Ended || !User.isAlive() || !Scp024Stage.inStage(User)) {
            return null;
        }
        ServerBossEvent Bar = Bars.get(User.getUUID());
        if (Bar == null) {
            Bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
            Bars.put(User.getUUID(), Bar);
            Shown.computeIfAbsent(User.getUUID(), Ignored -> ConcurrentHashMap.newKeySet()).add(Bar.getId());
            PacketDistributor.sendToPlayer(User, new Scp024BarPayload(Bar.getId()));
        }
        for (ServerPlayer Holder : new ArrayList<>(Bar.getPlayers())) {
            if (Holder != User) {
                Bar.removePlayer(Holder);
            }
        }
        if (!Bar.getPlayers().contains(User)) {
            Bar.addPlayer(User);
        }
        Bar.setVisible(true);
        return Bar;
    }

    public static void hideBar(UUID Contestant) {
        ServerBossEvent Bar = Bars.get(Contestant);
        if (Bar != null) {
            Bar.removeAllPlayers();
            Bar.setVisible(false);
        }
    }

    public static void clear(MinecraftServer Server, UUID Contestant) {
        ServerPlayer User = Server.getPlayerList().getPlayer(Contestant);
        ServerBossEvent Bar = Bars.remove(Contestant);
        if (Bar != null) {
            Bar.removeAllPlayers();
            Bar.setVisible(false);
        }
        for (ServerBossEvent Other : Bars.values()) {
            for (ServerPlayer Holder : new ArrayList<>(Other.getPlayers())) {
                if (Holder.getUUID().equals(Contestant)) {
                    Other.removePlayer(Holder);
                }
            }
        }
        if (User == null) {
            return;
        }
        List<UUID> Known = new ArrayList<>(Shown.getOrDefault(Contestant, Set.of()));
        for (UUID Id : Known) {
            User.connection.send(ClientboundBossEventPacket.createRemovePacket(Id));
        }
        User.connection.send(new ClientboundClearTitlesPacket(true));
        User.displayClientMessage(Component.empty(), true);
        PacketDistributor.sendToPlayer(User, new Scp024ClearPayload(Known));
    }

    public static void forget(UUID Contestant) {
        Shown.remove(Contestant);
    }

    public static int barsHolding(UUID Contestant) {
        int Count = 0;
        for (ServerBossEvent Bar : Bars.values()) {
            for (ServerPlayer Holder : Bar.getPlayers()) {
                if (Holder.getUUID().equals(Contestant) && Bar.isVisible()) {
                    Count++;
                }
            }
        }
        return Count;
    }

    public static int liveBars() {
        return Bars.size();
    }

    public static void reset() {
        for (ServerBossEvent Bar : Bars.values()) {
            Bar.removeAllPlayers();
            Bar.setVisible(false);
        }
        Bars.clear();
        Shown.clear();
    }
}
