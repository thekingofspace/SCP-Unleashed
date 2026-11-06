package net.rai.scp_unleashed.terminal.client;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

public final class TerminalAccess {
    private static final Field ConnectStatus = field(ConnectScreen.class, "status");
    private static final Field LoadingListener = field(LevelLoadingScreen.class, "progressListener");
    private static final Field ReceivingReason = field(ReceivingLevelScreen.class, "reason");
    private static final Field ReceivingReady = field(ReceivingLevelScreen.class, "levelReceived");
    private static final Field ProgressHeader = field(ProgressScreen.class, "header");
    private static final Field ProgressStage = field(ProgressScreen.class, "stage");
    private static final Field ProgressValue = field(ProgressScreen.class, "progress");
    private static final Field DisconnectDetails = field(DisconnectedScreen.class, "details");

    private TerminalAccess() {
    }

    private static Field field(Class<?> Owner, String Name) {
        try {
            Field Found = Owner.getDeclaredField(Name);
            Found.setAccessible(true);
            return Found;
        } catch (ReflectiveOperationException | RuntimeException Failure) {
            return null;
        }
    }

    private static Object read(Field Source, Object Target) {
        if (Source == null || Target == null) {
            return null;
        }
        try {
            return Source.get(Target);
        } catch (ReflectiveOperationException | RuntimeException Failure) {
            return null;
        }
    }

    public static Component connectStatus(ConnectScreen Shown) {
        return read(ConnectStatus, Shown) instanceof Component Status ? Status : null;
    }

    public static StoringChunkProgressListener listener(LevelLoadingScreen Shown) {
        return read(LoadingListener, Shown) instanceof StoringChunkProgressListener Listener ? Listener : null;
    }

    public static ReceivingLevelScreen.Reason reason(ReceivingLevelScreen Shown) {
        return read(ReceivingReason, Shown) instanceof ReceivingLevelScreen.Reason Why ? Why : null;
    }

    public static boolean ready(ReceivingLevelScreen Shown) {
        try {
            return read(ReceivingReady, Shown) instanceof BooleanSupplier Check && Check.getAsBoolean();
        } catch (RuntimeException Failure) {
            return false;
        }
    }

    public static boolean progressReadable() {
        return ProgressHeader != null && ProgressStage != null && ProgressValue != null;
    }

    public static Component progressHeader(ProgressScreen Shown) {
        return read(ProgressHeader, Shown) instanceof Component Header ? Header : null;
    }

    public static Component progressStage(ProgressScreen Shown) {
        return read(ProgressStage, Shown) instanceof Component Stage ? Stage : null;
    }

    public static int progressValue(ProgressScreen Shown) {
        return read(ProgressValue, Shown) instanceof Integer Value ? Value : -1;
    }

    public static Component disconnectReason(DisconnectedScreen Shown) {
        return read(DisconnectDetails, Shown) instanceof DisconnectionDetails Details ? Details.reason() : null;
    }
}
