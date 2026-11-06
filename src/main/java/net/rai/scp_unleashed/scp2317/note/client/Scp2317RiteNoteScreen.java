package net.rai.scp_unleashed.scp2317.note.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import net.rai.scp_unleashed.scp2317.note.Scp2317RiteNote;

import java.util.ArrayList;
import java.util.List;

public class Scp2317RiteNoteScreen extends Screen {
    public static final ResourceLocation Paper = Scp2317Registry.location("textures/gui/scp2317/rite_note.png");
    public static final int PageWidth = 248;
    public static final int PageHeight = 236;
    public static final int Sheet = 256;
    public static final int TextLeft = 17;
    public static final int TextTop = 20;
    public static final int TextWidth = 214;
    public static final int LineGap = 9;
    public static final int ParagraphGap = 3;
    public static final int ZoneLeft = 166;
    public static final int ZoneTop = 130;
    public static final int ZoneBottom = 206;
    public static final int ZoneGap = 6;
    public static final int LabelCentre = 201;
    public static final int LabelTop = 134;
    public static final int ScreenMargin = 8;
    public static final int Ink = 0xFF3B2B22;
    public static final int WordsInk = 0xFF7E1A1A;
    public static final int SignInk = 0xFF5C3A2C;
    public static final int LabelInk = 0xFFB0301E;
    public static final int OpenTicks = 8;
    public static final int[] Wobble = {0, 1, 0, 0, 1, 2, 1, 0, 1, 0, 0, 2};
    public static final int[] Jitter = {0, -1, 0, 1, 0, -1, 1, 0, -1, 0};

    public record Row(String Text, int X, int Y, int Colour) {
    }

    private final List<Row> Rows = new ArrayList<>();
    private int Age;

    public Scp2317RiteNoteScreen() {
        super(Component.translatable("item.scp_unleashed.scp001_scarlet_rite_note"));
    }

    @Override
    protected void init() {
        super.init();
        Rows.clear();
        Rows.addAll(layout());
    }

    public int room(int Y) {
        boolean Beside = Y + LineGap > ZoneTop && Y < ZoneBottom;
        return Beside ? ZoneLeft - ZoneGap - TextLeft : TextWidth;
    }

    public String fit(String Text, int Room) {
        if (font.width(Text) <= Room) {
            return Text;
        }
        String Cut = font.plainSubstrByWidth(Text, Room);
        if (Cut.isEmpty()) {
            return Text.substring(0, Text.offsetByCodePoints(0, 1));
        }
        if (Text.charAt(Cut.length()) == ' ') {
            return Cut;
        }
        int Space = Cut.lastIndexOf(' ');
        return Space > 0 ? Cut.substring(0, Space) : Cut;
    }

    public List<Row> layout() {
        List<Row> Made = new ArrayList<>();
        int Y = TextTop;
        int Line = 0;
        for (String Key : Scp2317RiteNote.Body) {
            boolean Words = Key.equals(Scp2317RiteNote.WordsKey);
            String Remaining = Component.translatable(Key).getString().trim();
            while (!Remaining.isEmpty()) {
                int Room = room(Y);
                String Piece = fit(Remaining, Room);
                int X = Words ? TextLeft + (Room - font.width(Piece)) / 2 : TextLeft + Wobble[Line % Wobble.length];
                Made.add(new Row(Piece, X, Y, Words ? WordsInk : Ink));
                Remaining = Remaining.substring(Piece.length()).stripLeading();
                Y += LineGap;
                Line++;
            }
            Y += ParagraphGap;
        }
        String Sign = Component.translatable(Scp2317RiteNote.SignKey).getString().trim();
        if (!Sign.isEmpty()) {
            Made.add(new Row(Sign, TextLeft + room(Y) - font.width(Sign), Y, SignInk));
        }
        return Made;
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
        float Fit = Math.min(1.0f, Math.min((float) (height - ScreenMargin) / PageHeight, (float) (width - ScreenMargin) / PageWidth));
        int Lift = Math.round((1.0f - Opening) * 10.0f);
        Graphics.pose().pushPose();
        if (Fit >= 1.0f) {
            Graphics.pose().translate((width - PageWidth) / 2, (height - PageHeight) / 2 + Lift, 0.0f);
        } else {
            Graphics.pose().translate(width / 2.0f, height / 2.0f + Lift, 0.0f);
            Graphics.pose().scale(Fit, Fit, 1.0f);
            Graphics.pose().translate(-PageWidth / 2.0f, -PageHeight / 2.0f, 0.0f);
        }
        Graphics.blit(Paper, 0, 0, 0, 0, PageWidth, PageHeight, Sheet, Sheet);
        for (Row Each : Rows) {
            Graphics.drawString(font, Each.Text(), Each.X(), Each.Y(), Each.Colour(), false);
        }
        drawLabel(Graphics);
        Graphics.pose().popPose();
    }

    private void drawLabel(GuiGraphics Graphics) {
        String Label = Component.translatable(Scp2317RiteNote.LabelKey).getString().trim();
        if (Label.isEmpty()) {
            return;
        }
        int X = LabelCentre - font.width(Label) / 2;
        int Index = 0;
        for (int Offset = 0; Offset < Label.length(); ) {
            int Next = Label.offsetByCodePoints(Offset, 1);
            String Piece = Label.substring(Offset, Next);
            Graphics.drawString(font, Piece, X, LabelTop + Jitter[Index % Jitter.length], LabelInk, false);
            X += font.width(Piece);
            Offset = Next;
            Index++;
        }
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
