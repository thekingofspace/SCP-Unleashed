package net.rai.scp_unleashed.reactor.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.reactor.network.ReactorSnapshotPayload;

public final class ReactorClientHooks {
    private ReactorClientHooks() {
    }

    public static void snapshot(ReactorSnapshotPayload Payload) {
        if (Minecraft.getInstance().screen instanceof ReactorControllerScreen Screen && Screen.getMenu().containerId == Payload.ContainerId()) {
            Screen.accept(Payload.Data());
        }
    }
}
