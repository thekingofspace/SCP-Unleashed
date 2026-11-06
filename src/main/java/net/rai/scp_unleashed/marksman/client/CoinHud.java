package net.rai.scp_unleashed.marksman.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.client.GunHud;
import net.rai.scp_unleashed.marksman.CoinToss;
import net.rai.scp_unleashed.marksman.MarksmanRevolverItem;

public final class CoinHud {
    public static final ResourceLocation HudTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/marksman/coin_hud.png");
    public static final ResourceLocation CoinLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "marksman_coins");
    public static final int TextureSize = 32;
    public static final int TrayWidth = 24;
    public static final int TrayHeight = 22;
    public static final int TrayGap = 2;
    public static final int PipSize = 7;
    public static final int LitU = 0;
    public static final int EmptyU = 7;
    public static final int PipV = 22;
    public static final int[][] PipSlots = {{4, 3}, {13, 3}, {4, 11}, {13, 11}};

    private CoinHud() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || User == null || User.isSpectator() || Game.level == null) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        if (!(Stack.getItem() instanceof MarksmanRevolverItem)) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int OffHandShift = User.getMainArm() == HumanoidArm.LEFT && !User.getOffhandItem().isEmpty() ? 29 : 0;
        int Left = Graphics.guiWidth() / 2 + 91 + 8 + OffHandShift + GunHud.PanelWidth + TrayGap;
        int Top = Graphics.guiHeight() - GunHud.PanelHeight - 1;
        int Coins = CoinToss.getCoins(Stack);
        GunTimer Charge = CoinToss.getCharge(Stack);
        long Now = Game.level.getGameTime();
        RenderSystem.enableBlend();
        Graphics.blit(HudTexture, Left, Top, 0.0F, 0.0F, TrayWidth, TrayHeight, TextureSize, TextureSize);
        for (int Index = 0; Index < PipSlots.length && Index < CoinToss.MaxCoins; Index++) {
            int X = Left + PipSlots[Index][0];
            int Y = Top + PipSlots[Index][1];
            if (Index < Coins) {
                Graphics.blit(HudTexture, X, Y, LitU, PipV, PipSize, PipSize, TextureSize, TextureSize);
                continue;
            }
            Graphics.blit(HudTexture, X, Y, EmptyU, PipV, PipSize, PipSize, TextureSize, TextureSize);
            if (Index == Coins && Charge != null) {
                int Rows = Mth.clamp(Math.round(Charge.progress(Now, Partial) * PipSize), 0, PipSize);
                if (Rows > 0) {
                    Graphics.blit(HudTexture, X, Y + PipSize - Rows, LitU, PipV + PipSize - Rows, PipSize, Rows, TextureSize, TextureSize);
                }
            }
        }
        RenderSystem.disableBlend();
    }
}
