package net.rai.scp_unleashed.dclass.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.rai.scp_unleashed.dclass.network.OpenPersonnelFilePayload;

public final class DClassClientHooks {
    private DClassClientHooks() {
    }

    public static void openFile(OpenPersonnelFilePayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        Game.setScreen(new PersonnelFileScreen(Payload.MainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, Payload.Status(), Payload.Controller()));
    }
}
