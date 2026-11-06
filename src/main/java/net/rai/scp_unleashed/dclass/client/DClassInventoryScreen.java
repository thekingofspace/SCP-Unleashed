package net.rai.scp_unleashed.dclass.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassInventoryMenu;
import net.rai.scp_unleashed.dclass.DClassNumbers;

public class DClassInventoryScreen extends AbstractContainerScreen<DClassInventoryMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/dclass/dclass_inventory.png");
    public static final ResourceLocation HeartSprite = ResourceLocation.withDefaultNamespace("hud/heart/full");
    public static final ResourceLocation FoodSprite = ResourceLocation.withDefaultNamespace("hud/food_full");
    public static final ResourceLocation ArmorSprite = ResourceLocation.withDefaultNamespace("hud/armor_full");
    public static final int PortraitX = 26;
    public static final int PortraitY = 8;
    public static final int PortraitWidth = 50;
    public static final int PortraitHeight = 70;
    public static final int InfoX = 100;
    public static final int InfoY = 10;
    public static final int InfoWidth = 68;
    public static final int InfoLine = 11;
    public static final int GridLabelY = 81;
    public static final int PlayerLabelY = 170;
    public static final int ImageHeight = 264;
    public static final int TextureWidth = 256;
    public static final int TextureHeight = 512;
    public static final int Ink = 0xFF3A3F45;
    public static final int Readout = 0xFF8CFFB4;
    public static final int ReadoutDim = 0xFF4E9A6E;
    private static final String Prefix = "gui.scp_unleashed.dclass_inventory.";

    public DClassInventoryScreen(DClassInventoryMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = ImageHeight;
        inventoryLabelY = PlayerLabelY;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float Partial, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0.0f, 0.0f, imageWidth, imageHeight, TextureWidth, TextureHeight);
        DClass Subject = menu.subject();
        if (Subject != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(Graphics, leftPos + PortraitX, topPos + PortraitY, leftPos + PortraitX + PortraitWidth,
                    topPos + PortraitY + PortraitHeight, 30, 0.0625f, MouseX, MouseY, Subject);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        DClass Subject = menu.subject();
        int X = InfoX;
        int Y = InfoY;
        String Designation = Subject == null ? DClassNumbers.format(0) : font.plainSubstrByWidth(Subject.designation(), InfoWidth);
        Graphics.drawString(font, Designation, X, Y, Readout, false);
        Y += InfoLine;
        if (Subject != null) {
            Graphics.drawString(font, font.plainSubstrByWidth(Subject.trait().getName().getString(), InfoWidth), X, Y, ReadoutDim, false);
        }
        Y += InfoLine + 2;
        stat(Graphics, HeartSprite, X, Y, String.format("%.0f/%.0f", menu.data(DClassInventoryMenu.HealthData) / 10.0f, menu.data(DClassInventoryMenu.MaxHealthData) / 10.0f));
        Y += InfoLine;
        stat(Graphics, FoodSprite, X, Y, menu.data(DClassInventoryMenu.FoodData) + "/20");
        Y += InfoLine;
        stat(Graphics, ArmorSprite, X, Y, String.valueOf(menu.data(DClassInventoryMenu.ArmorData)));
        Y += InfoLine;
        Graphics.drawString(font, Component.translatable(Prefix + "level", menu.data(DClassInventoryMenu.LevelData)), X, Y, ReadoutDim, false);
        Graphics.drawString(font, Component.translatable(Prefix + "effects"), 8, GridLabelY, Ink, false);
        Graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, Ink, false);
    }

    private void stat(GuiGraphics Graphics, ResourceLocation Sprite, int X, int Y, String Value) {
        Graphics.blitSprite(Sprite, X, Y - 1, 9, 9);
        Graphics.drawString(font, Value, X + 12, Y, Readout, false);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        renderTooltip(Graphics, MouseX, MouseY);
    }
}
