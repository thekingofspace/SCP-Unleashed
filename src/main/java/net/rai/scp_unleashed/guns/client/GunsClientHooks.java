package net.rai.scp_unleashed.guns.client;

import net.rai.scp_unleashed.guns.network.GunAckPayload;
import net.rai.scp_unleashed.guns.network.GunShotPayload;
import net.rai.scp_unleashed.guns.network.GunSoundPayload;

public final class GunsClientHooks {
    private GunsClientHooks() {
    }

    public static void onShot(GunShotPayload Payload) {
        GunEffects.onShot(Payload);
    }

    public static void onAck(GunAckPayload Payload) {
        GunClientState.acknowledge(Payload.Sequence(), Payload.Ammo());
    }

    public static void onSound(GunSoundPayload Payload) {
        GunClientSounds.cue(Payload.Cue(), Payload.Kind(), Payload.Scale());
    }
}
