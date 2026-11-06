package net.rai.scp_unleashed.fission.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.DissolverMode;
import net.rai.scp_unleashed.fission.GasDissolverBlockEntity;
import net.rai.scp_unleashed.fission.GasDissolverMenu;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GasDissolverScreen extends AbstractContainerScreen<GasDissolverMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/gas_dissolver.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int InventoryLabelY = 72;
    public static final int TankX = 13;
    public static final int TankY = 19;
    public static final int TankWidth = 16;
    public static final int TankHeight = 48;
    public static final int ChamberX = 37;
    public static final int ChamberY = 19;
    public static final int ChamberWidth = 54;
    public static final int ChamberHeight = 48;
    public static final int WaterTop = 22;
    public static final int InletX = 59;
    public static final int InletWidth = 10;
    public static final int SpargerY = 61;
    public static final int IconX = 97;
    public static final int LcdX = 107;
    public static final int LcdWidth = 58;
    public static final int LcdHeight = 11;
    public static final int GasLcdY = 22;
    public static final int UseLcdY = 38;
    public static final int AreaLcdY = 54;
    public static final int BubbleU = 176;
    public static final int BubbleV = 0;
    public static final int BubbleSize = 3;
    public static final int SmallBubbleU = 180;
    public static final int SmallBubbleSize = 2;
    public static final int WispU = 176;
    public static final int WispV = 8;
    public static final int WispWidth = 6;
    public static final int WispHeight = 5;
    public static final int Bubbles = 9;
    public static final int Wisps = 3;
    public static final int LcdColour = 0xFF5CFF8A;
    public static final int LcdDimColour = 0xFF2A7A44;
    public static final int TitleColour = 0xFF404040;
    private enum HoverSpot {
        WaterTank(TankX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        GasReadout(IconX, GasLcdY, LcdX + LcdWidth - IconX, LcdHeight),
        WaterReadout(IconX, UseLcdY, LcdX + LcdWidth - IconX, LcdHeight),
        AreaReadout(IconX, AreaLcdY, LcdX + LcdWidth - IconX, LcdHeight),
        Chamber(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

        private final HoverArea Area;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.Area = new HoverArea(X, Y, Width, Height);
        }
    }

    private int Ticks;
    private float Intensity;
    private float PreviousIntensity;

    public GasDissolverScreen(GasDissolverMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = InventoryLabelY;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        PreviousIntensity = Intensity;
        float Target = menu.getStatus() == DissolverMode.Working ? 1.0f : 0.0f;
        Intensity += (Target - Intensity) * (Target > Intensity ? 0.1f : 0.05f);
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Fill = Math.round((float) menu.getWater() / GasDissolverBlockEntity.TankCapacity * TankHeight);
        if (Fill > 0) {
            FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, leftPos + TankX, topPos + TankY + TankHeight - Fill, TankWidth, Fill);
        }
        renderChamber(Graphics, PartialTick);
    }

    private void renderChamber(GuiGraphics Graphics, float PartialTick) {
        if (menu.getStatus() == DissolverMode.Empty) {
            return;
        }
        int Left = leftPos + ChamberX;
        int Top = topPos + ChamberY;
        int Surface = Top + WaterTop;
        int Bottom = Top + ChamberHeight;
        FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, Left, Surface, ChamberWidth, Bottom - Surface);
        float Shown = Mth.lerp(PartialTick, PreviousIntensity, Intensity);
        if (Shown <= 0.02f) {
            return;
        }
        float Time = Ticks + PartialTick;
        int Rise = topPos + SpargerY - Surface;
        Graphics.enableScissor(Left, Top, Left + ChamberWidth, Bottom);
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
        for (int Index = 0; Index < Bubbles; Index++) {
            float Speed = 0.55f + (Index * 37 % 11) * 0.06f;
            float Travel = (Time * Speed + Index * 13.7f) % Rise;
            int X = Left + 5 + (Index * 23 + 7) % (ChamberWidth - 10) + Math.round(Mth.sin(Time * 0.2f + Index) * 1.2f);
            int Y = topPos + SpargerY - Math.round(Travel);
            boolean Large = Index % 3 != 0;
            Graphics.blit(Texture, X, Y, Large ? BubbleU : SmallBubbleU, BubbleV, Large ? BubbleSize : SmallBubbleSize, Large ? BubbleSize : SmallBubbleSize);
        }
        for (int Index = 0; Index < Wisps; Index++) {
            float Life = (Time * 0.05f + Index / (float) Wisps) % 1.0f;
            int X = leftPos + InletX + (InletWidth - WispWidth) / 2 + Math.round(Mth.sin(Time * 0.15f + Index * 2.1f) * 2.0f);
            int Y = Top + 1 + Math.round(Life * (WaterTop - WispHeight));
            Graphics.setColor(1.0f, 1.0f, 1.0f, Shown * (1.0f - Life));
            Graphics.blit(Texture, X, Y, WispU, WispV, WispWidth, WispHeight);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        Graphics.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, TitleColour, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TitleColour, false);
        DissolverMode Status = menu.getStatus();
        Component Line = Component.translatable(Status.getTranslationKey());
        Graphics.drawString(font, Line, PanelWidth - 8 - font.width(Line), titleLabelY, statusColour(Status), false);
        int Rate = menu.getRate();
        lcd(Graphics, Component.translatable("gui.scp_unleashed.gas_dissolver.lcd.rate", Rate), GasLcdY, Rate > 0 ? LcdColour : LcdDimColour);
        int Use = menu.getWaterUse();
        lcd(Graphics, Component.translatable("gui.scp_unleashed.gas_dissolver.lcd.use", Use), UseLcdY, Use > 0 ? LcdColour : LcdDimColour);
        lcd(Graphics, Component.translatable("gui.scp_unleashed.gas_dissolver.lcd.area", (int) GasDissolverBlockEntity.ScrubRadius), AreaLcdY,
                menu.getAreaRate() > 0 ? LcdColour : LcdDimColour);
    }

    private int statusColour(DissolverMode Status) {
        return switch (Status) {
            case Working -> 0xFF207A20;
            case Idle -> 0xFF606060;
            case Empty -> Ticks % 20 < 12 ? 0xFFB01818 : 0xFF701010;
        };
    }

    private void lcd(GuiGraphics Graphics, Component Text, int Y, int Colour) {
        Graphics.drawString(font, Text, LcdX + LcdWidth - 2 - font.width(Text), Y + (LcdHeight - 7) / 2, Colour, false);
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
            case WaterTank -> {
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.water", menu.getWater(), GasDissolverBlockEntity.TankCapacity));
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.water_hint").withStyle(ChatFormatting.GRAY));
            }
            case GasReadout -> {
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.rate", menu.getRate()));
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.rate_split", menu.getNetworkRate(), menu.getAreaRate()).withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.rate_limit", GasDissolverBlockEntity.perSecond(GasDissolverBlockEntity.rateFor(menu.getUpgrades())))
                        .withStyle(ChatFormatting.DARK_GRAY));
                if (menu.getUpgrades() > 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.machine.speed_upgrades", menu.getUpgrades(), UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case WaterReadout -> {
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.use", menu.getWaterUse()));
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.use_hint", GasDissolverBlockEntity.formatAmount(GasDissolverBlockEntity.WaterPerGas))
                        .withStyle(ChatFormatting.GRAY));
            }
            case AreaReadout -> {
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.area", (int) GasDissolverBlockEntity.ScrubRadius));
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.area_rate", menu.getAreaRate()).withStyle(ChatFormatting.GRAY));
            }
            case Chamber -> {
                DissolverMode Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                Lines.add(Component.translatable("gui.scp_unleashed.gas_dissolver.chamber_hint").withStyle(ChatFormatting.GRAY));
            }
        }
        return Lines;
    }
}
