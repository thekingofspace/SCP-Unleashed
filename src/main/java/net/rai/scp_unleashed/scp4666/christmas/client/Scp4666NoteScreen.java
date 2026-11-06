package net.rai.scp_unleashed.scp4666.christmas.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666NoteItem;

public class Scp4666NoteScreen extends Screen {
    public static final ResourceLocation Paper = Scp4666Registry.location("textures/gui/scp4666/saint_note.png");
    public static final int PaperWidth = 128;
    public static final int PaperHeight = 80;
    public static final int Ink = 0xFF4A2A24;
    public static final int SignatureInk = 0xFF7A2A2A;
    public static final int TextY = 26;
    public static final int SignatureY = 46;
    public static final int Margin = 14;

    public Scp4666NoteScreen() {
        super(Component.translatable("gui.scp_unleashed.scp4666_saint_note"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        onClose();
        return true;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        int Left = (width - PaperWidth) / 2;
        int Top = (height - PaperHeight) / 2;
        Graphics.blit(Paper, Left, Top, 0, 0, PaperWidth, PaperHeight, PaperWidth, PaperHeight);
        Component Text = Component.translatable(Scp4666NoteItem.TextKey);
        Component Signed = Component.translatable(Scp4666NoteItem.SignedKey);
        Graphics.drawString(font, Text, Left + (PaperWidth - font.width(Text)) / 2, Top + TextY, Ink, false);
        Graphics.drawString(font, Signed, Left + PaperWidth - Margin - font.width(Signed), Top + SignatureY, SignatureInk, false);
    }
}
