package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.network.Scp024BarPayload;
import net.rai.scp_unleashed.scp024.network.Scp024BurstPayload;
import net.rai.scp_unleashed.scp024.network.Scp024ClearPayload;
import net.rai.scp_unleashed.scp024.network.Scp024CuePayload;
import net.rai.scp_unleashed.scp024.network.Scp024DialoguePayload;
import net.rai.scp_unleashed.scp024.network.Scp024QuipPayload;

public final class Scp024ClientHooks {
    private Scp024ClientHooks() {
    }

    public static void dialogue(Scp024DialoguePayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        Client.setScreen(new Scp024DialogueScreen(Payload.Token(), Payload.Pages(), Payload.Cues()));
    }

    public static void quip(Scp024QuipPayload Payload) {
        Scp024QuipOverlay.show(Payload.Text(), Payload.Ticks());
    }

    public static void cue(Scp024CuePayload Payload) {
        Scp024Cue Cue = Scp024Cue.of(Payload.Cue());
        if (Cue == Scp024Cue.MusicLobby) {
            if (Scp024ClientState.inStage(Minecraft.getInstance())) {
                Scp024Music.start();
            }
            return;
        }
        if (Cue == Scp024Cue.MusicFade) {
            Scp024Music.fade();
            return;
        }
        Scp024Crowd.play(Cue);
    }

    public static void bar(Scp024BarPayload Payload) {
        Scp024ClientState.track(Payload.Bar());
    }

    public static void clear(Scp024ClearPayload Payload) {
        Scp024ClientState.clear(Minecraft.getInstance(), Payload.Bars());
    }

    public static void burst(Scp024BurstPayload Payload) {
        Scp024ClientState.burst(Minecraft.getInstance(), Payload.Cannons(), Payload.Focus(), Payload.Strength());
    }
}
