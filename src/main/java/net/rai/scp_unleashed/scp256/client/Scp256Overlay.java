package net.rai.scp_unleashed.scp256.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp256.Scp256Registry;
import net.rai.scp_unleashed.scp256.Scp256Schedule;
import net.rai.scp_unleashed.scp256.Scp256TypewriterBlockEntity;
import net.rai.scp_unleashed.scp256.Scp256Typewriters;

import java.util.ArrayList;
import java.util.List;

public final class Scp256Overlay {
    public static final ResourceLocation Slip = Scp256Registry.location("textures/gui/scp256/slip.png");
    public static final int SlipTextureWidth = 256;
    public static final int SlipTextureHeight = 64;
    public static final double Reach = 8.0;
    public static final int Hold = 70;
    public static final int Fade = 20;
    public static final int Rise = 5;
    public static final int MaxWidth = 200;
    public static final int Padding = 8;
    public static final int TopPadding = 6;
    public static final int LineHeight = 10;
    public static final int MaxLines = 5;
    public static final int BottomGap = 74;
    public static final int ShadowOffset = 2;
    public static final int Ink = 0x2A2522;
    public static final int InkWorn = 0x4E4741;
    public static final int Highlight = 0xF6F2E6;
    public static final int Shade = 0xC9BE9E;
    public static final int Blink = 16;
    private static String CachedText = "";
    private static List<String> CachedLines = List.of();

    private Scp256Overlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.level == null || Client.options.hideGui) {
            return;
        }
        long Now = Client.level.getGameTime();
        Scp256TypewriterBlockEntity Typewriter = Scp256Typewriters.nearest(Client.player.getEyePosition(), Reach, Now, Hold + Fade);
        if (Typewriter == null) {
            return;
        }
        Scp256Schedule Plan = Typewriter.schedule();
        if (Plan == null || Plan.strokes() == 0) {
            return;
        }
        double Elapsed = Typewriter.elapsed(Client.level, Delta.getGameTimeDeltaPartialTick(false));
        float Alpha = 1.0f - Mth.clamp((float) (Elapsed - Typewriter.getDuration() - Hold) / Fade, 0.0f, 1.0f);
        if (Alpha <= 0.02f) {
            return;
        }
        Font Text = Client.font;
        List<String> Lines = lines(Text, Plan.getText());
        int Widest = 0;
        for (String Line : Lines) {
            Widest = Math.max(Widest, Text.width(Line));
        }
        int Width = Widest + Padding * 2;
        int Height = Lines.size() * LineHeight + TopPadding + 4;
        int Lift = Math.round((1.0f - Scp256Sheet.ease((float) (Elapsed / Rise))) * 6.0f);
        int Left = (Graphics.guiWidth() - Width) / 2;
        int Top = Graphics.guiHeight() - BottomGap - Height + Lift;
        int Typed = Plan.typedAt(Elapsed);
        int Shown = Plan.typedText(Typed).length();
        RenderSystem.enableBlend();
        paper(Graphics, Left, Top, Width, Height, Alpha);
        int Remaining = Shown;
        int CursorX = Left + Padding;
        int CursorY = Top + TopPadding;
        int Seed = Typewriter.getSeed();
        int Used = 0;
        for (int Row = 0; Row < Lines.size() && Remaining > 0; Row++) {
            String Line = Lines.get(Row);
            String Part = Line.substring(0, Math.min(Line.length(), Remaining));
            int X = Left + Padding;
            int Y = Top + TopPadding + Row * LineHeight;
            for (int Index = 0; Index < Part.length(); Index++) {
                String Glyph = String.valueOf(Part.charAt(Index));
                int Colour = Scp256Schedule.mix(Seed, Used + Index) % 5 == 0 ? InkWorn : Ink;
                Graphics.drawString(Text, Glyph, X, Y, colour(Colour, Alpha), false);
                X += Text.width(Glyph);
            }
            Used += Line.length() + 1;
            CursorX = X;
            CursorY = Y;
            Remaining -= Line.length() + 1;
        }
        boolean Typing = Typed < Plan.strokes();
        if (Typing && (int) (Elapsed / (Blink / 2)) % 2 == 0) {
            Graphics.drawString(Text, "_", CursorX, CursorY, colour(InkWorn, Alpha), false);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static int colour(int Rgb, float Alpha) {
        int Value = Math.max(5, Math.round(Alpha * 255.0f));
        return (Value << 24) | (Rgb & 0xFFFFFF);
    }

    private static void paper(GuiGraphics Graphics, int Left, int Top, int Width, int Height, float Alpha) {
        Graphics.fill(Left + ShadowOffset, Top + ShadowOffset, Left + Width + ShadowOffset, Top + Height + ShadowOffset, colour(0x000000, Alpha * 0.28f));
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Alpha);
        int Across = Math.min(Width, SlipTextureWidth);
        int Down = Math.min(Height, SlipTextureHeight);
        Graphics.blit(Slip, Left + 1, Top, 1, 0, Across - 2, 1, SlipTextureWidth, SlipTextureHeight);
        Graphics.blit(Slip, Left, Top + 1, 0, 1, Across, Down - 2, SlipTextureWidth, SlipTextureHeight);
        Graphics.blit(Slip, Left + 1, Top + Height - 1, 1, Down - 1, Across - 2, 1, SlipTextureWidth, SlipTextureHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        Graphics.fill(Left + 1, Top, Left + Width - 1, Top + 1, colour(Highlight, Alpha * 0.8f));
        Graphics.fill(Left + 1, Top + Height - 1, Left + Width - 1, Top + Height, colour(Shade, Alpha));
        Graphics.fill(Left + Width - 1, Top + 1, Left + Width, Top + Height - 1, colour(Shade, Alpha * 0.7f));
    }

    public static List<String> lines(Font Text, String Message) {
        if (Message.equals(CachedText)) {
            return CachedLines;
        }
        List<String> Result = new ArrayList<>();
        StringBuilder Current = new StringBuilder();
        for (String Word : Message.split(" ")) {
            String Trial = Current.isEmpty() ? Word : Current + " " + Word;
            if (Current.isEmpty() || Text.width(Trial) <= MaxWidth) {
                Current = new StringBuilder(Trial);
            } else {
                Result.add(Current.toString());
                Current = new StringBuilder(Word);
            }
        }
        if (!Current.isEmpty()) {
            Result.add(Current.toString());
        }
        CachedText = Message;
        CachedLines = Result.size() > MaxLines ? Result.subList(0, MaxLines) : Result;
        return CachedLines;
    }
}
