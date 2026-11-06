package net.rai.scp_unleashed.worldend.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class WorldEndScreen extends Screen {
    public static final int Hold = 160;
    public static final int Background = 0xFF0B0305;
    public static final String TitleKey = "screen.scp_unleashed.worldend.title";
    public static final String LineKey = "screen.scp_unleashed.worldend.line";
    private final Component Reason;
    private int Ticks;

    public WorldEndScreen(Component Reason) {
        super(Component.translatable(TitleKey));
        this.Reason = Reason;
    }

    @Override
    public void tick() {
        Ticks++;
        if (Ticks >= Hold && minecraft != null && minecraft.level == null) {
            minecraft.setScreen(new TitleScreen());
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return Ticks > 40;
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.level == null) {
            minecraft.setScreen(new TitleScreen());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        Graphics.fill(0, 0, width, height, Background);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        renderBackground(Graphics, MouseX, MouseY, Partial);
        float Fade = Mth.clamp((Ticks + Partial - 20.0f) / 40.0f, 0.0f, 1.0f);
        int Alpha = Math.max(4, (int) (Fade * 255.0f));
        int Middle = height / 2;
        Graphics.drawCenteredString(font, getTitle(), width / 2, Middle - 16, (Alpha << 24) | 0x8E1F1A);
        if (!Reason.getString().isEmpty()) {
            Graphics.drawCenteredString(font, Reason, width / 2, Middle + 2, (Alpha << 24) | 0x6A4A44);
        }
        float Late = Mth.clamp((Ticks + Partial - 70.0f) / 40.0f, 0.0f, 1.0f);
        if (Late > 0.0f) {
            Graphics.drawCenteredString(font, Component.translatable(LineKey), width / 2, Middle + 20, (Math.max(4, (int) (Late * 200.0f)) << 24) | 0x4A3430);
        }
    }
}
