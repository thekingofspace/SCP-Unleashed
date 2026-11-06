package net.rai.scp_unleashed.scp539.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp539.Scp539Registry;

public final class Scp539Overlay {
    public static final ResourceLocation LayerId = Scp539Registry.location("scp539_plan_banner");
    public static final String TextKey = "gui.scp_unleashed.scp539.greatest_plan";
    public static final int MaxScale = 4;
    public static final int Margin = 24;
    public static final float Height = 0.68f;
    public static final int Gold = 0xFFFFD84A;
    public static final int Cream = 0xFFFFF4D6;
    public static final int FlashEvery = 3;
    private static int Left;
    private static int Shown;

    private Scp539Overlay() {
    }

    public static void warn(int Ticks) {
        Left = Math.max(Left, Ticks);
    }

    public static boolean showing() {
        return Left > 0;
    }

    public static void tick() {
        if (Left > 0) {
            Left--;
            Shown++;
        } else {
            Shown = 0;
        }
    }

    public static void reset() {
        Left = 0;
        Shown = 0;
    }

    public static int scaleFor(int ScreenWidth, int TextWidth) {
        return Mth.clamp((ScreenWidth - Margin) / Math.max(1, TextWidth), 1, MaxScale);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Left <= 0 || Client.player == null || Client.options.hideGui) {
            return;
        }
        Font Writer = Client.font;
        Component Text = Component.translatable(TextKey);
        int Width = Writer.width(Text);
        int Scale = scaleFor(Graphics.guiWidth(), Width);
        if (Shown < 2) {
            Scale = Math.max(1, Scale - 1);
        }
        int ShakeX = (Shown * 7) % 3 - 1;
        int ShakeY = (Shown * 5) % 3 - 1;
        int Colour = (Shown / FlashEvery) % 2 == 0 ? Gold : Cream;
        int Left0 = Math.round(Graphics.guiWidth() * 0.5f);
        int Top = Math.round(Graphics.guiHeight() * Height);
        Graphics.pose().pushPose();
        Graphics.pose().translate(Left0, Top, 0.0f);
        Graphics.pose().scale(Scale, Scale, 1.0f);
        Graphics.drawString(Writer, Text, -Width / 2 + ShakeX, -Writer.lineHeight / 2 + ShakeY, Colour, true);
        Graphics.pose().popPose();
    }
}
