package net.rai.scp_unleashed.scp024;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Scp024Run {
    public enum Phase {
        Welcome,
        Announce,
        Countdown,
        Playing,
        Eliminating,
        Celebrate,
        Prize,
        Finale
    }

    final UUID PlayerId;
    final int Field;
    int Wins;
    final List<String> Played = new ArrayList<>();
    Phase Stage = Phase.Welcome;
    String GameId = "";
    long Seed;
    int PhaseTicks;
    int Token;
    boolean Waiting;
    final List<Component> Pages = new ArrayList<>();
    final List<Integer> Cues = new ArrayList<>();
    @Nullable
    Scp024Session Session;
    @Nullable
    Scp024Host Host;
    @Nullable
    Scp024Lights LobbyLights;
    UUID LobbyToken = UUID.randomUUID();
    final UUID HoldKey = UUID.randomUUID();
    boolean Ended;

    Scp024Run(UUID PlayerId, int Field) {
        this.PlayerId = PlayerId;
        this.Field = Field;
    }

    public UUID playerId() {
        return PlayerId;
    }

    public int field() {
        return Field;
    }

    public int wins() {
        return Wins;
    }

    public Phase phase() {
        return Stage;
    }

    public String gameId() {
        return GameId;
    }

    public List<String> played() {
        return List.copyOf(Played);
    }

    @Nullable
    public Scp024Session session() {
        return Session;
    }

    public boolean ended() {
        return Ended;
    }
}
