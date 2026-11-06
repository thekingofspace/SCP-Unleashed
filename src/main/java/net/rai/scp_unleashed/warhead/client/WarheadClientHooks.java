package net.rai.scp_unleashed.warhead.client;

import net.rai.scp_unleashed.warhead.network.WarheadAlertPayload;
import net.rai.scp_unleashed.warhead.network.WarheadAnnouncePayload;
import net.rai.scp_unleashed.warhead.network.WarheadBlastPayload;

public final class WarheadClientHooks {
    private WarheadClientHooks() {
    }

    public static void alert(WarheadAlertPayload Payload) {
        WarheadAlarm.alert(Payload);
    }

    public static void announce(WarheadAnnouncePayload Payload) {
        WarheadAlarm.announce(Payload);
    }

    public static void blast(WarheadBlastPayload Payload) {
        WarheadEffects.blast(Payload.centre());
    }
}
