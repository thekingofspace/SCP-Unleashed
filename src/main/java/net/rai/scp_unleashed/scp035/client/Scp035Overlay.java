package net.rai.scp_unleashed.scp035.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp035Overlay {
    public static final ResourceLocation LureLayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp035_lure_overlay");
    public static final ResourceLocation MaskLayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp035_mask_overlay");
    public static final ResourceLocation Vignette = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp035/vignette.png");
    public static final ResourceLocation EyeSlits = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp035/eye_slits.png");
    public static final float BaseShade = 0.2f;
    public static final float ShadeRange = 0.45f;
    public static final float PulseShade = 0.12f;
    public static final int SlitsWidth = 128;
    public static final int SlitsHeight = 54;
    public static final int SlitsCore = 96;
    public static final int Surround = 0xF62A2826;
    public static final float LatchZoom = 0.7f;
    public static final float SwayTexels = 0.3f;

    private Scp035Overlay() {
    }

    public static void renderLure(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui) {
            return;
        }
        float Amount = Scp035ClientLure.intensity();
        if (Amount < 0.01f) {
            return;
        }
        float Pulse = Scp035ClientLure.pulse(Delta.getGameTimeDeltaPartialTick(false));
        float Alpha = Math.min(1.0f, (BaseShade + ShadeRange * Amount) * Math.min(1.0f, Amount * 6.0f) + PulseShade * Pulse * Amount);
        draw(Graphics, Vignette, Alpha);
    }

    public static void renderMask(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || !Client.options.getCameraType().isFirstPerson() || Client.getCameraEntity() != User || User.isScoping()) {
            return;
        }
        float PartialTick = Delta.getGameTimeDeltaPartialTick(false);
        float Closed = Scp035ClientMask.eased(PartialTick);
        if (Closed <= 0.0f) {
            return;
        }
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Texel = Math.min((float) Height / SlitsHeight, (float) Width / SlitsCore) * (1.0f + LatchZoom * (1.0f - Closed));
        float PanelWidth = SlitsWidth * Texel;
        float PanelHeight = SlitsHeight * Texel;
        float Left = (Width - PanelWidth) / 2.0f;
        float Top = (Height - PanelHeight) / 2.0f + Scp035ClientMask.breath(PartialTick) * SwayTexels * Texel;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, Closed * 1.5f));
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.translate(Left, Top, -90.0f);
        Pose.scale(Texel, Texel, 1.0f);
        Graphics.blit(EyeSlits, 0, 0, 0, 0.0f, 0.0f, SlitsWidth, SlitsHeight, SlitsWidth, SlitsHeight);
        Pose.popPose();
        int PanelLeft = Mth.floor(Left);
        int PanelRight = Mth.ceil(Left + PanelWidth);
        int PanelTop = Mth.floor(Top);
        int PanelBottom = Mth.ceil(Top + PanelHeight);
        if (PanelLeft > 0) {
            Graphics.fill(0, 0, PanelLeft + 1, Height, Surround);
        }
        if (PanelRight < Width) {
            Graphics.fill(PanelRight - 1, 0, Width, Height, Surround);
        }
        if (PanelTop > 0) {
            Graphics.fill(Math.max(0, PanelLeft + 1), 0, Math.min(Width, PanelRight - 1), PanelTop + 1, Surround);
        }
        if (PanelBottom < Height) {
            Graphics.fill(Math.max(0, PanelLeft + 1), PanelBottom - 1, Math.min(Width, PanelRight - 1), Height, Surround);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void draw(GuiGraphics Graphics, ResourceLocation Texture, float Alpha) {
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        Graphics.blit(Texture, 0, 0, -90, 0.0f, 0.0f, Width, Height, Width, Height);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
