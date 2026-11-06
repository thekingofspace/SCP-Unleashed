package net.rai.scp_unleashed.labware.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.labware.LabViralStorageBlockEntity;
import net.rai.scp_unleashed.labware.LabViralStorageMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LabViralStorageScreen extends AbstractContainerScreen<LabViralStorageMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/labware/viral_storage.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 192;
    public static final int TitleX = 10;
    public static final int TitleY = 7;
    public static final int InventoryLabelY = 101;
    public static final int GaugeX = 10;
    public static final int GaugeY = 27;
    public static final int GaugeWidth = 10;
    public static final int GaugeHeight = 52;
    public static final int ColdU = 176;
    public static final int WarmU = 186;
    public static final int FillV = 0;
    public static final int LampX = 160;
    public static final int LampY = 7;
    public static final int LampU = 176;
    public static final int LampV = 64;
    public static final int LampSize = 6;
    public static final int LampStep = 7;
    public static final int LampBlue = 5;
    public static final int LampAmber = 2;
    public static final int LampRed = 3;
    public static final int RackX = 35;
    public static final int RackY = 25;
    public static final int RackSize = 64;
    public static final int LcdX = 102;
    public static final int LcdY = 27;
    public static final int LcdWidth = 66;
    public static final int LcdHeight = 48;
    public static final int LcdPitch = 12;
    public static final int TitleColour = 0xFFE4E2D6;
    public static final int EngraveColour = 0xFF8E97A2;
    public static final int LcdColour = 0xFF8CFFA0;
    public static final int LcdDim = 0xFF3F7A4C;
    public static final int ColdColour = 0xFF7FD4FF;
    public static final int WarmColour = 0xFFFFC05A;
    public static final int AlarmColour = 0xFFFF5A4A;
    public static final int StandbyColour = 0xFFA9B0B8;

    private enum HoverSpot {
        Gauge(GaugeX - 2, GaugeY - 2, GaugeWidth + 4, GaugeHeight + 4),
        Lamp(LampX - 2, LampY - 2, LampSize + 4, LampSize + 4),
        Readout(LcdX - 2, LcdY - 2, LcdWidth + 4, LcdHeight + 4),
        Rack(RackX, RackY, RackSize, RackSize);

        private final LabHoverArea Area;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.Area = new LabHoverArea(X, Y, Width, Height);
        }
    }

    private int Ticks;

    public LabViralStorageScreen(LabViralStorageMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        titleLabelX = TitleX;
        titleLabelY = TitleY;
        inventoryLabelY = InventoryLabelY;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Column = menu.isPowered() ? ColdU : WarmU;
        Graphics.blit(Texture, leftPos + GaugeX, topPos + GaugeY, Column, FillV, GaugeWidth, GaugeHeight);
        int Lamp = lampIndex();
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampStep, LampV, LampSize, LampSize);
        }
    }

    private int lampIndex() {
        if (menu.isPowered()) {
            return menu.isStocked() ? LampBlue : -1;
        }
        if (menu.isBreached()) {
            return Ticks % 10 < 6 ? LampRed : -1;
        }
        return LampAmber;
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, TitleColour, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, EngraveColour, false);
        int Top = LcdY + 3;
        Component Status = Component.translatable(statusKey());
        Graphics.drawString(font, Status, LcdX + 3, Top, statusColour(), false);
        line(Graphics, Component.translatable("gui.scp_unleashed.lab_viral_storage.lcd.temp"), tempValue(), Top + LcdPitch, menu.isPowered() ? LcdColour : menu.isBreached() ? AlarmColour : WarmColour);
        line(Graphics, Component.translatable("gui.scp_unleashed.lab_viral_storage.lcd.power"),
                Component.literal(menu.getReceived() + "/" + menu.getDemand()), Top + LcdPitch * 2, menu.isStocked() && menu.isPowered() ? LcdColour : LcdDim);
        int Cultures = menu.getCultures();
        line(Graphics, Component.translatable("gui.scp_unleashed.lab_viral_storage.lcd.cultures"), Component.literal(Integer.toString(Cultures)), Top + LcdPitch * 3,
                Cultures > 0 ? WarmColour : LcdColour);
    }

    private void line(GuiGraphics Graphics, Component Label, Component Value, int Y, int Colour) {
        Graphics.drawString(font, Label, LcdX + 3, Y, LcdDim, false);
        Graphics.drawString(font, Value, LcdX + LcdWidth - 3 - font.width(Value), Y, Colour, false);
    }

    private String statusKey() {
        if (menu.isPowered()) {
            return menu.isStocked() ? "gui.scp_unleashed.lab_viral_storage.status.chilled" : "gui.scp_unleashed.lab_viral_storage.status.standby";
        }
        return menu.isBreached() ? "gui.scp_unleashed.lab_viral_storage.status.breached" : "gui.scp_unleashed.lab_viral_storage.status.unpowered";
    }

    private int statusColour() {
        if (menu.isPowered()) {
            return menu.isStocked() ? ColdColour : StandbyColour;
        }
        if (menu.isBreached()) {
            return Ticks % 10 < 6 ? AlarmColour : 0xFF8A2A22;
        }
        return WarmColour;
    }

    private Component tempValue() {
        return Component.translatable(menu.isPowered() ? "gui.scp_unleashed.lab_viral_storage.lcd.cold" : "gui.scp_unleashed.lab_viral_storage.lcd.warm");
    }

    @Nullable
    private static HoverSpot spotAt(double GuiX, double GuiY) {
        for (HoverSpot Spot : HoverSpot.values()) {
            if (Spot.Area.contains(GuiX, GuiY)) {
                return Spot;
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            renderTooltip(Graphics, MouseX, MouseY);
            return;
        }
        HoverSpot Spot = spotAt(MouseX - leftPos, MouseY - topPos);
        if (Spot != null) {
            Graphics.renderComponentTooltip(font, tooltipFor(Spot), MouseX, MouseY);
        }
    }

    private List<Component> tooltipFor(HoverSpot Spot) {
        List<Component> Lines = new ArrayList<>();
        switch (Spot) {
            case Gauge, Lamp -> holdLines(Lines);
            case Readout -> {
                holdLines(Lines);
                Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.power", menu.getReceived(), menu.getDemand()).withStyle(ChatFormatting.GRAY));
                cultureLines(Lines);
            }
            case Rack -> {
                Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.rack", LabViralStorageBlockEntity.SlotCount));
                cultureLines(Lines);
                Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.rack_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return Lines;
    }

    private void holdLines(List<Component> Lines) {
        if (menu.isPowered() && !menu.isStocked()) {
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.standby").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.standby_hint", LabViralStorageBlockEntity.PowerDraw).withStyle(ChatFormatting.DARK_GRAY));
        } else if (menu.isPowered()) {
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.chilled").withStyle(ChatFormatting.AQUA));
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.chilled_hint").withStyle(ChatFormatting.GRAY));
        } else if (menu.isBreached()) {
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.breached").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.breached_hint", (int) LabExposure.Radius).withStyle(ChatFormatting.GRAY));
        } else {
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.unpowered").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("gui.scp_unleashed.lab_viral_storage.unpowered_hint").withStyle(ChatFormatting.GRAY));
        }
    }

    private void cultureLines(List<Component> Lines) {
        int Cultures = menu.getCultures();
        Lines.add(Cultures > 0 ? Component.translatable("gui.scp_unleashed.lab_viral_storage.cultures", Cultures).withStyle(ChatFormatting.GOLD)
                : Component.translatable("gui.scp_unleashed.lab_viral_storage.no_cultures").withStyle(ChatFormatting.GRAY));
    }
}
