package net.rai.scp_unleashed.speaker.cassette;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.speaker.SpeakerRegistry;

import java.util.List;
import java.util.Optional;

public final class AnomalousLibrary {
    private static List<CassetteRecording> ClientRecordings = List.of();
    private static int ClientVersion;

    private AnomalousLibrary() {
    }

    public static CassetteLibrary of(Player User) {
        return User.getData(SpeakerRegistry.Library);
    }

    public static Optional<CassetteRecording> find(Player User, ResourceLocation Sound) {
        return of(User).find(Sound);
    }

    public static Outcome add(ServerPlayer User, CassetteRecording Recording) {
        CassetteLibrary Current = of(User);
        if (Current.contains(Recording.Sound())) {
            return Outcome.Duplicate;
        }
        if (Current.isFull()) {
            return Outcome.Full;
        }
        User.setData(SpeakerRegistry.Library, Current.with(Recording));
        sync(User);
        return Outcome.Added;
    }

    public static void sync(ServerPlayer User) {
        PacketDistributor.sendToPlayer(User, new CassetteLibraryPayload(of(User)));
    }

    public static void acceptClient(CassetteLibrary Library) {
        ClientRecordings = Library.Recordings();
        ClientVersion++;
    }

    public static void clearClient() {
        ClientRecordings = List.of();
        ClientVersion++;
    }

    public static List<CassetteRecording> client() {
        return ClientRecordings;
    }

    public static boolean clientOwns(ResourceLocation Sound) {
        for (CassetteRecording Recording : ClientRecordings) {
            if (Recording.Sound().equals(Sound)) {
                return true;
            }
        }
        return false;
    }

    public static int clientVersion() {
        return ClientVersion;
    }

    public enum Outcome {
        Added,
        Duplicate,
        Full
    }
}
