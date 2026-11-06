package net.rai.scp_unleashed.cameras.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.rai.scp_unleashed.cameras.network.CameraFeedPayload;
import net.rai.scp_unleashed.cameras.network.CameraWindowPayload;

public final class CameraClientHooks {
    private CameraClientHooks() {
    }

    public static boolean isViewing() {
        return CameraClient.isActive();
    }

    public static void openTablet(InteractionHand Hand) {
        Minecraft.getInstance().setScreen(new CameraTabletScreen(Hand));
    }

    public static void feed(CameraFeedPayload Payload) {
        CameraClient.feed(Payload);
    }

    public static void window(CameraWindowPayload Payload) {
        if (Payload.Open()) {
            CameraClientWindow.open(Payload.X(), Payload.Z());
        } else {
            CameraClientWindow.close();
        }
    }
}
