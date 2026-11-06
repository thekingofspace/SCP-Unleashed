package net.rai.scp_unleashed.z0002.client;

import net.rai.scp_unleashed.z0002.network.Z0002ChannelPayload;
import net.rai.scp_unleashed.z0002.network.Z0002DomainPayload;
import net.rai.scp_unleashed.z0002.network.Z0002WarningPayload;

public final class Z0002ClientHooks {
    private Z0002ClientHooks() {
    }

    public static void domain(Z0002DomainPayload Payload) {
        Z0002ClientDomain.accept(Payload);
    }

    public static void channel(Z0002ChannelPayload Payload) {
        Z0002ClientChannel.accept(Payload);
    }

    public static void warning(Z0002WarningPayload Payload) {
        Z0002Warning.begin(Payload.Ticks());
    }
}
