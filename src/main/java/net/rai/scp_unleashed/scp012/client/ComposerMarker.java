package net.rai.scp_unleashed.scp012.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class ComposerMarker {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp012_composer_marker");
    public static final int Margin = 3;
    public static final int PlateFill = 0xD8272A2F;
    public static final int PlateLight = 0xD8464B53;
    public static final int PlateShade = 0xD815161A;
    public static final int InkColour = 0xA3101E;
    public static final int InkLight = 0xD8303F;
    public static final int InkDark = 0x5E0710;
    public static final int BreathPeriod = 80;
    public static final float[] BreathLevels = {0.7f, 0.78f, 0.86f, 0.94f, 0.86f, 0.78f};
    public static final String[] Plate = {
            ".#########.",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            "###########",
            ".#########."
    };
    public static final String[] Clef = {
            "..h#.",
            ".#..#",
            ".#.#.",
            "..##.",
            ".##..",
            "h#.h.",
            "#.#.#",
            "#.#.#",
            ".###.",
            "..#..",
            "h.#..",
            ".d..."
    };

    private ComposerMarker() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui || !Scp012ClientState.isComposer()) {
            return;
        }
        int Width = Plate[0].length();
        int Height = Plate.length;
        int Left = Graphics.guiWidth() - Width - Margin;
        int Top = Graphics.guiHeight() - Height - Margin;
        for (int Y = 0; Y < Height; Y++) {
            for (int X = 0; X < Width; X++) {
                if (Plate[Y].charAt(X) != '#') {
                    continue;
                }
                boolean Rim = Y == 0 || X == 0 || Plate[Y - 1].charAt(X) != '#' || Plate[Y].charAt(X - 1) != '#';
                boolean Under = Y == Height - 1 || X == Width - 1 || Plate[Y + 1].charAt(X) != '#' || Plate[Y].charAt(X + 1) != '#';
                int Colour = Rim ? PlateLight : Under ? PlateShade : PlateFill;
                Graphics.fill(Left + X, Top + Y, Left + X + 1, Top + Y + 1, Colour);
            }
        }
        long Time = Client.player.tickCount;
        float Breath = BreathLevels[(int) Math.floorMod(Time * BreathLevels.length / BreathPeriod, (long) BreathLevels.length)];
        int Alpha = Math.round(Breath * 255.0f) << 24;
        int InkLeft = Left + (Width - Clef[0].length()) / 2;
        int InkTop = Top + (Height - Clef.length) / 2;
        for (int Y = 0; Y < Clef.length; Y++) {
            for (int X = 0; X < Clef[Y].length(); X++) {
                char Mark = Clef[Y].charAt(X);
                int Colour = switch (Mark) {
                    case '#' -> InkColour;
                    case 'h' -> InkLight;
                    case 'd' -> InkDark;
                    default -> -1;
                };
                if (Colour >= 0) {
                    Graphics.fill(InkLeft + X, InkTop + Y, InkLeft + X + 1, InkTop + Y + 1, Alpha | Colour);
                }
            }
        }
    }
}
