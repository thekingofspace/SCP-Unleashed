package net.rai.scp_unleashed.scp1762.client;

import net.minecraft.client.Minecraft;

public final class Scp1762ClientHooks {
    private Scp1762ClientHooks() {
    }

    public static void read(int Note) {
        Minecraft.getInstance().setScreen(new Scp1762NoteScreen(Note));
    }
}
