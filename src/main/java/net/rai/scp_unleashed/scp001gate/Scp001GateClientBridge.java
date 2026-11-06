package net.rai.scp_unleashed.scp001gate;

import java.util.function.Consumer;

public final class Scp001GateClientBridge {
    private static Consumer<Scp001GateGuardian> Hook = Guardian -> {
    };

    private Scp001GateClientBridge() {
    }

    public static void install(Consumer<Scp001GateGuardian> Next) {
        Hook = Next;
    }

    public static void tick(Scp001GateGuardian Guardian) {
        Hook.accept(Guardian);
    }
}
