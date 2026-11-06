package net.rai.scp_unleashed.terminal.client;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.rai.scp_unleashed.terminal.TerminalHighlight;
import net.rai.scp_unleashed.terminal.TerminalSettings;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

public final class TerminalClient {
    public static final long Started = Util.getMillis();

    private static final Logger Log = LogUtils.getLogger();
    private static final Set<Screen> Reported = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean MouseMode = true;
    private static int LastMouseX = Integer.MIN_VALUE;
    private static int LastMouseY = Integer.MIN_VALUE;

    private TerminalClient() {
    }

    public static boolean calm() {
        Minecraft Game = Minecraft.getInstance();
        return TerminalSettings.reducedMotion() || (Game.options != null && Game.options.panoramaSpeed().get() <= 0.0);
    }

    public static void ghost(GuiGraphics Graphics, AbstractWidget Widget, int MouseX, int MouseY, float PartialTick) {
        Graphics.enableScissor(MouseX, MouseY, MouseX + 1, MouseY + 1);
        Widget.render(Graphics, MouseX, MouseY, PartialTick);
        Graphics.disableScissor();
    }

    public static boolean observe(int MouseX, int MouseY) {
        boolean Moved = LastMouseX != Integer.MIN_VALUE && (MouseX != LastMouseX || MouseY != LastMouseY);
        LastMouseX = MouseX;
        LastMouseY = MouseY;
        MouseMode = TerminalHighlight.mouseMode(MouseMode, Moved, false);
        return MouseMode;
    }

    public static boolean mouseMode() {
        return MouseMode;
    }

    public static boolean navigation(int Key) {
        return Key == GLFW.GLFW_KEY_UP || Key == GLFW.GLFW_KEY_DOWN || Key == GLFW.GLFW_KEY_LEFT || Key == GLFW.GLFW_KEY_RIGHT || Key == GLFW.GLFW_KEY_TAB;
    }

    public static void keyboard(int Key) {
        if (navigation(Key)) {
            MouseMode = TerminalHighlight.mouseMode(MouseMode, false, true);
        }
    }

    public static void fallback(Screen Shown, String Reason) {
        if (Shown != null && Reported.add(Shown)) {
            Log.info("Terminal screens: {} shows the normal screen ({})", Shown.getClass().getName(), Reason);
        }
    }

    public static String key(Component Line) {
        return Line != null && Line.getContents() instanceof TranslatableContents Translated ? Translated.getKey() : "";
    }

    public static String upper(Component Line) {
        return Line == null ? "" : Line.getString().toUpperCase(Locale.ROOT);
    }

    public static String clock(long Ms) {
        long Seconds = Math.max(0L, Ms / 1000L);
        return String.format(Locale.ROOT, "%02d:%02d:%02d", Seconds / 3600L, Seconds / 60L % 60L, Seconds % 60L);
    }

    public static String stamp(long Ms) {
        long Tenths = Math.max(0L, Ms / 100L);
        return String.format(Locale.ROOT, "%02d:%02d.%d", Tenths / 600L % 100L, Tenths / 10L % 60L, Tenths % 10L);
    }
}
