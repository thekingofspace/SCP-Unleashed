package net.rai.scp_unleashed.scp001gate.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;
import net.rai.scp_unleashed.scp001gate.network.Scp001GateWarnPayload;

public final class Scp001GateOverlay {
    public static final ResourceLocation LayerId = Scp001GateRegistry.location("scp001_gate_warning");
    public static final int Length = 90;
    public static final int FadeIn = 6;
    public static final int FadeOut = 26;
    public static final int[] Core = {0xFFD27A, 0xFFB050, 0xFF8030, 0xFF5A1E};
    public static final int[] Ember = {0x8A3A10, 0x7A2A0C, 0x6A1A08, 0x5A0E06};
    public static final int Haze = 0xC0461A;
    private static Component Message = Component.empty();
    private static int Tier;
    private static float Closeness;
    private static long Shown = -1000;

    private Scp001GateOverlay() {
    }

    public static void show(Scp001GateWarnPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        long Now = Client.level.getGameTime();
        float Age = Now - Shown;
        Tier = Mth.clamp(Payload.Tier(), 0, Core.length - 1);
        Closeness = Payload.Closeness();
        Message = Component.translatable("message.scp_unleashed.scp001_gate.warn." + Tier + "." + Mth.clamp(Payload.Variant(), 0, 2));
        Shown = Age >= 0.0f && Age < Length - FadeOut ? Now - Math.min((long) Age, FadeIn) : Now;
    }

    public static float share(float Age) {
        if (Age < 0.0f || Age > Length) {
            return 0.0f;
        }
        float In = Mth.clamp(Age / FadeIn, 0.0f, 1.0f);
        float Out = Mth.clamp((Length - Age) / FadeOut, 0.0f, 1.0f);
        return Math.min(In, Out);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.options.hideGui) {
            return;
        }
        float Age = Client.level.getGameTime() - Shown + Delta.getGameTimeDeltaPartialTick(false);
        float Alpha = share(Age);
        if (Alpha <= 0.01f) {
            return;
        }
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        edges(Graphics, Width, Height, Alpha * (0.25f + 0.45f * Closeness));
        Font Text = Client.font;
        FormattedCharSequence Line = Message.getVisualOrderText();
        int TextWidth = Math.max(1, Text.width(Line));
        float Scale = Math.min(4.0f + Tier * 0.6f, Width * 0.86f / TextWidth);
        float Shake = (0.6f + Closeness * 1.6f) * Alpha;
        float Time = Client.level.getGameTime() + Delta.getGameTimeDeltaPartialTick(false);
        float Jx = (float) Math.round(Mth.sin(Time * 2.3f) * Shake);
        float Jy = (float) Math.round(Mth.cos(Time * 3.1f) * Shake * 0.6f);
        int Fade = Mth.clamp((int) (Alpha * 255.0f), 4, 255) << 24;
        Graphics.pose().pushPose();
        Graphics.pose().translate(Width / 2.0f + Jx, Height * 0.38f + Jy, 0.0f);
        Graphics.pose().scale(Scale, Scale, 1.0f);
        int Left = -TextWidth / 2;
        int Top = -Text.lineHeight / 2;
        Graphics.drawString(Text, Line, Left + 1, Top + 1, Fade | Ember[Tier], false);
        Graphics.drawString(Text, Line, Left, Top + 1, Fade | Ember[Math.max(0, Tier - 1)], false);
        Graphics.drawString(Text, Line, Left, Top, Fade | Core[Tier], false);
        Graphics.pose().popPose();
    }

    private static void edges(GuiGraphics Graphics, int Width, int Height, float Strength) {
        int Bands = 6;
        for (int Band = 0; Band < Bands; Band++) {
            int Alpha = Mth.clamp((int) (Strength * 110.0f * (1.0f - Band / (float) Bands)), 0, 255);
            if (Alpha <= 0) {
                continue;
            }
            int Colour = Alpha << 24 | Haze;
            int Inset = Band * 6;
            int Thick = 6;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Thick, Colour);
            Graphics.fill(Inset, Height - Inset - Thick, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Inset + Thick, Inset + Thick, Height - Inset - Thick, Colour);
            Graphics.fill(Width - Inset - Thick, Inset + Thick, Width - Inset, Height - Inset - Thick, Colour);
        }
    }
}
