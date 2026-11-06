package net.rai.scp_unleashed.scp2317.note.client;

import net.minecraft.client.Minecraft;

public final class Scp2317RiteNoteClientHooks {
    private Scp2317RiteNoteClientHooks() {
    }

    public static void read() {
        Minecraft Client = Minecraft.getInstance();
        if (!(Client.screen instanceof Scp2317RiteNoteScreen)) {
            Client.setScreen(new Scp2317RiteNoteScreen());
        }
    }
}
