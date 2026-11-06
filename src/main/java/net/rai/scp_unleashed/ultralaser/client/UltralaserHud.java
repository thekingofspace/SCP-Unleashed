package net.rai.scp_unleashed.ultralaser.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunAmmo;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.client.GunHud;
import net.rai.scp_unleashed.ultralaser.UltralaserItem;
import net.rai.scp_unleashed.ultralaser.UltralaserScore;

public final class UltralaserHud {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/ultralaser/ultralaser_hud.png");
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 32;
    public static final int BarX = 20;
    public static final int BarY = 13;
    public static final int BarWidth = 38;
    public static final int BarHeight = 5;
    public static final int TroughV = 0;
    public static final int PrimeV = 5;
    public static final int ReadyV = 10;
    public static final int VentV = 15;
    public static final int NotchU = 0;
    public static final int NotchV = 20;
    public static final int NotchWidth = 1;
    public static final int NotchHeight = 7;
    public static final int BracketU = 2;
    public static final int BracketV = 20;
    public static final int BracketSize = 5;
    public static final int TealColour = 0xFF4FF0D2;
    public static final int PinkColour = 0xFFFF6FB8;
    public static final int HintLift = 10;
    public static final int[] NotchTicks = {UltralaserScore.CountTick, UltralaserScore.TwoTick, UltralaserScore.ThreeTick, UltralaserScore.ReadyTick};

    private UltralaserHud() {
    }

    public static void renderPanel(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || User == null || User.isSpectator() || Game.level == null) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        if (!UltralaserItem.isUltralaser(Stack) || GunItem.getReload(Stack) != null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int OffHandShift = User.getMainArm() == HumanoidArm.LEFT && !User.getOffhandItem().isEmpty() ? 29 : 0;
        int Left = Graphics.guiWidth() / 2 + 91 + 8 + OffHandShift;
        int Top = Graphics.guiHeight() - GunHud.PanelHeight - 1;
        int Reserve = User.getAbilities().instabuild ? -1 : GunAmmo.count(User, GunKind.Ultralaser);
        boolean HintShown = GunItem.getAmmo(Stack) == 0 && Reserve != 0;
        int LabelY = Top - HintLift - (HintShown ? HintLift : 0);
        Font Text = Game.font;
        GunTimer Charging = GunItem.getCharging(Stack);
        RenderSystem.enableBlend();
        if (Charging != null) {
            float Progress = UltralaserPrimingSound.primeProgress(User, Charging, Partial);
            int Ticks = (int) UltralaserPrimingSound.primeTicks(User, Charging, Partial);
            boolean Counting = Ticks >= UltralaserScore.CountTick;
            bar(Graphics, Left + BarX, Top + BarY, Progress, Counting ? ReadyV : PrimeV);
            for (int Tick : NotchTicks) {
                int Notch = Left + BarX + Math.round((BarWidth - 1) * Tick / (float) UltralaserScore.PrimeTicks);
                Graphics.blit(Texture, Notch, Top + BarY - 1, NotchU, NotchV, NotchWidth, NotchHeight, TextureWidth, TextureHeight);
            }
            String Key = Counting ? "hud.scp_unleashed.ultralaser.count" : "hud.scp_unleashed.ultralaser.priming";
            int Pulse = (int) Math.floor(Math.max(0.0F, Ticks - UltralaserScore.OriginTicks) / UltralaserScore.PulseTicks);
            int Colour = Counting && Pulse % 2 == 1 ? PinkColour : TealColour;
            Graphics.drawString(Text, Component.translatable(Key), Left + 4, LabelY, Colour, true);
        } else if (User.getCooldowns().isOnCooldown(Stack.getItem())) {
            float Heat = User.getCooldowns().getCooldownPercent(Stack.getItem(), Partial);
            bar(Graphics, Left + BarX, Top + BarY, Heat, VentV);
            Graphics.drawString(Text, Component.translatable("hud.scp_unleashed.ultralaser.venting"), Left + 4, LabelY, PinkColour, true);
        }
        RenderSystem.disableBlend();
    }

    private static void bar(GuiGraphics Graphics, int X, int Y, float Progress, int FillV) {
        Graphics.blit(Texture, X, Y, 0, TroughV, BarWidth, BarHeight, TextureWidth, TextureHeight);
        int Filled = Mth.clamp(Math.round(Progress * (BarWidth - 2)), 0, BarWidth - 2);
        if (Filled > 0) {
            Graphics.blit(Texture, X + 1, Y + 1, 1, FillV + 1, Filled, BarHeight - 2, TextureWidth, TextureHeight);
        }
    }

    public static void renderReticle(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || User == null || Game.level == null || !Game.options.getCameraType().isFirstPerson() || User.isSpectator()) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        GunTimer Charging = UltralaserItem.isUltralaser(Stack) ? GunItem.getCharging(Stack) : null;
        if (Charging == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Progress = UltralaserPrimingSound.primeProgress(User, Charging, Partial);
        int Ticks = (int) UltralaserPrimingSound.primeTicks(User, Charging, Partial);
        int Row = Ticks >= UltralaserScore.CountTick ? BracketSize : 0;
        int Gap = 3 + Math.round(10.0F * (1.0F - Progress));
        int CentreX = Graphics.guiWidth() / 2;
        int CentreY = Graphics.guiHeight() / 2;
        RenderSystem.enableBlend();
        corner(Graphics, CentreX - Gap - BracketSize, CentreY - Gap - BracketSize, 0, Row);
        corner(Graphics, CentreX + Gap + 1, CentreY - Gap - BracketSize, 1, Row);
        corner(Graphics, CentreX - Gap - BracketSize, CentreY + Gap + 1, 2, Row);
        corner(Graphics, CentreX + Gap + 1, CentreY + Gap + 1, 3, Row);
        RenderSystem.disableBlend();
    }

    private static void corner(GuiGraphics Graphics, int X, int Y, int Index, int Row) {
        Graphics.blit(Texture, X, Y, BracketU + Index * BracketSize, BracketV + Row, BracketSize, BracketSize, TextureWidth, TextureHeight);
    }
}
