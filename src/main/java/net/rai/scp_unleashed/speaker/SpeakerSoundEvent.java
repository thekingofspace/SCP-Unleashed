package net.rai.scp_unleashed.speaker;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public class SpeakerSoundEvent extends Event {
    private final ServerLevel World;
    private final SpeakerBlockEntity Speaker;
    @Nullable
    private final SoundMessages.Request Request;

    public SpeakerSoundEvent(ServerLevel World, SpeakerBlockEntity Speaker, @Nullable SoundMessages.Request Request) {
        this.World = World;
        this.Speaker = Speaker;
        this.Request = Request;
    }

    public static void post(ServerLevel World, SpeakerBlockEntity Speaker, @Nullable SoundMessages.Request Request) {
        NeoForge.EVENT_BUS.post(new SpeakerSoundEvent(World, Speaker, Request));
    }

    public ServerLevel getLevel() {
        return World;
    }

    public SpeakerBlockEntity getSpeaker() {
        return Speaker;
    }

    @Nullable
    public SoundMessages.Request getRequest() {
        return Request;
    }

    public boolean isSilenced() {
        return Request == null;
    }
}
