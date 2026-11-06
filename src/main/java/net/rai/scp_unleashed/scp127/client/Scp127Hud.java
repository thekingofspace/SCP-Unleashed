package net.rai.scp_unleashed.scp127.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.client.GunClientState;
import net.rai.scp_unleashed.guns.client.GunHud;
import net.rai.scp_unleashed.scp127.Scp127Item;
import net.rai.scp_unleashed.scp127.Scp127Levels;

public final class Scp127Hud {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp127/scp127_hud.png");
    public static final ResourceLocation Layer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp127_panel");
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 32;
    public static final int EmblemSize = 16;
    public static final int UnlitU = 0;
    public static final int LitU = 16;
    public static final int GrowingU = 32;
    public static final int EmblemV = 0;
    public static final int ChevronLeft = 1;
    public static final int ChevronWidth = 14;
    public static final int ChevronHeight = 4;
    public static final int[] ChevronTops = {11, 6, 1};
    public static final int BarX = 20;
    public static final int BarY = 14;
    public static final int BarWidth = 36;
    public static final int BarHeight = 3;
    public static final int TroughV = 16;
    public static final int FillV = 19;
    public static final int HintLift = 10;
    public static final int HintColour = 0xFFE85840;

    private Scp127Hud() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (Game.options.hideGui || User == null || User.isSpectator() || Game.level == null) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        if (!Scp127Item.is(Stack)) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        long Now = Game.level.getGameTime();
        int OffHandShift = User.getMainArm() == HumanoidArm.LEFT && !User.getOffhandItem().isEmpty() ? 29 : 0;
        int Left = Graphics.guiWidth() / 2 + 91 + 8 + OffHandShift;
        int Top = Graphics.guiHeight() - GunHud.PanelHeight - 1;
        int Rank = Scp127Levels.rankOf(Stack);
        int X = Left + 3;
        int Y = Top + 3;
        RenderSystem.enableBlend();
        Graphics.blit(Texture, X, Y, UnlitU, EmblemV, EmblemSize, EmblemSize, TextureWidth, TextureHeight);
        for (int Index = 0; Index < Rank && Index < ChevronTops.length; Index++) {
            int Row = ChevronTops[Index];
            Graphics.blit(Texture, X, Y + Row, LitU, EmblemV + Row, EmblemSize, ChevronHeight, TextureWidth, TextureHeight);
        }
        if (Rank < Scp127Levels.MaxLevel) {
            int Row = ChevronTops[Rank];
            int Filled = Mth.clamp(Math.round(Scp127Levels.progress(Stack) * ChevronWidth), 0, ChevronWidth);
            if (Filled > 0) {
                Graphics.blit(Texture, X + ChevronLeft, Y + Row, GrowingU + ChevronLeft, EmblemV + Row, Filled, ChevronHeight, TextureWidth, TextureHeight);
            }
        }
        int Loaded = GunClientState.predictedAmmo(User, Stack);
        if (Loaded < Scp127Levels.capacity()) {
            bar(Graphics, Left + BarX, Top + BarY, Scp127Levels.regrowthProgress(Stack, Now, Partial));
        }
        if (Loaded == 0) {
            Graphics.drawString(Game.font, Component.translatable("hud.scp_unleashed.scp127.regrowing"), Left + 4, Top - HintLift, HintColour, true);
        }
        RenderSystem.disableBlend();
    }

    private static void bar(GuiGraphics Graphics, int X, int Y, float Progress) {
        Graphics.blit(Texture, X, Y, 0, TroughV, BarWidth, BarHeight, TextureWidth, TextureHeight);
        int Filled = Mth.clamp(Math.round(Progress * BarWidth), 0, BarWidth);
        if (Filled > 0) {
            Graphics.blit(Texture, X, Y, 0, FillV, Filled, BarHeight, TextureWidth, TextureHeight);
        }
    }
}
