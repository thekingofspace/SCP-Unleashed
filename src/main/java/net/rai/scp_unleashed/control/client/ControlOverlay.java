package net.rai.scp_unleashed.control.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.control.network.ControlEventPayload;

public final class ControlOverlay {
    public static final int LinkTicks = 14;
    public static final int ReleaseTicks = 12;
    public static final int SeveredTicks = 50;
    public static final int Step = 6;
    public static final int Bands = 5;
    private static byte Kind = -1;
    private static int Remaining;
    private static int Length = 1;

    private ControlOverlay() {
    }

    public static void trigger(byte Event) {
        Kind = Event;
        Length = switch (Event) {
            case ControlEventPayload.Linked -> LinkTicks;
            case ControlEventPayload.Released -> ReleaseTicks;
            default -> SeveredTicks;
        };
        Remaining = Length;
    }

    public static void tick() {
        if (Remaining > 0) {
            Remaining--;
        }
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        if (Remaining <= 0 || Kind < 0) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Left = Mth.clamp((Remaining - Partial) / Length, 0.0f, 1.0f);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Colour = switch (Kind) {
            case ControlEventPayload.Linked -> 0x3FC8E8;
            case ControlEventPayload.Released -> 0xB4BCC4;
            default -> 0xB4141C;
        };
        float Strength = Kind == ControlEventPayload.Severed ? Math.min(1.0f, Left * 1.6f) : Left;
        for (int Band = 0; Band < Bands; Band++) {
            int Inset = Band * Step;
            int Alpha = Math.round(Strength * (Kind == ControlEventPayload.Severed ? 120 : 70) * (Bands - Band) / Bands);
            if (Alpha <= 0) {
                continue;
            }
            int Fill = Alpha << 24 | Colour;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Step, Fill);
            Graphics.fill(Inset, Height - Inset - Step, Width - Inset, Height - Inset, Fill);
            Graphics.fill(Inset, Inset + Step, Inset + Step, Height - Inset - Step, Fill);
            Graphics.fill(Width - Inset - Step, Inset + Step, Width - Inset, Height - Inset - Step, Fill);
        }
        if (Kind == ControlEventPayload.Linked) {
            int Line = Math.round((1.0f - Left) * Height);
            Graphics.fill(0, Line - 2, Width, Line + 2, Math.round(Left * 90) << 24 | Colour);
        }
        if (Kind == ControlEventPayload.Severed) {
            Font Text = Minecraft.getInstance().font;
            Component Message = Component.translatable("gui.scp_unleashed.control.severed");
            boolean Blink = (Remaining / 4) % 2 == 0 || Left < 0.5f;
            if (Blink) {
                int TextWidth = Text.width(Message);
                int X = (Width - TextWidth) / 2;
                int Y = Height / 3;
                Graphics.fill(X - 6, Y - 4, X + TextWidth + 6, Y + 12, Math.round(Strength * 170) << 24 | 0x1A0406);
                Graphics.drawString(Text, Message, X, Y, Math.max(16, Math.round(Strength * 255)) << 24 | 0xFF5A5A, false);
            }
        }
    }
}
