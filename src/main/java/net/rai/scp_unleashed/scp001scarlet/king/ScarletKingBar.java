package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ScarletKingBar {
    private static ServerBossEvent Current;
    private static int Generation = Integer.MIN_VALUE;

    private ScarletKingBar() {
    }

    public static synchronized ServerBossEvent of(int Fight) {
        if (Current == null || Generation != Fight) {
            if (Current != null) {
                Current.removeAllPlayers();
            }
            Current = new ServerBossEvent(Component.translatable("boss.scp_unleashed.scarlet_king.0"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_12);
            Current.setDarkenScreen(true);
            Generation = Fight;
        }
        return Current;
    }

    @Nullable
    public static synchronized ServerBossEvent current() {
        return Current;
    }

    public static synchronized boolean owns(int Fight) {
        return Current != null && Generation == Fight;
    }

    public static synchronized void clear(int Fight) {
        if (owns(Fight)) {
            Current.removeAllPlayers();
        }
    }

    public static synchronized void forget() {
        if (Current != null) {
            Current.removeAllPlayers();
        }
        Current = null;
        Generation = Integer.MIN_VALUE;
    }

    public static synchronized void drop(ServerPlayer Leaving) {
        if (Current == null) {
            return;
        }
        for (ServerPlayer Listed : List.copyOf(Current.getPlayers())) {
            if (Listed == Leaving || Listed.getUUID().equals(Leaving.getUUID())) {
                Current.removePlayer(Listed);
            }
        }
    }

    public static synchronized void sync(ServerBossEvent Bar, List<ServerPlayer> Wanted) {
        Set<ServerPlayer> Keep = Collections.newSetFromMap(new IdentityHashMap<>());
        Keep.addAll(Wanted);
        for (ServerPlayer Listed : List.copyOf(Bar.getPlayers())) {
            if (!Keep.contains(Listed)) {
                Bar.removePlayer(Listed);
            }
        }
        for (ServerPlayer Each : Wanted) {
            if (!Bar.getPlayers().contains(Each)) {
                Bar.addPlayer(Each);
            }
        }
    }
}
