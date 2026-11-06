package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunAmmo;
import net.rai.scp_unleashed.guns.GunHeat;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.scp001scarlet.king.client.ScarletRifleHud;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GunHud {
    public static final ResourceLocation ScopeTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/misc/guns/scope.png");
    public static final ResourceLocation HudTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/guns/gun_hud.png");
    public static final int ScopeSize = 256;
    public static final int HudWidth = 64;
    public static final int HudHeight = 32;
    public static final int PanelWidth = 60;
    public static final int PanelHeight = 22;
    public static final int PipU = 0;
    public static final int PipV = 22;
    public static final int PipSize = 7;
    public static final int BarU = 21;
    public static final int BarV = 22;
    public static final int BarWidth = 36;
    public static final int BarHeight = 3;
    public static final int CrossLength = 4;
    public static final int CrossBase = 3;
    public static final int MaxGap = 60;
    public static final int AmmoColour = 0xFFF2E6C8;
    public static final int ReserveColour = 0xFF9AA3AD;
    public static final int EmptyColour = 0xFFE5534B;
    public static final int ChargeColour = 0xFF4FF0D2;
    public static final int KillColour = 0xFFFF4B3E;
    public static final int WarmColour = 0xFFFFB347;
    public static final int HotColour = 0xFFFF5A3C;
    public static final int VentColour = 0xFFE5534B;

    private GunHud() {
    }

    @SubscribeEvent
    public static void hideVanillaCrosshair(RenderGuiLayerEvent.Pre Event) {
        if (!Event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            return;
        }
        LocalPlayer User = Minecraft.getInstance().player;
        if (User != null && GunItem.kindOf(User.getMainHandItem()) != null) {
            Event.setCanceled(true);
        }
    }

    public static void renderCrosshair(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || User == null || !Game.options.getCameraType().isFirstPerson() || User.isSpectator()) {
            return;
        }
        GunKind Kind = GunItem.kindOf(User.getMainHandItem());
        if (Kind == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int CentreX = Graphics.guiWidth() / 2;
        int CentreY = Graphics.guiHeight() / 2;
        float Visible = 1.0F - GunClientState.aimEased(Partial);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        if (Visible > 0.05F) {
            int Shade = Mth.clamp(Math.round(Visible * 255.0F), 0, 255);
            int Colour = Shade << 24 | 0xFFFFFF;
            double Fov = Game.options.fov().get() * GunClientState.zoom(Partial);
            double Spread = GunClientState.spread(User, Kind, Partial);
            int Gap = CrossBase + (int) Math.min(MaxGap, Math.round(Graphics.guiHeight() * 0.5 * Math.tan(Math.toRadians(Spread)) / Math.tan(Math.toRadians(Fov * 0.5))));
            Graphics.fill(CentreX, CentreY, CentreX + 1, CentreY + 1, Colour);
            Graphics.fill(CentreX - Gap - CrossLength, CentreY, CentreX - Gap, CentreY + 1, Colour);
            Graphics.fill(CentreX + Gap + 1, CentreY, CentreX + Gap + 1 + CrossLength, CentreY + 1, Colour);
            Graphics.fill(CentreX, CentreY - Gap - CrossLength, CentreX + 1, CentreY - Gap, Colour);
            Graphics.fill(CentreX, CentreY + Gap + 1, CentreX + 1, CentreY + Gap + 1 + CrossLength, Colour);
        }
        RenderSystem.defaultBlendFunc();
        float Marker = GunClientState.hitMarker(Partial);
        if (Marker > 0.0F) {
            int Alpha = Mth.clamp(Math.round(Marker * 255.0F), 0, 255);
            int Colour = Alpha << 24 | (GunClientState.hitKill() ? KillColour : 0xFFFFFF) & 0xFFFFFF;
            for (int Step = 3; Step <= 6; Step++) {
                Graphics.fill(CentreX - Step, CentreY - Step, CentreX - Step + 1, CentreY - Step + 1, Colour);
                Graphics.fill(CentreX + Step, CentreY - Step, CentreX + Step + 1, CentreY - Step + 1, Colour);
                Graphics.fill(CentreX - Step, CentreY + Step, CentreX - Step + 1, CentreY + Step + 1, Colour);
                Graphics.fill(CentreX + Step, CentreY + Step, CentreX + Step + 1, CentreY + Step + 1, Colour);
            }
        }
        RenderSystem.disableBlend();
    }

    public static void renderScope(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player == null || !Game.options.getCameraType().isFirstPerson()) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Scope = GunClientState.scope(Partial);
        if (Scope <= 0.01F) {
            return;
        }
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Fit = Math.min(Width, Height);
        int Size = Mth.floor(Fit * (1.0F + 0.6F * (1.0F - Scope)));
        int Left = (Width - Size) / 2;
        int Top = (Height - Size) / 2;
        int Right = Left + Size;
        int Bottom = Top + Size;
        int Shade = Mth.clamp(Math.round(Scope * 255.0F), 0, 255) << 24;
        RenderSystem.enableBlend();
        Graphics.setColor(1.0F, 1.0F, 1.0F, Scope);
        Graphics.blit(ScopeTexture, Left, Top, -90, 0.0F, 0.0F, Size, Size, Size, Size);
        Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        Graphics.fill(RenderType.guiOverlay(), 0, Bottom, Width, Height, -90, Shade);
        Graphics.fill(RenderType.guiOverlay(), 0, 0, Width, Top, -90, Shade);
        Graphics.fill(RenderType.guiOverlay(), 0, Top, Left, Bottom, -90, Shade);
        Graphics.fill(RenderType.guiOverlay(), Right, Top, Width, Bottom, -90, Shade);
        RenderSystem.disableBlend();
    }

    public static void renderAmmo(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || User == null || User.isSpectator() || Game.level == null) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null || !Kind.getStats().usesCharges() && Kind.getStats().getCapacity() <= 0) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        GunStats Stats = Kind.getStats();
        Font Text = Game.font;
        int OffHandShift = User.getMainArm() == HumanoidArm.LEFT && !User.getOffhandItem().isEmpty() ? 29 : 0;
        int Left = Graphics.guiWidth() / 2 + 91 + 8 + OffHandShift;
        int Top = Graphics.guiHeight() - PanelHeight - 1;
        RenderSystem.enableBlend();
        Graphics.blit(HudTexture, Left, Top, 0.0F, 0.0F, PanelWidth, PanelHeight, HudWidth, HudHeight);
        if (Kind == GunKind.ScarletRifle) {
            ScarletRifleHud.render(Graphics, Text, Left, Top, Stack, Game.level.getGameTime(), Delta.getGameTimeDeltaPartialTick(false));
            RenderSystem.disableBlend();
            return;
        }
        long Now = Game.level.getGameTime();
        if (Stats.usesCharges()) {
            int Charges = GunItem.getCharges(Stack);
            for (int Pip = 0; Pip < Stats.getCharges(); Pip++) {
                boolean Lit = Pip < Charges;
                Graphics.blit(HudTexture, Left + 4 + Pip * 9, Top + 4, PipU + (Lit ? PipSize : 0), PipV, PipSize, PipSize, HudWidth, HudHeight);
            }
            Graphics.drawString(Text, Component.literal(Integer.toString(Charges)), Left + 34, Top + 4, Charges > 0 ? ChargeColour : EmptyColour, true);
            GunTimer Charging = GunItem.getCharging(Stack);
            if (Charging != null) {
                bar(Graphics, Left + 20, Top + 14, Charging.progress(Now, Partial));
            }
        } else {
            int Loaded = GunClientState.predictedAmmo(User, Stack);
            boolean UsesAmmo = Kind.usesAmmo();
            int Reserve = !UsesAmmo ? Stats.getCapacity() : User.getAbilities().instabuild ? -1 : GunAmmo.count(User, Kind);
            if (UsesAmmo) {
                Graphics.renderItem(new ItemStack(Kind.getAmmo()), Left + 3, Top + 3);
            }
            Component Count = Component.literal(Integer.toString(Loaded)).withStyle(ChatFormatting.BOLD);
            Graphics.drawString(Text, Count, Left + 21, Top + 3, Loaded > 0 ? AmmoColour : EmptyColour, true);
            int SpareX = Left + 23 + Text.width(Count);
            int Room = Left + PanelWidth - 3 - SpareX;
            String Spare = Reserve < 0 ? "/ ∞" : "/ " + Reserve;
            if (Text.width(Spare) > Room) {
                Spare = Reserve < 0 ? "/∞" : "/" + Reserve;
            }
            int SpareWidth = Text.width(Spare);
            if (SpareWidth > Room && Room > 0) {
                float Squeeze = Math.max(0.5F, Room / (float) SpareWidth);
                Graphics.pose().pushPose();
                Graphics.pose().translate(SpareX, Top + 3 + (1.0F - Squeeze) * Text.lineHeight * 0.5F, 0.0F);
                Graphics.pose().scale(Squeeze, Squeeze, 1.0F);
                Graphics.drawString(Text, Spare, 0, 0, ReserveColour, true);
                Graphics.pose().popPose();
            } else {
                Graphics.drawString(Text, Spare, SpareX, Top + 3, ReserveColour, true);
            }
            GunTimer Reloading = GunItem.getReload(Stack);
            GunTimer Vent = GunHeat.vent(Stack);
            if (Reloading != null) {
                bar(Graphics, Left + 20, Top + 14, Reloading.progress(Now, Partial));
            } else if (Vent != null && !Vent.isDone(Now)) {
                heatBar(Graphics, Left + 20, Top + 14, 1.0F - Vent.progress(Now, Partial), VentColour);
                Graphics.drawString(Text, Component.translatable("hud.scp_unleashed.guns.overheated"), Left + 4, Top - 10, EmptyColour, true);
            } else if (Loaded == 0 && Reserve != 0 && UsesAmmo) {
                Component Hint = Component.translatable("hud.scp_unleashed.guns.reload", Component.keybind(GunItem.ReloadKey));
                Graphics.drawString(Text, Hint, Left + 4, Top - 10, EmptyColour, true);
            } else if (Stats.usesHeat()) {
                float Heat = GunHeat.share(Stack, Stats, Now);
                if (Heat > 0.01F) {
                    heatBar(Graphics, Left + 20, Top + 14, Heat, blend(WarmColour, HotColour, Heat));
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private static void heatBar(GuiGraphics Graphics, int X, int Y, float Progress, int Colour) {
        Graphics.blit(HudTexture, X, Y, BarU, BarV, BarWidth, BarHeight, HudWidth, HudHeight);
        int Filled = Mth.clamp(Math.round(Progress * BarWidth), 0, BarWidth);
        if (Filled > 0) {
            Graphics.setColor((Colour >> 16 & 0xFF) / 255.0F, (Colour >> 8 & 0xFF) / 255.0F, (Colour & 0xFF) / 255.0F, 1.0F);
            Graphics.blit(HudTexture, X, Y, BarU, BarV + BarHeight, Filled, BarHeight, HudWidth, HudHeight);
            Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static int blend(int From, int To, float Weight) {
        float Share = Mth.clamp(Weight, 0.0F, 1.0F);
        int Red = Math.round(Mth.lerp(Share, From >> 16 & 0xFF, To >> 16 & 0xFF));
        int Green = Math.round(Mth.lerp(Share, From >> 8 & 0xFF, To >> 8 & 0xFF));
        int Blue = Math.round(Mth.lerp(Share, From & 0xFF, To & 0xFF));
        return 0xFF000000 | Red << 16 | Green << 8 | Blue;
    }

    private static void bar(GuiGraphics Graphics, int X, int Y, float Progress) {
        Graphics.blit(HudTexture, X, Y, BarU, BarV, BarWidth, BarHeight, HudWidth, HudHeight);
        int Filled = Mth.clamp(Math.round(Progress * BarWidth), 0, BarWidth);
        if (Filled > 0) {
            Graphics.blit(HudTexture, X, Y, BarU, BarV + BarHeight, Filled, BarHeight, HudWidth, HudHeight);
        }
    }
}
