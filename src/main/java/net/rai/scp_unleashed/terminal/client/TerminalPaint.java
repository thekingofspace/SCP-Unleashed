package net.rai.scp_unleashed.terminal.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.terminal.TerminalMotd;
import net.rai.scp_unleashed.terminal.TerminalPalette;
import net.rai.scp_unleashed.terminal.TerminalSettings;
import net.rai.scp_unleashed.terminal.TerminalTags;

import java.util.ArrayList;
import java.util.List;

public final class TerminalPaint {
    public static final ResourceLocation FontId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "terminal");
    public static final Style Mono = Style.EMPTY.withFont(FontId);
    public static final int CharW = 6;
    public static final int LineH = 10;
    public static final int RowH = 12;
    public static final int HeaderGap = 20;
    public static final int MaxContentW = 760;
    public static final int MinMenuW = 132;
    public static final int PanelGap = 16;
    public static final int MinPanelW = 150;
    public static final int MaxLogoScale = 5;
    public static final int GlowAlpha = 0x22;
    public static final int ScanAlpha = 0x1C;
    public static final int BandAlpha = 0x0A;
    public static final int BandH = 12;
    public static final int FlickerMs = 90;
    public static final int CursorMs = 530;
    public static final String Logo = "SCP UNLEASHED";
    public static final String Block = "█";

    public final GuiGraphics Graphics;
    public final Font Text;
    public final TerminalPalette Colours;
    public final int Width;
    public final int Height;
    public final long Now;
    public final boolean Calm;
    public final int Pad;
    public final int ContentX;
    public final int ContentW;

    public TerminalPaint(GuiGraphics NewGraphics, Font NewText, int NewWidth, int NewHeight, long NewNow) {
        Graphics = NewGraphics;
        Text = NewText;
        Colours = TerminalSettings.palette();
        Width = NewWidth;
        Height = NewHeight;
        Now = NewNow;
        Calm = TerminalClient.calm();
        Pad = pad(NewWidth);
        ContentW = contentW(NewWidth);
        ContentX = (NewWidth - ContentW) / 2;
    }

    public static int pad(int ScreenW) {
        return ScreenW >= 400 ? 10 : 6;
    }

    public static int contentW(int ScreenW) {
        return Math.max(0, Math.min(ScreenW - pad(ScreenW) * 2, MaxContentW));
    }

    public static int logoScale(int ContentWidth, int ScreenH) {
        int ByWidth = ContentWidth * 3 / 4 / (14 * CharW);
        int ByHeight = (ScreenH - 190) / 8;
        return Math.max(2, Math.min(MaxLogoScale, Math.min(ByWidth, ByHeight)));
    }

    public static MutableComponent mono(String Line) {
        return Component.literal(Line).withStyle(Mono);
    }

    public static MutableComponent mono(Component Line) {
        return Line.copy().withStyle(Mono);
    }

    public int right() {
        return ContentX + ContentW;
    }

    public int width(String Line) {
        return Text.width(mono(Line));
    }

    public int width(Component Line) {
        return Text.width(mono(Line));
    }

    public int columns(int Span) {
        return Math.max(0, Span / CharW);
    }

    public int text(String Line, int X, int Y, int Colour) {
        return Graphics.drawString(Text, mono(Line), X, Y, Colour, false);
    }

    public int text(Component Line, int X, int Y, int Colour) {
        return Graphics.drawString(Text, mono(Line), X, Y, Colour, false);
    }

    public int glow(String Line, int X, int Y, int Colour) {
        return glow(mono(Line), X, Y, Colour);
    }

    public int glow(Component Line, int X, int Y, int Colour) {
        Component Styled = mono(Line);
        int Halo = TerminalPalette.alpha(Colour, GlowAlpha);
        Graphics.drawString(Text, Styled, X - 1, Y, Halo, false);
        Graphics.drawString(Text, Styled, X + 1, Y, Halo, false);
        Graphics.drawString(Text, Styled, X, Y - 1, Halo, false);
        Graphics.drawString(Text, Styled, X, Y + 1, Halo, false);
        return Graphics.drawString(Text, Styled, X, Y, Colour, false);
    }

    public int clipped(String Line, int X, int Y, int Colour, int MaxColumns) {
        return text(fit(Line, MaxColumns), X, Y, Colour);
    }

    public static String fit(String Line, int MaxColumns) {
        if (MaxColumns <= 0) {
            return "";
        }
        if (Line.length() <= MaxColumns) {
            return Line;
        }
        return MaxColumns <= 3 ? Line.substring(0, MaxColumns) : Line.substring(0, MaxColumns - 3) + "...";
    }

    public List<FormattedCharSequence> wrap(Component Line, int Span) {
        return Text.split(mono(Line), Math.max(CharW, Span));
    }

    public int tag(String Label, int X, int Y, int Colour) {
        text("[", X, Y, Colour);
        text("]", X + TerminalTags.closeOffset(CharW), Y, Colour);
        if (!Label.isEmpty()) {
            text(Label, X + TerminalTags.labelOffset(width(Label), CharW), Y, Colour);
        }
        return X + TerminalTags.textOffset(CharW);
    }

    public int spinner(int Step, int X, int Y, int Colour) {
        text(TerminalTags.spinner(Step), X, Y, Colour);
        return X + TerminalTags.textOffset(CharW);
    }

    public void fill(int X0, int Y0, int X1, int Y1, int Colour) {
        Graphics.fill(X0, Y0, X1, Y1, Colour);
    }

    public void background() {
        Graphics.fill(0, 0, Width, Height, Colours.Bg());
    }

    public boolean blink() {
        return Calm || (Now / CursorMs) % 2L == 0L;
    }

    public void cursor(int X, int Y, int Colour) {
        if (blink()) {
            text(Block, X, Y, Colour);
        }
    }

    public void crt() {
        for (int Y = 1; Y < Height - 1; Y += 2) {
            Graphics.fill(0, Y, Width, Y + 1, ScanAlpha << 24);
        }
        if (Calm) {
            return;
        }
        int Band = (int) ((Now * 4L / 100L) % (Height + BandH * 3L)) - BandH;
        Graphics.fill(0, Math.max(1, Band), Width, Math.min(Height - 1, Band + BandH), TerminalPalette.alpha(Colours.Bright(), BandAlpha));
        long Bucket = Now / FlickerMs;
        int Flicker = Math.floorMod(TerminalMotd.mix(Bucket), 11L) == 0L ? 0x0C : (int) Math.floorMod(TerminalMotd.mix(Bucket + 7L), 4L);
        if (Flicker > 0) {
            Graphics.fill(0, 0, Width, Height, Flicker << 24);
        }
    }

    public void dashes(int X, int Y, int Span, int Colour) {
        for (int At = X; At < X + Span; At += CharW) {
            Graphics.fill(At, Y, Math.min(At + CharW - 1, X + Span), Y + 1, Colour);
        }
    }

    public void header(Component Left, Component Right) {
        int Y = Pad;
        text(Left, ContentX, Y, Colours.Main());
        text(Right, right() - width(Right), Y, Colours.Dim());
        dashes(ContentX, Y + 11, ContentW, Colours.Dim());
    }

    public void panel(int X0, int Y0, int X1, int Y1, Component Title) {
        Graphics.fill(X0, Y0, X1, Y1, Colours.Panel());
        Graphics.fill(X0, Y0, X1, Y0 + 1, Colours.Dim());
        Graphics.fill(X0, Y1 - 1, X1, Y1, Colours.Dim());
        Graphics.fill(X0, Y0, X0 + 1, Y1, Colours.Dim());
        Graphics.fill(X1 - 1, Y0, X1, Y1, Colours.Dim());
        if (Title != null) {
            int TitleW = width(Title);
            Graphics.fill(X0 + 5, Y0 - 4, X0 + 9 + TitleW, Y0 + 5, Colours.Bg());
            text(Title, X0 + 7, Y0 - 3, Colours.Main());
        }
    }

    public void logo(int X, int Y, int Scale, int Shown, boolean Cursor) {
        String Word = Logo.substring(0, Math.max(0, Math.min(Logo.length(), Shown)));
        Graphics.pose().pushPose();
        Graphics.pose().translate(X, Y, 0.0f);
        Graphics.pose().scale(Scale, Scale, 1.0f);
        Graphics.drawString(Text, mono(Word), 1, 1, Colours.Faint(), false);
        int Split = Math.min(3, Word.length());
        Graphics.drawString(Text, mono(Word.substring(0, Split)), 0, 0, Colours.Bright(), false);
        if (Word.length() > Split) {
            Graphics.drawString(Text, mono(Word.substring(Split)), Split * CharW, 0, Colours.Main(), false);
        }
        if (Cursor && blink()) {
            Graphics.drawString(Text, mono(Block), Word.length() * CharW, 0, Colours.Main(), false);
        }
        Graphics.pose().popPose();
        if (Scale >= 3) {
            int Span = (Word.length() + 1) * CharW * Scale + Scale;
            for (int Row = 0; Row < 9; Row++) {
                Graphics.fill(X, Y + Row * Scale + Scale - 1, X + Span, Y + Row * Scale + Scale, 0x50000000);
            }
        }
    }

    public void row(int X, int Y, int Span, Component Label, boolean Hover, boolean Disabled, Component Suffix, int SuffixColour) {
        if (Hover && !Disabled) {
            Graphics.fill(X, Y, X + Span, Y + RowH - 1, Colours.Main());
            int End = text(Component.literal("> ").append(Label), X + 3, Y + 2, Colours.Bg());
            if (Suffix != null) {
                End = text(Suffix, End, Y + 2, Colours.Bg());
            }
            if (blink()) {
                text("_", End, Y + 2, Colours.Bg());
            }
            return;
        }
        int Colour = Disabled ? Colours.Dim() : (Hover ? Colours.Bright() : Colours.Main());
        int End = text(Label, X + 3 + 2 * CharW, Y + 2, Colour);
        if (Suffix != null) {
            text(Suffix, End, Y + 2, SuffixColour);
        }
    }

    public int bracket(Component Label, int X, int Y, boolean Hover) {
        Component Shown = Component.literal("[ ").append(Label).append(" ]");
        int Span = width(Shown);
        if (Hover) {
            Graphics.fill(X - 1, Y - 2, X + Span + 1, Y + 9, Colours.Main());
            text(Shown, X, Y, Colours.Bg());
        } else {
            text(Shown, X, Y, Colours.Main());
        }
        return Span;
    }

    public void inverse(String Label, int X, int Y, int Colour, int Ink) {
        int Span = width(Label);
        Graphics.fill(X - 1, Y - 1, X + Span + 1, Y + 8, Colour);
        text(Label, X, Y, Ink);
    }

    public static List<String> words(String Line, int MaxColumns) {
        List<String> Lines = new ArrayList<>();
        StringBuilder Current = new StringBuilder();
        for (String Word : Line.split(" ")) {
            if (Current.length() > 0 && Current.length() + 1 + Word.length() > MaxColumns) {
                Lines.add(Current.toString());
                Current.setLength(0);
            }
            if (Current.length() > 0) {
                Current.append(' ');
            }
            Current.append(Word);
        }
        if (Current.length() > 0) {
            Lines.add(Current.toString());
        }
        return Lines;
    }
}
