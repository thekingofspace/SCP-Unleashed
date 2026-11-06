package net.rai.scp_unleashed.z0001.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.rai.scp_unleashed.z0001.Z0001Outcome;
import net.rai.scp_unleashed.z0001.network.Z0001DreadPayload;
import net.rai.scp_unleashed.z0001.network.Z0001ResultPayload;

public final class Z0001ClientHooks {
    private Z0001ClientHooks() {
    }

    public static void openBook(InteractionHand Hand) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player != null) {
            Game.setScreen(new Z0001BookScreen(Hand));
        }
    }

    public static void result(Z0001ResultPayload Payload) {
        if (Minecraft.getInstance().screen instanceof Z0001BookScreen Book) {
            Book.accept(Z0001Outcome.byId(Payload.Outcome()), Payload.Name());
        }
    }

    public static void dread(Z0001DreadPayload Payload) {
        Z0001Dread.begin(Payload.Name(), Payload.Ticks());
    }
}
