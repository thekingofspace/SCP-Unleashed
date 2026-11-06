package net.rai.scp_unleashed.computer.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.computer.network.ComputerClosePayload;
import net.rai.scp_unleashed.computer.network.ComputerNoticePayload;
import net.rai.scp_unleashed.computer.network.ComputerOpenPayload;
import net.rai.scp_unleashed.computer.network.ComputerSyncPayload;

public final class ComputerClientHooks {
    private ComputerClientHooks() {
    }

    public static void open(ComputerOpenPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player == null || Game.level == null) {
            return;
        }
        if (Payload.Screen() == ComputerOpenPayload.DatabaseScreen) {
            if (Game.screen instanceof MessageDatabaseScreen Open && Open.getPos().equals(Payload.Pos())) {
                Open.accept(Payload.Data());
            } else {
                Game.setScreen(new MessageDatabaseScreen(Payload.Pos(), Payload.Data()));
            }
            return;
        }
        if (!(Game.screen instanceof ComputerScreen Open && Open.getPos().equals(Payload.Pos()))) {
            Game.setScreen(new ComputerScreen(Payload.Pos()));
        }
    }

    public static void sync(ComputerSyncPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level != null && Game.screen instanceof ComputerScreen Open && Open.getPos().equals(Payload.Pos())) {
            Open.accept(Payload.Section(), Payload.Data(), Game.level.registryAccess());
        }
    }

    public static void close(ComputerClosePayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.screen instanceof ComputerScreen Open && Open.getPos().equals(Payload.Pos())) {
            Open.lost(Payload.Reason());
        }
    }

    public static void notice(ComputerNoticePayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.screen instanceof ComputerScreen Open && Open.getPos().equals(Payload.Pos())) {
            Open.notice(Payload.Text(), Payload.Tone());
        }
    }
}
