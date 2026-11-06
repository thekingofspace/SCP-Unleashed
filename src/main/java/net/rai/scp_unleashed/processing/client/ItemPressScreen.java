package net.rai.scp_unleashed.processing.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.processing.ItemPressMenu;
import net.rai.scp_unleashed.processing.PressDie;

import java.util.List;

public class ItemPressScreen extends ProcessingScreen<ItemPressMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/item_press.png");
    public static final int RodX = 85;
    public static final int RodTop = 28;
    public static final int RodWidth = 6;
    public static final int RodU = 176;
    public static final int RodV = 0;
    public static final int HeadX = 77;
    public static final int HeadTop = 30;
    public static final int HeadWidth = 22;
    public static final int HeadHeight = 8;
    public static final int HeadU = 182;
    public static final int HeadV = 0;
    public static final int Travel = 14;
    public static final int StrokeX = 77;
    public static final int StrokeY = 28;
    public static final int StrokeWidth = 22;
    public static final int StrokeHeight = 28;
    public static final int FillU = 176;
    public static final int FillV = 30;
    public static final int GaugeX = 19;
    public static final int GaugeY = 21;
    public static final int GaugeWidth = 4;
    public static final int GaugeHeight = 38;
    public static final int GaugeU = 192;
    public static final int GaugeV = 30;
    public static final int LampU = 200;
    public static final int LampV = 30;
    public static final int StampFullU = 222;
    public static final int StampV = 40;
    public static final int StampWidth = 22;
    public static final int StampHeight = 28;
    public static final int DieButtonX = 117;
    public static final int DieButtonY = 55;
    public static final int DieButtonWidth = 20;
    public static final int DieButtonHeight = 14;
    public static final int DieButtonU = 176;
    public static final int DieButtonHoverU = 196;
    public static final int DieButtonV = 84;
    public static final int DieIconX = 4;
    public static final int DieIconY = 2;
    public static final int DieIconU = 216;
    public static final int DieIconV = 84;
    public static final int DieIconWidth = 12;
    public static final int DieIconHeight = 10;

    public ItemPressScreen(ItemPressMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title, Texture, FillU, FillV, LampU, LampV);
    }

    @Override
    protected void renderMachine(GuiGraphics Graphics, float PartialTick) {
        float Progress = menu.getProgress();
        int Drop = Math.round(Progress * Travel);
        int RodLength = HeadTop + Drop - RodTop;
        if (RodLength > 0) {
            Graphics.blit(Texture, leftPos + RodX, topPos + RodTop, RodU, RodV, RodWidth, RodLength);
        }
        Graphics.blit(Texture, leftPos + HeadX, topPos + HeadTop + Drop, HeadU, HeadV, HeadWidth, HeadHeight);
        int Pressure = Math.round(Progress * GaugeHeight);
        if (Pressure > 0) {
            Graphics.blit(Texture, leftPos + GaugeX, topPos + GaugeY + GaugeHeight - Pressure, GaugeU, GaugeV + GaugeHeight - Pressure, GaugeWidth, Pressure);
        }
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        super.renderBg(Graphics, PartialTick, MouseX, MouseY);
        boolean Hovered = isHovering(DieButtonX, DieButtonY, DieButtonWidth, DieButtonHeight, MouseX, MouseY);
        Graphics.blit(Texture, leftPos + DieButtonX, topPos + DieButtonY, Hovered ? DieButtonHoverU : DieButtonU, DieButtonV, DieButtonWidth, DieButtonHeight);
        Graphics.blit(Texture, leftPos + DieButtonX + DieIconX, topPos + DieButtonY + DieIconY, DieIconU + menu.getDie().ordinal() * DieIconWidth, DieIconV, DieIconWidth, DieIconHeight);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        if (isHovering(DieButtonX, DieButtonY, DieButtonWidth, DieButtonHeight, MouseX, MouseY)) {
            PressDie Die = menu.getDie();
            Graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("container.scp_unleashed.item_press.die", Component.translatable(Die.getTranslationKey())),
                    Component.translatable("container.scp_unleashed.item_press.die.switch").withStyle(ChatFormatting.GRAY),
                    Component.translatable("container.scp_unleashed.item_press.die.hint").withStyle(ChatFormatting.DARK_GRAY)), MouseX, MouseY);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0 && isHovering(DieButtonX, DieButtonY, DieButtonWidth, DieButtonHeight, MouseX, MouseY) && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ItemPressMenu.CycleDieButton);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    protected boolean isOverProgress(int MouseX, int MouseY) {
        return isHovering(StrokeX, StrokeY, StrokeWidth, StrokeHeight, MouseX, MouseY) || isHovering(GaugeX - 2, GaugeY - 2, GaugeWidth + 4, GaugeHeight + 4, MouseX, MouseY);
    }

    @Override
    protected boolean isOverRecipeArea(int MouseX, int MouseY) {
        return isHovering(StrokeX, StrokeY, StrokeWidth, StrokeHeight, MouseX, MouseY);
    }

    @Override
    protected String getActionKey() {
        return "container.scp_unleashed.item_press.progress";
    }
}
