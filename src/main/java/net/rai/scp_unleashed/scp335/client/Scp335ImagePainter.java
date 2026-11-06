package net.rai.scp_unleashed.scp335.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp335.Scp335Episodes;
import net.rai.scp_unleashed.scp335.Scp335Phase;
import net.rai.scp_unleashed.scp335.Scp335Registry;
import net.rai.scp_unleashed.senses.Senses;

public final class Scp335ImagePainter {
    public static final ResourceLocation Texture = Scp335Registry.location("textures/gui/scp335/all.png");
    public static final int FrameWidth = 100;
    public static final int FrameHeight = 58;
    public static final int Frames = 20;
    public static final int Band = 2;

    private Scp335ImagePainter() {
    }

    public static void paint(GuiGraphics Graphics, int X, int Y, int W, int H, int Ticks) {
        LocalPlayer User = Minecraft.getInstance().player;
        Graphics.fill(X, Y, X + W, Y + H, 0xFF000000);
        if (User == null || Senses.isBlindfolded(User)) {
            return;
        }
        int Scale = W >= FrameWidth * 2 && H >= FrameHeight * 2 ? 2 : 1;
        int DrawWidth = FrameWidth * Scale;
        int DrawHeight = FrameHeight * Scale;
        int Left = X + (W - DrawWidth) / 2;
        int Top = Y + (H - DrawHeight) / 2;
        float Rush = glimpse();
        int Step = Ticks / (Rush > 0.5f ? 1 : 2);
        int Frame = Rush > 0.5f ? Scp335Overlay.mix(Step) % Frames : Math.floorMod(Step, Frames);
        float Shade = AntiMemetic.isProtected(User) ? 0.4f : 1.0f;
        int Rare = Math.max(2, Math.round(11.0f - Rush * 9.0f));
        Graphics.enableScissor(X, Y, X + W, Y + H);
        RenderSystem.setShaderColor(Shade, Shade, Shade, 1.0f);
        for (int Row = 0; Row < FrameHeight; Row += Band) {
            int Mixed = Scp335Overlay.mix(Step * 31 + Row);
            int Shift = Mixed % Rare == 0 ? (Mixed >>> 8) % 13 - 6 : 0;
            int Source = Frame;
            if (Rush > 0.0f && (Mixed >>> 4) % 5 == 0) {
                Source = (Frame + 1 + (Mixed >>> 12) % (Frames - 1)) % Frames;
            }
            Graphics.blit(Texture, Left + Shift * Scale, Top + Row * Scale, DrawWidth, Band * Scale, 0.0f, Source * FrameHeight + Row, FrameWidth, Band, FrameWidth,
                    FrameHeight * Frames);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (Rush > 0.75f) {
            int Flash = Math.round(Mth.clamp((Rush - 0.75f) * 4.0f, 0.0f, 1.0f) * 150.0f);
            Graphics.fill(X, Y, X + W, Y + H, Flash << 24 | 0xFFFFFF);
        }
        Graphics.disableScissor();
    }

    private static float glimpse() {
        Scp335ClientState.Shown State = Scp335ClientState.local();
        if (State == null || State.Phase != Scp335Phase.Glimpse) {
            return 0.0f;
        }
        return Mth.clamp((float) State.Tick / Scp335Episodes.GlimpseTicks, 0.0f, 1.0f);
    }
}
