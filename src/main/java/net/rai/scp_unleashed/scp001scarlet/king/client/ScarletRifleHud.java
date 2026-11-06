package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.client.GunClientState;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletRifleItem;

public final class ScarletRifleHud {
    public static final int Frame = 0xFF2A0A0E;
    public static final int Empty = 0xFF3C1216;
    public static final int Cell = 0xFFE0303A;
    public static final int Bright = 0xFFFF7A5C;
    public static final int Recharge = 0xFFFF9A3C;
    public static final int Label = 0xFFF2C8B8;
    public static final int Segments = 12;

    private ScarletRifleHud() {
    }

    public static void render(GuiGraphics Graphics, Font Text, int Left, int Top, ItemStack Stack, long Now, float Partial) {
        Minecraft Game = Minecraft.getInstance();
        int Loaded = Game.player == null ? GunItem.getAmmo(Stack) : GunClientState.predictedAmmo(Game.player, Stack);
        GunTimer Forced = GunItem.getReload(Stack);
        float Share = Forced != null ? Forced.progress(Now, Partial) : Mth.clamp(Loaded / (float) ScarletRifleItem.Capacity, 0.0f, 1.0f);
        Graphics.drawString(Text, Component.translatable("hud.scp_unleashed.scarlet_rifle.cell"), Left + 4, Top + 3, Label, true);
        String Count = Forced != null ? "--" : Integer.toString(Loaded);
        Graphics.drawString(Text, Count, Left + 56 - Text.width(Count), Top + 3, Loaded > 0 ? Bright : Empty, true);
        int BarLeft = Left + 4;
        int BarTop = Top + 13;
        int SegmentWidth = 3;
        Graphics.fill(BarLeft - 1, BarTop - 1, BarLeft + Segments * (SegmentWidth + 1), BarTop + 5, Frame);
        int Lit = Math.round(Share * Segments);
        for (int Index = 0; Index < Segments; Index++) {
            int X = BarLeft + Index * (SegmentWidth + 1);
            int Colour = Index < Lit ? Forced != null ? Recharge : Index == Lit - 1 ? Bright : Cell : Empty;
            Graphics.fill(X, BarTop, X + SegmentWidth, BarTop + 4, Colour);
        }
        if (Forced != null) {
            Graphics.drawString(Text, Component.translatable("hud.scp_unleashed.scarlet_rifle.recharging"), Left + 4, Top - 10, Recharge, true);
        }
    }
}
