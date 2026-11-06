package net.rai.scp_unleashed.scp105.photo096.client;

import net.minecraft.client.Minecraft;

public final class Scp105Photo096ClientHooks {
    private Scp105Photo096ClientHooks() {
    }

    public static void open() {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player != null) {
            Game.setScreen(new Scp105Photo096Screen());
        }
    }
}
