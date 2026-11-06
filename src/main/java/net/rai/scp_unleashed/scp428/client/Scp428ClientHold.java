package net.rai.scp_unleashed.scp428.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp428.Scp428Hold;
import net.rai.scp_unleashed.scp428.Scp428Registry;
import net.rai.scp_unleashed.scp428.network.Scp428StrugglePayload;
import org.lwjgl.glfw.GLFW;

public final class Scp428ClientHold {
    public static final ResourceLocation LayerId = Scp428Registry.location("scp428_struggle");
    public static final ResourceLocation Texture = Scp428Registry.location("textures/gui/scp428/struggle.png");
    public static final int TextureWidth = 32;
    public static final int TextureHeight = 16;
    public static final int PipWidth = 5;
    public static final int PipHeight = 7;
    public static final int PipStep = 6;
    public static final int CapWidth = 3;
    public static final int BarHeight = 11;
    public static final int IconSize = 9;
    public static final int Below = 24;
    public static final int LockedPips = 12;
    public static final double AimShare = 0.3;
    public static final int TextColour = 0xF2C9C2;
    public static final int LockedColour = 0xB49A9C;

    private static int Holders;
    private static int Presses;
    private static int Required;

    private Scp428ClientHold() {
    }

    public static void update(int NewHolders, int NewPresses, int NewRequired) {
        Holders = NewHolders;
        Presses = NewPresses;
        Required = NewRequired;
    }

    public static void reset() {
        update(0, 0, 0);
    }

    public static boolean held() {
        return Holders > 0;
    }

    public static boolean locked() {
        return held() && Required == Scp428Hold.Locked;
    }

    public static void press(int Action, boolean Matches) {
        Minecraft Game = Minecraft.getInstance();
        if (!held() || !Matches || Action != GLFW.GLFW_PRESS || Game.screen != null || Game.player == null) {
            return;
        }
        PacketDistributor.sendToServer(Scp428StrugglePayload.Instance);
    }

    public static double slowed(double Sensitivity) {
        if (!held()) {
            return Sensitivity;
        }
        double Speed = Sensitivity * 0.6 + 0.2;
        return (Speed * Math.cbrt(AimShare) - 0.2) / 0.6;
    }

    public static void tick(Minecraft Game) {
        if (held() && (Game.player == null || !Game.player.isAlive())) {
            reset();
        }
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        if (!held() || Game.options.hideGui || Game.player == null) {
            return;
        }
        boolean Locked = locked();
        int Count = Locked ? LockedPips : Math.max(1, Required);
        int Inner = Count * PipStep - 1;
        int BarWidth = CapWidth * 2 + Inner + 2;
        int Total = IconSize + 2 + BarWidth;
        int Left = Graphics.guiWidth() / 2 - Total / 2;
        int Top = Graphics.guiHeight() / 2 + Below;
        RenderSystem.enableBlend();
        Graphics.blit(Texture, Left, Top + 1, 22, 0, IconSize, IconSize, TextureWidth, TextureHeight);
        int BarLeft = Left + IconSize + 2;
        Graphics.blit(Texture, BarLeft, Top, 15, 0, CapWidth, BarHeight, TextureWidth, TextureHeight);
        for (int Column = 0; Column < Inner + 2; Column++) {
            Graphics.blit(Texture, BarLeft + CapWidth + Column, Top, 18, 0, 1, BarHeight, TextureWidth, TextureHeight);
        }
        Graphics.blit(Texture, BarLeft + CapWidth + Inner + 2, Top, 19, 0, CapWidth, BarHeight, TextureWidth, TextureHeight);
        for (int Pip = 0; Pip < Count; Pip++) {
            int U = Locked ? 10 : Pip < Presses ? 5 : 0;
            Graphics.blit(Texture, BarLeft + CapWidth + 1 + Pip * PipStep, Top + 2, U, 0, PipWidth, PipHeight, TextureWidth, TextureHeight);
        }
        RenderSystem.disableBlend();
        Font Text = Game.font;
        Component Line = Locked ? Component.translatable("gui.scp_unleashed.scp428.locked")
                : Component.translatable("gui.scp_unleashed.scp428.struggle", Game.options.keyShift.getTranslatedKeyMessage());
        Graphics.drawString(Text, Line, Graphics.guiWidth() / 2 - Text.width(Line) / 2, Top + BarHeight + 3, Locked ? LockedColour : TextColour, true);
    }
}
