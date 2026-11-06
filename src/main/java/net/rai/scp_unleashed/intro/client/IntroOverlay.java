package net.rai.scp_unleashed.intro.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.arrival.client.ArrivalCard;
import net.rai.scp_unleashed.intro.IntroRegistry;

public final class IntroOverlay {
    public static final ResourceLocation Sheet = IntroRegistry.location("textures/gui/intro/dialogue.png");
    public static final int SheetWidth = 512;
    public static final int SheetHeight = 256;
    public static final int BoxWidth = 300;
    public static final int BoxHeight = 76;
    public static final int BottomMargin = 14;
    public static final int PlateU = 0;
    public static final int PlateV = 80;
    public static final int PlateWidth = 96;
    public static final int PlateHeight = 15;
    public static final int PlateX = 8;
    public static final int PlateY = -12;
    public static final int ArrowU = 100;
    public static final int EndU = 112;
    public static final int MarkV = 80;
    public static final int MarkWidth = 9;
    public static final int MarkHeight = 7;
    public static final int PipU = 124;
    public static final int PipV = 80;
    public static final int PipSize = 5;
    public static final int KeyU = 0;
    public static final int KeyV = 100;
    public static final int KeyWidth = 6;
    public static final int KeyHeight = 14;
    public static final int TextX = 16;
    public static final int TextY = 13;
    public static final int LinePitch = 11;
    public static final int TextColour = 0xFFD6DAE4;
    public static final int PlateColour = 0xFFE2E6EE;
    public static final int HintColour = 0xFFC8CED8;
    public static final int BrassColour = 0xFFC9A04A;
    public static final int BrassDark = 0xFF7A5C26;
    public static final int Panel = 0xFF16181D;
    public static final int PanelEdge = 0xFF3A3F49;
    public static final int TitleColour = 0xFFF2D27A;
    public static final int SubtitleColour = 0xFFB4BCC8;

    private IntroOverlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Tracker) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.options.hideGui && !IntroClient.active()) {
            return;
        }
        float Partial = Tracker.getGameTimeDeltaPartialTick(true);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        Font Shape = Client.font;
        IntroDialogue Talk = IntroClient.dialogue();
        if (Talk != null && Talk.page() >= 0) {
            dialogue(Graphics, Shape, Talk, Width, Height, Partial);
        }
        if (IntroClient.skipHintShown()) {
            skipHint(Graphics, Shape, Width, Partial);
        }
        float Veil = IntroClient.veil(Partial);
        if (Veil > 0.001f) {
            int Alpha = Mth.clamp(Math.round(Veil * 255.0f), 0, 255);
            int Colour = IntroClient.veilWhite() ? 0xF4F6FF : 0x000000;
            Graphics.fill(0, 0, Width, Height, Alpha << 24 | Colour);
        }
        if (IntroClient.titleTicks() >= 0) {
            title(Graphics, Shape, Width, Height, IntroClient.titleTicks() + Partial);
        }
    }

    public static void dialogue(GuiGraphics Graphics, Font Shape, IntroDialogue Talk, int Width, int Height, float Partial) {
        int Left = (Width - BoxWidth) / 2;
        int Top = Height - BoxHeight - BottomMargin;
        float Rise = Mth.clamp((Talk.age() + Partial) / 8.0f, 0.0f, 1.0f);
        Top += Math.round((1.0f - Rise) * 10.0f);
        Graphics.blit(Sheet, Left, Top, 0, 0, BoxWidth, BoxHeight, SheetWidth, SheetHeight);
        Graphics.blit(Sheet, Left + PlateX, Top + PlateY, PlateU, PlateV, PlateWidth, PlateHeight, SheetWidth, SheetHeight);
        Component Plate = Talk.plate();
        Graphics.drawString(Shape, Plate, Left + PlateX + (PlateWidth - Shape.width(Plate)) / 2, Top + PlateY + 4, PlateColour, false);
        int Visible = Talk.visibleChars(Partial);
        for (int Row = 0; Row < Talk.lines().size() && Visible > 0; Row++) {
            String Line = Talk.lines().get(Row);
            String Part = Line.substring(0, Math.min(Line.length(), Visible));
            Graphics.drawString(Shape, Part, Left + TextX, Top + TextY + Row * LinePitch, TextColour, false);
            Visible -= Line.length();
        }
        if (Talk.pages() > 1) {
            int PipsLeft = Left + BoxWidth - 24 - Talk.pages() * (PipSize + 2);
            for (int Index = 0; Index < Talk.pages(); Index++) {
                Graphics.blit(Sheet, PipsLeft + Index * (PipSize + 2), Top + BoxHeight - 12, PipU + (Index == Talk.page() ? 0 : PipSize + 1), PipV, PipSize, PipSize,
                        SheetWidth, SheetHeight);
            }
        }
        if (Talk.pageDone()) {
            int Bob = (Talk.age() / 8) % 2;
            Graphics.blit(Sheet, Left + BoxWidth - 17, Top + BoxHeight - 13 + Bob, Talk.lastPage() ? EndU : ArrowU, MarkV, MarkWidth, MarkHeight, SheetWidth, SheetHeight);
        }
    }

    private static void panel(GuiGraphics Graphics, int X0, int Y0, int X1, int Y1, int Alpha) {
        int A = Alpha << 24;
        Graphics.fill(X0, Y0, X1, Y1, A | (Panel & 0xFFFFFF));
        Graphics.fill(X0, Y0, X1, Y0 + 1, A | (PanelEdge & 0xFFFFFF));
        Graphics.fill(X0, Y1 - 1, X1, Y1, A | (PanelEdge & 0xFFFFFF));
        Graphics.fill(X0, Y0, X0 + 1, Y1, A | (PanelEdge & 0xFFFFFF));
        Graphics.fill(X1 - 1, Y0, X1, Y1, A | (PanelEdge & 0xFFFFFF));
        Graphics.fill(X0 + 1, Y0 + 1, X0 + 2, Y0 + 2, A | (BrassColour & 0xFFFFFF));
        Graphics.fill(X1 - 2, Y0 + 1, X1 - 1, Y0 + 2, A | (BrassColour & 0xFFFFFF));
        Graphics.fill(X0 + 1, Y1 - 2, X0 + 2, Y1 - 1, A | (BrassColour & 0xFFFFFF));
        Graphics.fill(X1 - 2, Y1 - 2, X1 - 1, Y1 - 1, A | (BrassColour & 0xFFFFFF));
    }

    public static void skipHint(GuiGraphics Graphics, Font Shape, int Width, float Partial) {
        float Share = IntroClient.skipShare(Partial);
        float Appear = Mth.clamp((IntroClient.sceneTicks() - IntroClient.SkipHintAfter + Partial) / 20.0f, 0.0f, 1.0f);
        int Alpha = Math.round(Appear * (Share > 0.0f ? 235.0f : 150.0f));
        if (Alpha < 8) {
            return;
        }
        Component Key = IntroClientEvents.SkipKey.getTranslatedKeyMessage();
        Component Text = Component.translatable("intro.scp_unleashed.skip_hint");
        int KeyWide = Shape.width(Key) + 8;
        int TextWide = Shape.width(Text);
        int Inner = KeyWide + 5 + TextWide;
        int X1 = Width - 8;
        int X0 = X1 - Inner - 12;
        int Y0 = 8;
        int Y1 = Y0 + 24;
        panel(Graphics, X0, Y0, X1, Y1, Alpha);
        int A = Alpha << 24;
        int KeyX = X0 + 6;
        Graphics.fill(KeyX, Y0 + 4, KeyX + KeyWide, Y0 + 16, A | 0x2A2E36);
        Graphics.fill(KeyX, Y0 + 15, KeyX + KeyWide, Y0 + 16, A | (BrassDark & 0xFFFFFF));
        Graphics.fill(KeyX, Y0 + 4, KeyX + KeyWide, Y0 + 5, A | 0x4A505C);
        Graphics.drawString(Shape, Key, KeyX + 4, Y0 + 6, A | 0xF0F2F6, false);
        Graphics.drawString(Shape, Text, KeyX + KeyWide + 5, Y0 + 6, A | (HintColour & 0xFFFFFF), false);
        int BarX0 = X0 + 6;
        int BarX1 = X1 - 6;
        Graphics.fill(BarX0, Y0 + 19, BarX1, Y0 + 21, A | 0x2A2E36);
        int Filled = Math.round((BarX1 - BarX0) * Share);
        if (Filled > 0) {
            Graphics.fill(BarX0, Y0 + 19, BarX0 + Filled, Y0 + 21, A | (BrassColour & 0xFFFFFF));
        }
    }

    public static void title(GuiGraphics Graphics, Font Shape, int Width, int Height, float Ticks) {
        ArrivalCard.draw(Graphics, Shape, Width, Height, Ticks, IntroClient.TitleLength, Component.translatable("intro.scp_unleashed.arrival.title"),
                Component.translatable("intro.scp_unleashed.arrival.subtitle"), 1.0f);
    }
}
