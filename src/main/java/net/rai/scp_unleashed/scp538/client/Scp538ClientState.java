package net.rai.scp_unleashed.scp538.client;

import net.rai.scp_unleashed.scp538.network.Scp538InfectionPayload;

public final class Scp538ClientState {
    private static volatile int Stage;
    private static volatile int Progress;

    private Scp538ClientState() {
    }

    public static void receive(Scp538InfectionPayload Payload) {
        Stage = Math.max(0, Payload.Stage());
        Progress = Math.max(0, Payload.Progress());
        if (Stage == 0) {
            Scp538Crawlers.reset();
        }
    }

    public static int stage() {
        return Stage;
    }

    public static int progress() {
        return Progress;
    }

    public static void reset() {
        Stage = 0;
        Progress = 0;
    }
}
