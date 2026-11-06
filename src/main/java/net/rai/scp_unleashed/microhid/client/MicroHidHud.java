package net.rai.scp_unleashed.microhid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guns.client.GunHud;
import net.rai.scp_unleashed.microhid.MicroHidEnergy;
import net.rai.scp_unleashed.microhid.MicroHidItem;

public final class MicroHidHud {
    public static final int ChargeColour = 0xFF7CC4FF;
    public static final int LowColour = 0xFFF2B033;
    public static final int EmptyColour = 0xFFE5534B;
    public static final int FireColour = 0xFFD8F0FF;
    public static final int SpinColour = 0xFF5AA8FF;
    public static final int HintLift = 10;

    private MicroHidHud() {
    }

    public static void renderPanel(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || User == null || User.isSpectator() || Game.level == null) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        if (!MicroHidItem.isMicroHid(Stack)) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int OffHandShift = User.getMainArm() == HumanoidArm.LEFT && !User.getOffhandItem().isEmpty() ? 29 : 0;
        int Left = Graphics.guiWidth() / 2 + 91 + 8 + OffHandShift;
        int Top = Graphics.guiHeight() - GunHud.PanelHeight - 1;
        Font Text = Game.font;
        int Percent = MicroHidEnergy.percent(Stack);
        float Fill = MicroHidEnergy.fraction(Stack);
        int Colour = Percent <= 0 ? EmptyColour : Fill < MicroHidEnergy.LowShare ? LowColour : ChargeColour;
        RenderSystem.enableBlend();
        Graphics.blit(GunHud.HudTexture, Left, Top, 0.0F, 0.0F, GunHud.PanelWidth, GunHud.PanelHeight, GunHud.HudWidth, GunHud.HudHeight);
        Graphics.renderItem(Stack, Left + 3, Top + 3);
        Component Count = Component.literal(Percent + "%").withStyle(ChatFormatting.BOLD);
        Graphics.drawString(Text, Count, Left + 21, Top + 3, Colour, true);
        bar(Graphics, Left + 20, Top + 14, Fill);
        MicroHidEffects.Phase Current = MicroHidEffects.phaseOf(User);
        if (Current == MicroHidEffects.Phase.Spin) {
            float Progress = MicroHidEffects.spinProgress(User, Partial);
            Graphics.drawString(Text, Component.translatable("hud.scp_unleashed.micro_hid.spinning", Math.round(Progress * 100.0F)), Left + 4, Top - HintLift, SpinColour, true);
        } else if (Current == MicroHidEffects.Phase.Fire) {
            Graphics.drawString(Text, Component.translatable("hud.scp_unleashed.micro_hid.firing"), Left + 4, Top - HintLift, FireColour, true);
        } else if (Percent <= 0) {
            Graphics.drawString(Text, Component.translatable("hud.scp_unleashed.micro_hid.empty"), Left + 4, Top - HintLift, EmptyColour, true);
        }
        RenderSystem.disableBlend();
    }

    private static void bar(GuiGraphics Graphics, int X, int Y, float Progress) {
        Graphics.blit(GunHud.HudTexture, X, Y, GunHud.BarU, GunHud.BarV, GunHud.BarWidth, GunHud.BarHeight, GunHud.HudWidth, GunHud.HudHeight);
        int Filled = Mth.clamp(Math.round(Progress * GunHud.BarWidth), 0, GunHud.BarWidth);
        if (Filled > 0) {
            Graphics.blit(GunHud.HudTexture, X, Y, GunHud.BarU, GunHud.BarV + GunHud.BarHeight, Filled, GunHud.BarHeight, GunHud.HudWidth, GunHud.HudHeight);
        }
    }
}
