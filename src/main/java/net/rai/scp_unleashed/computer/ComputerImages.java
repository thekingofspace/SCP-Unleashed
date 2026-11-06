package net.rai.scp_unleashed.computer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ComputerImages {
    private static final Map<ResourceLocation, Viewer> Viewers = new ConcurrentHashMap<>();

    private ComputerImages() {
    }

    public static void register(ResourceLocation Picture, Viewer Handler) {
        Viewers.put(Picture, Handler);
    }

    public static boolean isKnown(ResourceLocation Picture) {
        return Viewers.containsKey(Picture);
    }

    public static void viewed(ServerPlayer User, ComputerBlockEntity Computer, @Nullable UploadPortBlockEntity Port, ComputerFile File) {
        Viewer Handler = Viewers.get(File.Document());
        if (Handler != null) {
            Handler.viewed(User, Computer, Port, File);
        }
    }

    @FunctionalInterface
    public interface Viewer {
        void viewed(ServerPlayer User, ComputerBlockEntity Computer, @Nullable UploadPortBlockEntity Port, ComputerFile File);
    }
}
