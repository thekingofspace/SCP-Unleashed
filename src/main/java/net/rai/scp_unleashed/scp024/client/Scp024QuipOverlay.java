package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp024QuipOverlay {
    public static final int BoxU = 0;
    public static final int BoxV = 112;
    public static final int BoxWidth = 236;
    public static final int BoxHeight = 44;
    public static final int Margin = 6;
    public static final int Top = 34;
    public static final int FaceX = 6;
    public static final int FaceY = 6;
    public static final int FaceSize = 32;
    public static final int TextX = 44;
    public static final int TextY = 7;
    public static final int TextWidth = 184;
    public static final int TextRows = 3;
    public static final int LinePitch = 11;
    public static final int Slide = 6;
    public static final int TextColour = 0xFFEDE3CF;
    @Nullable
    private static Component Text;
    private static int Life;
    private static int Total;

    private Scp024QuipOverlay() {
    }

    public static void show(Component Line, int Ticks) {
        Text = Line;
        Life = Ticks;
        Total = Ticks;
    }

    public static void tick() {
        if (Life > 0) {
            Life--;
            if (Life == 0) {
                Text = null;
            }
        }
    }

    public static void reset() {
        Text = null;
        Life = 0;
    }

    public static boolean active() {
        return Text != null;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Text == null || Client.options.hideGui || Client.player == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Age = Total - Life + Partial;
        float Remaining = Life - Partial;
        float In = Mth.clamp(Age / Slide, 0.0f, 1.0f);
        float Out = Mth.clamp(Remaining / Slide, 0.0f, 1.0f);
        int Offset = Math.round((1.0f - Math.min(In, Out)) * (BoxWidth + Margin));
        int Left = Graphics.guiWidth() - BoxWidth - Margin + Offset;
        Graphics.blit(Scp024DialogueScreen.Sheet, Left, Top, BoxU, BoxV, BoxWidth, BoxHeight, Scp024DialogueScreen.SheetWidth, Scp024DialogueScreen.SheetHeight);
        Graphics.blit(Scp024DialogueScreen.Portrait, Left + FaceX, Top + FaceY, FaceSize, FaceSize, 0.0f, 0.0f, 64, 64, 64, 64);
        Font Writer = Client.font;
        List<FormattedCharSequence> Lines = Writer.split(Text, TextWidth);
        int Rows = Math.min(TextRows, Lines.size());
        int Start = Top + TextY + (TextRows - Rows) * LinePitch / 2;
        for (int Row = 0; Row < Rows; Row++) {
            Graphics.drawString(Writer, Lines.get(Row), Left + TextX, Start + Row * LinePitch, TextColour, false);
        }
    }
}
