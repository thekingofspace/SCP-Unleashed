package net.rai.scp_unleashed.notice.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.NoticeStack;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class NoticeHud {
    public static final int Margin = 4;
    public static final int Padding = 4;
    public static final int IconSize = 16;
    public static final int TextLeft = Padding + IconSize + 5;
    public static final int LineHeight = 10;
    public static final int GlyphHeight = 8;
    public static final int MinBoxWidth = 96;
    public static final int MaxBoxWidth = 220;
    public static final int MinUsableWidth = 180;
    public static final int MaxTitleLines = 3;
    public static final int MaxDetailLines = 2;
    public static final int BackgroundAlpha = 200;
    public static final int TitleColour = 0xFFFFFF;
    public static final int DetailColour = 0xB4B4B4;
    public static final int AccentAlpha = 255;
    public static final int BossBarHalf = 91;
    public static final int BossBarHeight = 5;
    public static final int ChatBottom = 40;
    private static final NoticeStack Stack = new NoticeStack();
    private static final Map<NoticeStack.Card, Layout> Layouts = new IdentityHashMap<>();
    private static int BossBottom;

    private NoticeHud() {
    }

    public static void push(Notice Incoming) {
        Stack.push(Incoming);
    }

    public static void reset() {
        Stack.clear();
        Layouts.clear();
        BossBottom = 0;
    }

    public static void beginFrame() {
        BossBottom = 0;
    }

    public static void bossBar(int BarTop) {
        BossBottom = Math.max(BossBottom, BarTop + BossBarHeight);
    }

    public static boolean isHidden(Minecraft Client) {
        return Client.player == null || Client.options.hideGui || Client.getDebugOverlay().showDebugScreen();
    }

    public static boolean isPaused(Minecraft Client) {
        return isHidden(Client) || Client.screen != null && !(Client.screen instanceof ChatScreen);
    }

    public static void tick(Minecraft Client) {
        if (Client.player == null) {
            if (!Stack.isEmpty()) {
                reset();
            }
            return;
        }
        Stack.tick(isPaused(Client));
        for (NoticeStack.Card Fresh : Stack.takeFresh()) {
            Client.getNarrator().say(Fresh.getNotice().Title());
        }
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (isHidden(Client)) {
            return;
        }
        float Scale = (float) (double) ScpClientConfig.NoticeScale.get();
        int Width = Mth.floor(Graphics.guiWidth() / Scale);
        int Height = Mth.floor(Graphics.guiHeight() / Scale);
        int Left = Margin + ScpClientConfig.NoticeOffsetX.get();
        int Top = Margin + ScpClientConfig.NoticeOffsetY.get();
        int BoxLimit = Math.min(MaxBoxWidth, Width - Left - Margin);
        if (BossBottom > 0) {
            int BossLeft = Mth.floor((Graphics.guiWidth() / 2 - BossBarHalf) / Scale);
            if (BossLeft - NoticeStack.Gap * 2 - Left >= MinUsableWidth) {
                BoxLimit = Math.min(BoxLimit, BossLeft - NoticeStack.Gap * 2 - Left);
            } else {
                Top = Math.max(Top, Mth.ceil(BossBottom / Scale) + NoticeStack.Gap + 1);
            }
        }
        BoxLimit = Math.max(MinBoxWidth, BoxLimit);
        int Floor = Height;
        if (Client.options.chatVisibility().get() != ChatVisiblity.HIDDEN) {
            double ChatPixels = Client.gui.getChat().getHeight() * Client.gui.getChat().getScale();
            Floor = Height - Mth.ceil((ChatBottom + ChatPixels) / Scale);
        }
        Font Text = Client.font;
        int Wrap = BoxLimit - TextLeft - Padding;
        List<NoticeStack.Card> Ordered = Stack.layout(Top, Floor, Held -> layoutOf(Text, Held, Wrap).Height());
        Layouts.keySet().removeIf(Held -> !Ordered.contains(Held));
        if (Ordered.isEmpty()) {
            return;
        }
        float Partial = isPaused(Client) ? 0.0f : Delta.getGameTimeDeltaPartialTick(false);
        Graphics.pose().pushPose();
        Graphics.pose().scale(Scale, Scale, 1.0f);
        RenderSystem.enableBlend();
        for (NoticeStack.Card Held : Ordered) {
            draw(Graphics, Text, Held, Layouts.get(Held), Stack.frame(Held, Partial), Left);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        Graphics.pose().popPose();
    }

    private static Layout layoutOf(Font Text, NoticeStack.Card Held, int Wrap) {
        Layout Known = Layouts.get(Held);
        if (Known != null && Known.Wrap() == Wrap) {
            return Known;
        }
        Notice Shown = Held.getNotice();
        List<FormattedCharSequence> TitleLines = limit(Text.split(Shown.Title(), Wrap), MaxTitleLines);
        List<FormattedCharSequence> DetailLines = Shown.Detail().map(Line -> limit(Text.split(Line, Wrap), MaxDetailLines)).orElse(List.of());
        int Widest = 0;
        for (FormattedCharSequence Line : TitleLines) {
            Widest = Math.max(Widest, Text.width(Line));
        }
        for (FormattedCharSequence Line : DetailLines) {
            Widest = Math.max(Widest, Text.width(Line));
        }
        int Lines = TitleLines.size() + DetailLines.size();
        int BoxWidth = Math.max(MinBoxWidth, TextLeft + Widest + Padding - 1);
        int BoxHeight = Math.max(NoticeStack.MinHeight, Lines * LineHeight - (LineHeight - GlyphHeight) + Padding * 2);
        Layout Made = new Layout(Wrap, TitleLines, DetailLines, BoxWidth, BoxHeight);
        Layouts.put(Held, Made);
        return Made;
    }

    private static List<FormattedCharSequence> limit(List<FormattedCharSequence> Lines, int Most) {
        return Lines.size() <= Most ? Lines : List.copyOf(Lines.subList(0, Most));
    }

    private static void draw(GuiGraphics Graphics, Font Text, NoticeStack.Card Held, Layout Box, NoticeStack.Frame Shown, int Left) {
        float Alpha = Shown.Alpha();
        if (Box == null || Alpha <= 0.02f) {
            return;
        }
        int W = Box.Width();
        int H = Box.Height();
        int X = Left - Math.round((W + Left) * (1.0f - Shown.Slide()));
        int Y = Shown.Y();
        int Background = Math.round(BackgroundAlpha * Alpha) << 24;
        Graphics.fill(X + 1, Y, X + W - 1, Y + 1, Background);
        Graphics.fill(X, Y + 1, X + W, Y + H - 1, Background);
        Graphics.fill(X + 1, Y + H - 1, X + W - 1, Y + H, Background);
        Notice Content = Held.getNotice();
        if (Content.Accent() != Notice.NoAccent) {
            Graphics.fill(X + 1, Y + 2, X + 2, Y + H - 2, Math.round(AccentAlpha * Alpha) << 24 | Content.Accent() & 0xFFFFFF);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Alpha);
        Graphics.blit(NoticeIcons.texture(Content.Icon()), X + Padding, Y + (H - IconSize) / 2, 0.0f, 0.0f, IconSize, IconSize, IconSize, IconSize);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int TextAlpha = Math.round(255 * Alpha);
        if (TextAlpha < 8) {
            return;
        }
        int Lines = Box.TitleLines().size() + Box.DetailLines().size();
        int LineY = Y + (H - (Lines * LineHeight - (LineHeight - GlyphHeight))) / 2;
        for (FormattedCharSequence Line : Box.TitleLines()) {
            Graphics.drawString(Text, Line, X + TextLeft, LineY, TextAlpha << 24 | TitleColour, false);
            LineY += LineHeight;
        }
        for (FormattedCharSequence Line : Box.DetailLines()) {
            Graphics.drawString(Text, Line, X + TextLeft, LineY, TextAlpha << 24 | DetailColour, false);
            LineY += LineHeight;
        }
    }

    private record Layout(int Wrap, List<FormattedCharSequence> TitleLines, List<FormattedCharSequence> DetailLines, int Width, int Height) {
    }
}
