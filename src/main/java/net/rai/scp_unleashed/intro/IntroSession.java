package net.rai.scp_unleashed.intro;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class IntroSession {
    public static final int Waiting = 0;
    public static final int Office = 1;
    public static final int Gate = 2;
    public static final int Crossing = 3;
    public static final int Returning = 4;
    public final UUID Player;
    public int Stage = Waiting;
    public int Token;
    public boolean Ready;
    public int Ticks;
    public boolean Replay;
    public int Plot = -1;
    public boolean Skipped;
    public boolean Stood;
    @Nullable
    public UUID Scientist;

    public IntroSession(UUID Player) {
        this.Player = Player;
    }

    public boolean playing() {
        return Stage == Office || Stage == Gate || Stage == Crossing;
    }

    public static int awaitStage(int Stage) {
        return switch (Stage) {
            case Office -> IntroDirector.AwaitOffice;
            case Gate -> IntroDirector.AwaitGate;
            case Returning -> IntroDirector.AwaitReturn;
            default -> IntroDirector.AwaitStart;
        };
    }
}
