package net.rai.scp_unleashed.scp001day.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.Scp001DayWorld;

public final class Scp001DayOverlay {
    public static final ResourceLocation LayerId = Scp001DayRegistry.location("scp001_day_heat");
    public static final int Pixel = 8;
    public static final int Bands = 5;
    public static final int Drips = 24;
    public static final String[] Glyph = {
            "....#....",
            ".#.....#.",
            "...ooo...",
            "..ooooo..",
            "#.ooooo.#",
            "..ooooo..",
            "...ooo...",
            ".#.....#.",
            "....#...."};
    public static final String[][] Figure = {
            {"...##..", "...##..", "..####.", ".#.##.#", ".#.##.#", "...##..", "..#..#.", "..#..#.", "..#..#.", "..#..#.", ".##..##"},
            {".......", "..##...", "..####.", ".#.##.#", ".#.##.#", ".o.##.o", "..#..#.", "..#..#.", "..#..#.", "..#..#.", ".##..##"},
            {".......", ".......", ".......", "..##...", ".####..", "#.##.#.", "o.##.o.", "..##o..", ".#..#..", ".#..#..", "o####o."},
            {".......", ".......", ".......", ".......", ".......", "...##..", "..####.", ".o####o", "..####.", ".o####o", "ooooooo"},
            {".......", ".......", ".......", ".......", ".......", ".......", ".......", "...#...", "..###..", ".#####.", "ooooooo"}};
    public static final int Meter = 0xFF9A3C;
    public static final int Shade = 0x8C9AB4;
    public static final int Warn = 0xFF5A32;
    public static final int WarnRay = 0xFF2E1E;
    public static final int Flesh = 0xB4523F;
    public static final int FleshDark = 0x5E2A26;
    public static final int Tint = 0x5A0E0A;
    public static final int Blood = 0x8E1C14;

    private Scp001DayOverlay() {
    }

    public static void indicator(GuiGraphics Graphics, Minecraft Client) {
        if (Client.level == null || !Scp001DayClientState.lethalDay() || !Scp001DayWorld.inside(Client.level)) {
            return;
        }
        boolean Lit = Scp001DayClientState.litPoints() > 0;
        float Charge = Scp001DayClientState.charge();
        int Left = Graphics.guiWidth() / 2 + 8;
        int Top = Graphics.guiHeight() / 2 + 3;
        long Time = Client.level.getGameTime();
        boolean Flash = Lit && (Time / 3) % 2 == 0;
        int Alpha = Lit ? (Flash ? 255 : 170) : 120;
        for (int Row = 0; Row < Glyph.length; Row++) {
            for (int Column = 0; Column < Glyph[Row].length(); Column++) {
                char Cell = Glyph[Row].charAt(Column);
                if (Cell == '.' || (Cell == '#' && !Lit)) {
                    continue;
                }
                int Colour = Lit ? (Cell == '#' ? WarnRay : Warn) : Shade;
                Graphics.fill(Left + Column, Top + Row, Left + Column + 1, Top + Row + 1, (Alpha << 24) | Colour);
            }
        }
        if (Lit || Charge > 0.0f) {
            int Filled = Math.max(Lit ? 1 : 0, Math.round(Charge * 7.0f));
            for (int Index = 0; Index < 7; Index++) {
                int Colour = Index < Filled ? (0xF0 << 24) | (Lit ? WarnRay : Meter) : (0x50 << 24) | Shade;
                Graphics.fill(Left + 1 + Index, Top + Glyph.length + 1, Left + 2 + Index, Top + Glyph.length + 2, Colour);
            }
        }
    }

    public static void figure(GuiGraphics Graphics, Minecraft Client, float Turn) {
        String[] Frame = Figure[Math.min(Figure.length - 1, (int) (Turn * Figure.length))];
        int Left = Graphics.guiWidth() / 2 + 9;
        int Top = Graphics.guiHeight() / 2 + 1;
        long Time = Client.level == null ? 0L : Client.level.getGameTime();
        double Beat = 0.5 + 0.5 * Math.sin(Time * (0.15 + 0.6 * Turn));
        int Alpha = (int) (150 + 100 * Beat);
        for (int Row = 0; Row < Frame.length; Row++) {
            for (int Column = 0; Column < Frame[Row].length(); Column++) {
                char Cell = Frame[Row].charAt(Column);
                if (Cell == '.') {
                    continue;
                }
                Graphics.fill(Left + Column, Top + Row, Left + Column + 1, Top + Row + 1, (Alpha << 24) | (Cell == '#' ? Flesh : FleshDark));
            }
        }
    }

    public static void turning(GuiGraphics Graphics, Minecraft Client, float Turn) {
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Wash = (int) (Mth.clamp(0.12f + 0.45f * Turn, 0.0f, 1.0f) * 255.0f);
        Graphics.fill(0, 0, Width, Height, (Wash << 24) | Tint);
        int Step = Math.max(2, Pixel * Width / 960);
        for (int Band = 0; Band < Bands; Band++) {
            float Share = 1.0f - Band / (float) Bands;
            int Alpha = (int) (Mth.clamp(0.4f + Turn * 1.2f - Band * 0.15f, 0.0f, 1.0f) * 200.0f * Share);
            if (Alpha <= 0) {
                continue;
            }
            int Colour = (Alpha << 24) | (Band < 2 ? 0x2A0705 : Blood);
            int Inset = Band * Step;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Step, Colour);
            Graphics.fill(Inset, Height - Inset - Step, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Inset + Step, Inset + Step, Height - Inset - Step, Colour);
            Graphics.fill(Width - Inset - Step, Inset + Step, Width - Inset, Height - Inset - Step, Colour);
        }
        long Time = Client.level == null ? 0L : Client.level.getGameTime();
        for (int Index = 0; Index < Drips; Index++) {
            int Seed = Index * 7919 + 13;
            int X = Math.floorMod(Seed * 31, Math.max(1, Width / Step)) * Step;
            float Phase = Math.floorMod(Seed + Time / 3, 90) / 90.0f;
            int Length = (int) ((0.2f + 0.8f * Phase) * Turn * Height * 0.5f) / Step * Step;
            if (Length <= 0) {
                continue;
            }
            int Alpha = (int) (110 + 120 * Turn);
            Graphics.fill(X, 0, X + Step, Length, (Alpha << 24) | Blood);
            Graphics.fill(X, Length, X + Step, Length + Step, (Alpha << 24) | 0xB4321E);
        }
        if (Turn > 0.8f) {
            int Dark = (int) (Mth.clamp((Turn - 0.8f) / 0.2f, 0.0f, 1.0f) * 200.0f);
            Graphics.fill(0, 0, Width, Height, (Dark << 24) | 0x100202);
        }
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.options.hideGui || Client.player == null) {
            return;
        }
        float Turn = Scp001DayClientState.turning(Client.player, Delta.getGameTimeDeltaPartialTick(false));
        if (Turn >= 0.0f) {
            turning(Graphics, Client, Turn);
            figure(Graphics, Client, Turn);
            return;
        }
        indicator(Graphics, Client);
    }
}
