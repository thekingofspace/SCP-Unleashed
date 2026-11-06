package net.rai.scp_unleashed.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.space.AirCanisterItem;
import net.rai.scp_unleashed.space.SpaceAir;
import net.rai.scp_unleashed.space.SpaceRegistry;
import net.rai.scp_unleashed.space.SpaceSuitTank;
import net.rai.scp_unleashed.space.SpaceVacuum;

public final class SpaceAirHud {
    public static final ResourceLocation LayerId = SpaceRegistry.location("space_air_gauge");
    public static final ResourceLocation Texture = SpaceRegistry.location("textures/gui/space/air_gauge.png");
    public static final int TextureSize = 128;
    public static final int GaugeWidth = 81;
    public static final int GaugeHeight = 9;
    public static final int BarX = 10;
    public static final int BarY = 2;
    public static final int BarWidth = 70;
    public static final int BarHeight = 5;
    public static final int NormalV = 9;
    public static final int LowV = 14;
    public static final int CriticalV = 19;
    public static final int MissingV = 24;
    public static final int RowHeight = 10;

    private SpaceAirHud() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.options.hideGui || Client.gameMode == null || !Client.gameMode.canHurtPlayer()) {
            return;
        }
        ItemStack Chest = User.getItemBySlot(EquipmentSlot.CHEST);
        if (!SpaceAir.hasTank(Chest)) {
            return;
        }
        SpaceSuitTank Tank = SpaceAir.tank(Chest);
        int Left = Graphics.guiWidth() / 2 + 91 - GaugeWidth;
        int Top = Graphics.guiHeight() - Client.gui.rightHeight;
        Client.gui.rightHeight += RowHeight;
        RenderSystem.enableBlend();
        Graphics.blit(Texture, Left, Top, 0, 0, GaugeWidth, GaugeHeight, TextureSize, TextureSize);
        if (!Tank.Fitted()) {
            Graphics.blit(Texture, Left, Top, 0, MissingV, 9, 9, TextureSize, TextureSize);
            RenderSystem.disableBlend();
            return;
        }
        int Air = Tank.Air();
        int Filled = Math.round(BarWidth * Math.min(1.0F, Air / (float) AirCanisterItem.Capacity));
        if (Air > 0 && Filled <= 0) {
            Filled = 1;
        }
        int Row = Air <= SpaceVacuum.CriticalAir ? CriticalV : Air <= SpaceVacuum.LowAir ? LowV : NormalV;
        boolean Blink = Air <= SpaceVacuum.CriticalAir && (User.tickCount / 6) % 2 == 0;
        if (Filled > 0) {
            if (Blink) {
                Graphics.setColor(1.0F, 1.0F, 1.0F, 0.45F);
            }
            Graphics.blit(Texture, Left + BarX, Top + BarY, 0, Row, Filled, BarHeight, TextureSize, TextureSize);
            Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        RenderSystem.disableBlend();
    }
}
