package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class Scp902ClientGate {
    public static final int DebugKey = 292;
    public static final String PunchyId = "punchy";
    public static final String PunchyRenderer = "punchy.client.render.PunchyArmRenderer";
    public static final String PunchyHide = "setFirstPersonHidden";
    private static final Logger Log = LogUtils.getLogger();
    private static boolean Engaged;
    @Nullable
    private static CameraType Previous;
    private static boolean DebugWasOpen;
    private static boolean PunchyChecked;
    @Nullable
    private static Method PunchyToggle;

    private Scp902ClientGate() {
    }

    public static boolean engaged() {
        return Engaged;
    }

    public static void tick(Minecraft Client) {
        boolean Wanted = Scp902ClientScene.cinematic() && Client.player != null;
        if (Wanted && !Engaged) {
            engage(Client);
        }
        if (Wanted) {
            hold(Client);
        } else if (Engaged) {
            release(Client);
        }
    }

    private static void engage(Minecraft Client) {
        Engaged = true;
        Previous = Client.options.getCameraType();
        DebugWasOpen = Client.getDebugOverlay().showDebugScreen();
    }

    private static void hold(Minecraft Client) {
        if (Client.options.getCameraType() != CameraType.FIRST_PERSON) {
            Client.options.setCameraType(CameraType.FIRST_PERSON);
            Client.gameRenderer.checkEntityPostEffect(Client.getCameraEntity());
        }
        if (Client.getDebugOverlay().showDebugScreen()) {
            Client.getDebugOverlay().toggleOverlay();
        }
        Client.gameRenderer.setRenderHand(false);
        punchy(true);
        drain(Client.options);
        if (Client.screen instanceof ChatScreen) {
            Client.setScreen(null);
        }
    }

    public static void release(Minecraft Client) {
        if (!Engaged) {
            return;
        }
        Engaged = false;
        if (Previous != null && Previous != Client.options.getCameraType()) {
            Client.options.setCameraType(Previous);
            Client.gameRenderer.checkEntityPostEffect(Previous.isFirstPerson() ? Client.getCameraEntity() : null);
        }
        Previous = null;
        if (DebugWasOpen && !Client.getDebugOverlay().showDebugScreen()) {
            Client.getDebugOverlay().toggleOverlay();
        }
        DebugWasOpen = false;
        Client.gameRenderer.setRenderHand(true);
        punchy(false);
    }

    public static void drain(Options Settings) {
        while (Settings.keyTogglePerspective.consumeClick()) {
        }
        while (Settings.keyChat.consumeClick()) {
        }
        while (Settings.keyCommand.consumeClick()) {
        }
    }

    public static boolean blocksKey(long Window, int Key, int Scan, int Action) {
        if (!Engaged) {
            return false;
        }
        if (Key == DebugKey) {
            return true;
        }
        if (Action == 0) {
            return false;
        }
        if (InputConstants.isKeyDown(Window, DebugKey)) {
            return true;
        }
        Minecraft Client = Minecraft.getInstance();
        if (Client.screen != null) {
            return false;
        }
        Options Settings = Client.options;
        return Settings.keyTogglePerspective.matches(Key, Scan) || Settings.keyChat.matches(Key, Scan) || Settings.keyCommand.matches(Key, Scan);
    }

    private static void punchy(boolean Hidden) {
        if (!PunchyChecked) {
            PunchyChecked = true;
            if (ModList.get().isLoaded(PunchyId)) {
                try {
                    PunchyToggle = Class.forName(PunchyRenderer).getMethod(PunchyHide, boolean.class);
                } catch (ReflectiveOperationException | LinkageError Problem) {
                    Log.warn("SCP-902 could not reach Punchy's first-person hide switch, relying on the skipped hand pass", Problem);
                }
            }
        }
        if (PunchyToggle == null) {
            return;
        }
        try {
            PunchyToggle.invoke(null, Hidden);
        } catch (ReflectiveOperationException | RuntimeException Problem) {
            Log.warn("SCP-902 could not toggle Punchy's first-person arm", Problem);
            PunchyToggle = null;
        }
    }
}
