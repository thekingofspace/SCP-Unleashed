package net.rai.scp_unleashed.scp327.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp327.network.Scp327StrugglePayload;

public final class Scp327ClientStruggle {
    private static boolean Held;
    private static boolean WasDown;
    private static int Thrash;

    private Scp327ClientStruggle() {
    }

    public static void held(boolean Value) {
        Held = Value;
        WasDown = false;
        Thrash = 0;
    }

    public static boolean isHeld() {
        return Held;
    }

    public static float thrash() {
        return Thrash / 6.0f;
    }

    public static void tick(Minecraft Client) {
        if (Thrash > 0) {
            Thrash--;
        }
        if (!Held || Client.player == null || Client.isPaused()) {
            WasDown = false;
            return;
        }
        boolean Down = Client.options.keyJump.isDown();
        if (Down && !WasDown) {
            PacketDistributor.sendToServer(Scp327StrugglePayload.Press);
            Thrash = 6;
        }
        WasDown = Down;
    }

    public static void reset() {
        Held = false;
        WasDown = false;
        Thrash = 0;
    }
}
