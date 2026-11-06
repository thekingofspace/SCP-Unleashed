package net.rai.scp_unleashed.scp5001.mekhane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneFxPayload;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneSitePayload;

public final class MekhaneClientHooks {
    private MekhaneClientHooks() {
    }

    public static void openConsole(BlockPos Pos) {
        Minecraft.getInstance().setScreen(new MekhaneConsoleScreen(Pos));
    }

    public static void fx(MekhaneFxPayload Payload) {
        MekhaneClientState.fx(Payload);
    }

    public static void site(MekhaneSitePayload Payload) {
        MekhaneClientState.site(Payload);
    }
}
