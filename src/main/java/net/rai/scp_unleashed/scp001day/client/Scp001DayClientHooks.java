package net.rai.scp_unleashed.scp001day.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.scp001day.network.Scp001DayTurningPayload;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.SunVector;

public final class Scp001DayClientHooks {
    private Scp001DayClientHooks() {
    }

    public static void turning(Scp001DayTurningPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level != null) {
            Scp001DayClientState.turning(Payload.EntityId(), Payload.Progress(), Client.level.getGameTime());
        }
    }

    public static void sunlightDebug() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        String Prefix = Scp001DayShafts.Prefix;
        ChatComponent Chat = Client.gui.getChat();
        SunVector Sun = Scp001DaySun.direction(Client.level.getDayTime());
        Chat.addMessage(Component.translatable(Prefix + "status", Scp001DayShafts.status()));
        Chat.addMessage(Component.translatable(Prefix + "grid", Scp001DaySunGrid.progress(), Scp001DaySunGrid.loaded(), Scp001DaySunVolume.progress(), Scp001DayShafts.frames(),
                Component.translatable(Prefix + (Scp001DayShafts.bound() ? "yes" : "no"))));
        Chat.addMessage(Component.translatable(Prefix + "sun", String.format(Locale.ROOT, "%.1f", Scp001DaySun.elevation(Sun)), String.format(Locale.ROOT, "%.2f", Scp001DaySun.intensity(Sun)),
                Math.round(Scp001DayClientState.localExposure() * 100.0f)));
        if (!"outside".equals(Scp001DayShafts.reason())) {
            Scp001DayShafts.measureNextFrame();
        }
    }
}
