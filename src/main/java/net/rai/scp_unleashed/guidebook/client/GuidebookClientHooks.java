package net.rai.scp_unleashed.guidebook.client;

import net.minecraft.client.Minecraft;

public final class GuidebookClientHooks {
    private GuidebookClientHooks() {
    }

    public static void open() {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player != null && !(Game.screen instanceof FacilityTabletScreen)) {
            Game.setScreen(new FacilityTabletScreen());
        }
    }
}
