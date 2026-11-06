package net.rai.scp_unleashed.cameras.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.CameraMount;

import java.util.Locale;

public final class CameraOverlay {
    public static final ResourceLocation FeedTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/cameras/feed.png");
    public static final ResourceLocation StaticTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/cameras/static.png");
    public static final int FeedSize = 64;
    public static final int StaticSize = 128;
    public static final int NoiseTile = 64;
    public static final int Inset = 10;
    public static final int TextInset = 18;
    public static final int Bracket = 14;
    public static final int BracketU = 0;
    public static final int BracketV = 32;
    public static final int BracketStep = 16;
    public static final int DotU = 16;
    public static final int DotV = 0;
    public static final int DotSize = 7;
    public static final int CrossU = 24;
    public static final int CrossV = 0;
    public static final int CrossSize = 9;
    public static final int TrackU = 0;
    public static final int TrackV = 16;
    public static final int TrackWidth = 61;
    public static final int TrackHeight = 5;
    public static final int MarkU = 0;
    public static final int MarkV = 22;
    public static final int MarkWidth = 3;
    public static final int MarkHeight = 7;
    public static final int Tint = 0x1E0C1A10;
    public static final int Scanline = 0x26000000;
    public static final int ScanEvery = 3;
    public static final int BandColour = 0x0DFFFFFF;
    public static final int BandHeight = 6;
    public static final int[] Vignette = {0x58000000, 0x34000000, 0x1A000000};
    public static final int VignetteStep = 4;
    public static final int RecColour = 0xFFE5483A;
    public static final int Ink = 0xFFD6EAD2;
    public static final int Dim = 0xA8C4DCC0;
    public static final int Hint = 0xB0B8D0B4;
    public static final int BlindColour = 0xFF000000;
    public static final float QuietNoise = 0.11f;
    public static final float LoudNoise = 0.92f;
    public static final int RecPeriod = 20;
    public static final int RecShown = 13;
    public static final int NoSignalAfter = 60;
    public static final int HintGap = 6;
    public static final int CrowdedLift = 11;

    private CameraOverlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Tracker) {
        if (!CameraClient.isActive()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        if (CameraClient.blind()) {
            Graphics.fill(0, 0, Width, Height, BlindColour);
            return;
        }
        int Ticks = CameraClient.ticks();
        boolean Connecting = CameraClient.connecting();
        Graphics.fill(0, 0, Width, Height, Tint);
        noise(Graphics, Width, Height, Ticks, Connecting ? LoudNoise : QuietNoise);
        if (Connecting) {
            Graphics.fill(0, 0, Width, Height, 0x60000000);
        }
        for (int Y = 0; Y < Height; Y += ScanEvery) {
            Graphics.fill(0, Y, Width, Y + 1, Scanline);
        }
        int Band = Math.floorMod(Ticks * 2, Height + 40) - 20;
        Graphics.fill(0, Band, Width, Band + BandHeight, BandColour);
        for (int Step = 0; Step < Vignette.length; Step++) {
            int Edge = Step * VignetteStep;
            int Colour = Vignette[Step];
            Graphics.fill(Edge, Edge, Width - Edge, Edge + VignetteStep, Colour);
            Graphics.fill(Edge, Height - Edge - VignetteStep, Width - Edge, Height - Edge, Colour);
            Graphics.fill(Edge, Edge + VignetteStep, Edge + VignetteStep, Height - Edge - VignetteStep, Colour);
            Graphics.fill(Width - Edge - VignetteStep, Edge + VignetteStep, Width - Edge, Height - Edge - VignetteStep, Colour);
        }
        brackets(Graphics, Width, Height);
        Font Text = Game.font;
        if (Connecting) {
            String Word = CameraClient.sinceSwitch() > NoSignalAfter ? "gui.scp_unleashed.camera_feed.no_signal" : "gui.scp_unleashed.camera_feed.connecting";
            Component Message = Component.translatable(Word);
            Graphics.pose().pushPose();
            Graphics.pose().translate(Width / 2.0f, Height / 2.0f - 8.0f, 0.0f);
            Graphics.pose().scale(2.0f, 2.0f, 1.0f);
            Graphics.drawString(Text, Message, -Text.width(Message) / 2, 0, Ink, true);
            Graphics.pose().popPose();
        } else {
            Graphics.blit(FeedTexture, Width / 2 - CrossSize / 2, Height / 2 - CrossSize / 2, CrossU, CrossV, CrossSize, CrossSize, FeedSize, FeedSize);
        }
        if (Math.floorMod(Ticks, RecPeriod) < RecShown) {
            Graphics.blit(FeedTexture, TextInset, TextInset - 1, DotU, DotV, DotSize, DotSize, FeedSize, FeedSize);
            Graphics.drawString(Text, Component.translatable("gui.scp_unleashed.camera_feed.rec"), TextInset + DotSize + 3, TextInset - 1, RecColour, true);
        }
        String Name = CameraClient.name();
        Graphics.drawString(Text, Component.literal(Name), TextInset, TextInset + 11, Ink, true);
        Component Counter = Component.translatable("gui.scp_unleashed.camera_feed.counter", String.format(Locale.ROOT, "%02d", CameraClient.index() + 1), String.format(Locale.ROOT, "%02d", CameraClient.count()));
        Graphics.drawString(Text, Counter, Width - TextInset - Text.width(Counter), TextInset - 1, Ink, true);
        Component Clock = clock(Game);
        Graphics.drawString(Text, Clock, TextInset, Height - TextInset - 8, Ink, true);
        Component Aim = Component.translatable("gui.scp_unleashed.camera_feed.aim", signed(CameraClient.pan()), signed(CameraClient.tilt()));
        Graphics.drawString(Text, Aim, Width - TextInset - Text.width(Aim), Height - TextInset - 8, Dim, true);
        KeyMapping Leave = CameraClientEvents.LeaveKey.isUnbound() ? Game.options.keyShift : CameraClientEvents.LeaveKey;
        Component Keys = Component.translatable("gui.scp_unleashed.camera_feed.keys",
                CameraClientEvents.PreviousKey.getTranslatedKeyMessage(), CameraClientEvents.NextKey.getTranslatedKeyMessage(), Leave.getTranslatedKeyMessage());
        if (Text.width(Keys) > Width - TextInset * 2) {
            Keys = Component.translatable("gui.scp_unleashed.camera_feed.keys_exit", Leave.getTranslatedKeyMessage());
        }
        int KeysLeft = Width / 2 - Text.width(Keys) / 2;
        boolean Crowded = KeysLeft < TextInset + Text.width(Clock) + HintGap || KeysLeft + Text.width(Keys) > Width - TextInset - Text.width(Aim) - HintGap;
        int Lift = Crowded ? CrowdedLift : 0;
        meter(Graphics, Width, Height - Lift);
        Graphics.drawString(Text, Keys, KeysLeft, Height - TextInset - 8 - Lift, Hint, true);
    }

    private static void noise(GuiGraphics Graphics, int Width, int Height, int Ticks, float Alpha) {
        int Frame = Math.floorMod(Ticks * 7 + (Ticks >> 1) * 3, 4);
        int FrameU = (Frame % 2) * NoiseTile;
        int FrameV = (Frame / 2) * NoiseTile;
        int Shift = Math.floorMod(Ticks * 37, NoiseTile);
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        for (int Y = -Shift; Y < Height; Y += NoiseTile) {
            for (int X = -((Shift * 3) % NoiseTile); X < Width; X += NoiseTile) {
                Graphics.blit(StaticTexture, X, Y, FrameU, FrameV, NoiseTile, NoiseTile, StaticSize, StaticSize);
            }
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void brackets(GuiGraphics Graphics, int Width, int Height) {
        int Right = Width - Inset - Bracket;
        int Bottom = Height - Inset - Bracket;
        Graphics.blit(FeedTexture, Inset, Inset, BracketU, BracketV, Bracket, Bracket, FeedSize, FeedSize);
        Graphics.blit(FeedTexture, Right, Inset, BracketU + BracketStep, BracketV, Bracket, Bracket, FeedSize, FeedSize);
        Graphics.blit(FeedTexture, Inset, Bottom, BracketU + BracketStep * 2, BracketV, Bracket, Bracket, FeedSize, FeedSize);
        Graphics.blit(FeedTexture, Right, Bottom, BracketU + BracketStep * 3, BracketV, Bracket, Bracket, FeedSize, FeedSize);
    }

    private static void meter(GuiGraphics Graphics, int Width, int Height) {
        int Left = Width / 2 - TrackWidth / 2;
        int Top = Height - TextInset - 22;
        Graphics.blit(FeedTexture, Left, Top, TrackU, TrackV, TrackWidth, TrackHeight, FeedSize, FeedSize);
        float Share = (CameraClient.pan() + CameraMount.PanLimit) / (2.0f * CameraMount.PanLimit);
        int Mark = Left + Math.round(Share * (TrackWidth - MarkWidth));
        Graphics.blit(FeedTexture, Mark, Top - 1, MarkU, MarkV, MarkWidth, MarkHeight, FeedSize, FeedSize);
    }

    private static String signed(float Value) {
        int Rounded = Math.round(Value);
        return (Rounded >= 0 ? "+" : "-") + String.format(Locale.ROOT, "%02d", Math.abs(Rounded));
    }

    private static Component clock(Minecraft Game) {
        long Time = Game.level == null ? 0L : Game.level.getDayTime();
        long Day = Time / 24000L + 1L;
        long Within = Math.floorMod(Time, 24000L);
        long Hours = (Within / 1000L + 6L) % 24L;
        long Minutes = (Within % 1000L) * 60L / 1000L;
        long Seconds = ((Within % 1000L) * 3600L / 1000L) % 60L;
        return Component.translatable("gui.scp_unleashed.camera_feed.clock", Day, String.format(Locale.ROOT, "%02d:%02d:%02d", Hours, Minutes, Seconds));
    }
}
