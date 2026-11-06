package net.rai.scp_unleashed.extraction.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rift.RiftEntity;

import java.util.Locale;

public final class DetectorHud {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/extraction/detector_hud.png");
    public static final int TextureWidth = 256;
    public static final int TextureHeight = 64;
    public static final int PanelWidth = 78;
    public static final int PanelHeight = 23;
    public static final int DialU = 80;
    public static final int DialSize = 21;
    public static final int FullU = 104;
    public static final int EmptyU = 108;
    public static final int SegmentWidth = 3;
    public static final int SegmentHeight = 5;
    public static final int Segments = 12;
    public static final int LedU = 112;
    public static final int ChevronU = 116;
    public static final int DotU = 122;
    public static final int ArrowTop = 24;
    public static final int ArrowSize = 15;
    public static final int Directions = 32;
    public static final int Spacing = 4;
    public static final int IndicatorSpacing = 26;
    public static final float ChevronAngle = 25.0F;
    private static final int TextColour = 0xE6EAEE;
    private static final int IdleColour = 0x8A9098;

    private DetectorHud() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || Game.level == null || User == null || User.isSpectator()) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Alpha = Mth.clamp(DetectorTracker.alpha(Partial), 0.0F, 1.0F);
        if (Alpha < 0.03F) {
            return;
        }
        int Middle = Graphics.guiWidth() / 2;
        int Gap = Game.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR ? IndicatorSpacing : Spacing;
        int Left = Mth.clamp(User.getMainArm() == HumanoidArm.RIGHT ? Middle + 91 + Gap : Middle - 91 - Gap - PanelWidth, 0, Graphics.guiWidth() - PanelWidth);
        int Top = Graphics.guiHeight() - PanelHeight;
        int TextAlpha = Math.max(4, Math.round(Alpha * 255.0F)) << 24;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0F, 1.0F, 1.0F, Alpha);
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureWidth, TextureHeight);
        Graphics.blit(Texture, Left + 1, Top + 1, DialU, 0, DialSize, DialSize, TextureWidth, TextureHeight);
        DetectorTracker.Reading Found = DetectorTracker.current();
        Entity Seen = Found == null ? null : Game.level.getEntity(Found.RiftId());
        int BarLeft = Left + 26;
        int BarTop = Top + 14;
        if (Found != null && Seen instanceof RiftEntity Rift) {
            int Colour = Found.Grade().getColour();
            float Red = (Colour >> 16 & 255) / 255.0F;
            float Green = (Colour >> 8 & 255) / 255.0F;
            float Blue = (Colour & 255) / 255.0F;
            Vec3 Eye = User.getEyePosition(Partial);
            Vec3 Offset = Rift.getCenter(Partial).subtract(Eye);
            double Flat = Math.sqrt(Offset.x * Offset.x + Offset.z * Offset.z);
            float Bearing = (float) Math.toDegrees(Math.atan2(-Offset.x, Offset.z));
            float Relative = Mth.wrapDegrees(Bearing - User.getViewYRot(Partial));
            int Index = Math.floorMod(Math.round(Relative / (360.0F / Directions)), Directions);
            Graphics.setColor(Red, Green, Blue, Alpha);
            Graphics.blit(Texture, Left + 1 + DialSize / 2 - ArrowSize / 2, Top + 1 + DialSize / 2 - ArrowSize / 2,
                    (Index % 16) * ArrowSize, ArrowTop + (Index / 16) * ArrowSize, ArrowSize, ArrowSize, TextureWidth, TextureHeight);
            int Lit = Mth.ceil(Found.Strength() * Segments);
            for (int Segment = 0; Segment < Segments; Segment++) {
                boolean On = Segment < Lit;
                float Boost = On && Segment == Lit - 1 && DetectorTracker.flashing() ? 1.25F : 1.0F;
                Graphics.setColor(Math.min(1.0F, Red * Boost), Math.min(1.0F, Green * Boost), Math.min(1.0F, Blue * Boost), Alpha * (On ? 1.0F : 0.7F));
                Graphics.blit(Texture, BarLeft + Segment * (SegmentWidth + 1), BarTop, On ? FullU : EmptyU, 0, SegmentWidth, SegmentHeight, TextureWidth, TextureHeight);
            }
            float Glow = DetectorTracker.flashing() ? 1.0F : 0.35F;
            Graphics.setColor(Red * Glow + (1.0F - Glow) * 0.2F, Green * Glow + (1.0F - Glow) * 0.2F, Blue * Glow + (1.0F - Glow) * 0.2F, Alpha);
            Graphics.blit(Texture, Left + PanelWidth - 6, Top + 3, LedU, 0, 3, 3, TextureWidth, TextureHeight);
            double Distance = Offset.length();
            String Shown = Distance < 10.0 ? String.format(Locale.ROOT, "%.1f", Distance) : Integer.toString((int) Math.round(Distance));
            String Label = Component.translatable("hud.scp_unleashed.extraction.detector.distance", Shown).getString();
            Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            int Written = Graphics.drawString(Game.font, Label, Left + 26, Top + 3, TextAlpha | TextColour, true);
            float Elevation = (float) Math.toDegrees(Math.atan2(Offset.y, Flat)) + User.getViewXRot(Partial);
            if (Math.abs(Elevation) > ChevronAngle) {
                Graphics.setColor(Red, Green, Blue, Alpha);
                Graphics.blit(Texture, Written + 2, Top + 5, ChevronU, Elevation > 0.0F ? 0 : 4, 5, 3, TextureWidth, TextureHeight);
            }
        } else {
            Graphics.setColor(0.5F, 0.54F, 0.58F, Alpha);
            Graphics.blit(Texture, Left + 1 + DialSize / 2 - 1, Top + 1 + DialSize / 2 - 1, DotU, 0, 3, 3, TextureWidth, TextureHeight);
            for (int Segment = 0; Segment < Segments; Segment++) {
                Graphics.setColor(0.45F, 0.48F, 0.52F, Alpha * 0.7F);
                Graphics.blit(Texture, BarLeft + Segment * (SegmentWidth + 1), BarTop, EmptyU, 0, SegmentWidth, SegmentHeight, TextureWidth, TextureHeight);
            }
            Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Graphics.drawString(Game.font, Component.translatable("hud.scp_unleashed.extraction.detector.none").getString(), Left + 26, Top + 3, TextAlpha | IdleColour, true);
        }
        Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
