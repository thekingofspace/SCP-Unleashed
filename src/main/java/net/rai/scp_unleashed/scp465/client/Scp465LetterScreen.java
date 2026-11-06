package net.rai.scp_unleashed.scp465.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp465.Scp465LetterItem;
import net.rai.scp_unleashed.scp465.Scp465Registry;

import java.util.List;

public class Scp465LetterScreen extends Screen {
    public static final ResourceLocation Card = Scp465Registry.location("textures/gui/scp465/letter_card.png");
    public static final int Width = 200;
    public static final int Height = 140;
    public static final int Sheet = 256;
    public static final int TextLeft = 22;
    public static final int TextTop = 42;
    public static final int TextWidth = 156;
    public static final int LineGap = 11;
    public static final int Ink = 0x4A2E58;
    public static final int Heading = 0xC8344A;
    public static final int SignInk = 0x6A4A7C;
    public static final int OpenTicks = 8;

    private final int Line;
    private final String Reader;
    private int Age;

    public Scp465LetterScreen(int Line, String Reader) {
        super(Component.translatable("gui.scp_unleashed.scp465_letter.title"));
        this.Line = Line;
        this.Reader = Reader;
    }

    public Component note() {
        String Key = Scp465LetterItem.noteKey(Line);
        return Scp465LetterItem.named(Line) ? Component.translatable(Key, Reader) : Component.translatable(Key);
    }

    @Override
    public void tick() {
        super.tick();
        Age++;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        float Opening = Mth.clamp((Age + Partial) / OpenTicks, 0.0f, 1.0f);
        int Lift = Math.round((1.0f - Opening) * 12.0f);
        int Left = (width - Width) / 2;
        int Top = (height - Height) / 2 + Lift;
        Graphics.blit(Card, Left, Top, 0, 0, Width, Height, Sheet, Sheet);
        Component Title = Component.translatable("gui.scp_unleashed.scp465_letter.heading");
        Graphics.drawString(font, Title, Left + (Width - font.width(Title)) / 2, Top + 24, Heading, false);
        List<FormattedCharSequence> Lines = font.split(note(), TextWidth);
        int Y = Top + TextTop + Math.max(0, (4 - Lines.size()) * LineGap / 2);
        for (FormattedCharSequence Row : Lines) {
            Graphics.drawString(font, Row, Left + (Width - font.width(Row)) / 2, Y, Ink, false);
            Y += LineGap;
        }
        Component Sign = Component.translatable("gui.scp_unleashed.scp465_letter.sign");
        Graphics.drawString(font, Sign, Left + Width - 22 - font.width(Sign), Top + Height - 26, SignInk, false);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Age > OpenTicks) {
            onClose();
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }
}
