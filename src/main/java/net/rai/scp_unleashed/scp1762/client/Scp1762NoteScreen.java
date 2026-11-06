package net.rai.scp_unleashed.scp1762.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp1762.Scp1762NoteItem;
import net.rai.scp_unleashed.scp1762.Scp1762Registry;

import java.util.ArrayList;
import java.util.List;

public class Scp1762NoteScreen extends Screen {
    public static final ResourceLocation Paper = Scp1762Registry.location("textures/gui/scp1762/note.png");
    public static final int Width = 196;
    public static final int Height = 148;
    public static final int Sheet = 256;
    public static final int TextLeft = 18;
    public static final int TextTop = 20;
    public static final int TextWidth = 160;
    public static final int LineGap = 10;
    public static final int Ink = 0x3A3428;
    public static final int SignInk = 0x5A5040;
    public static final int OpenTicks = 8;

    private final int Note;
    private int Age;

    public Scp1762NoteScreen(int Note) {
        super(Component.translatable(Scp1762NoteItem.Prefix + "." + Note));
        this.Note = Note;
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
        int Lift = Math.round((1.0f - Opening) * 10.0f);
        int Left = (width - Width) / 2;
        int Top = (height - Height) / 2 + Lift;
        Graphics.blit(Paper, Left, Top, 0, 0, Width, Height, Sheet, Sheet);
        List<FormattedCharSequence> Rows = new ArrayList<>();
        for (int Row = 0; Row < Scp1762NoteItem.rows(Note); Row++) {
            Rows.addAll(font.split(Component.translatable(Scp1762NoteItem.key(Note, Row)), TextWidth));
        }
        int Y = Top + TextTop + Math.max(0, (9 - Rows.size()) * LineGap / 2);
        for (FormattedCharSequence Row : Rows) {
            Graphics.drawString(font, Row, Left + TextLeft, Y, Ink, false);
            Y += LineGap;
        }
        Component Sign = Component.translatable(Scp1762NoteItem.sign(Note));
        Graphics.drawString(font, Sign, Left + Width - 20 - font.width(Sign), Top + Height - 24, SignInk, false);
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
