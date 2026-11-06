package net.rai.scp_unleashed.rocketry.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.client.FissionGuiFluids;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.oil.OilRegistry;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.platform.LaunchCheck;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformKind;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformMenu;
import net.rai.scp_unleashed.rocketry.platform.LaunchStatus;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestination;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestinations;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LaunchPlatformScreen extends AbstractContainerScreen<LaunchPlatformMenu> {
    public static final ResourceLocation RocketTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/rocketry/rocket_platform.png");
    public static final ResourceLocation SatelliteTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/rocketry/satellite_platform.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int TallPanelHeight = 188;
    public static final int TankX = 9;
    public static final int TankY = 19;
    public static final int TankWidth = 16;
    public static final int TankHeight = 50;
    public static final int ScreenX = 30;
    public static final int ScreenY = 16;
    public static final int ScreenWidth = 94;
    public static final int ScreenHeight = 41;
    public static final int CellX = 33;
    public static final int CellY = 19;
    public static final int CellWidth = 46;
    public static final int CellHeight = 12;
    public static final int LedSize = 5;
    public static final int LedU = 176;
    public static final int LedV = 0;
    public static final int ButtonX = 30;
    public static final int ButtonY = 60;
    public static final int TallButtonY = 80;
    public static final int ButtonWidth = 94;
    public static final int ButtonHeight = 12;
    public static final int ButtonU = 0;
    public static final int ButtonV = 208;
    public static final int SelectorY = 59;
    public static final int SelectorHeight = 18;
    public static final int PreviousX = 30;
    public static final int NextX = 112;
    public static final int ArrowWidth = 12;
    public static final int ArrowU = 208;
    public static final int ArrowV = 0;
    public static final int DisplayX = 43;
    public static final int DisplayWidth = 68;
    public static final int DestinationIconX = 44;
    public static final int DestinationIconY = 60;
    public static final int DestinationIconSize = 16;
    public static final int NameX = 62;
    public static final int NameY = 64;
    public static final int BayX = 127;
    public static final int BayY = 16;
    public static final int BayWidth = 20;
    public static final int BayHeight = 42;
    public static final int IconU = 176;
    public static final int IconV = 8;
    public static final int IconWidth = 14;
    public static final int IconHeight = 32;
    public static final int IconX = 130;
    public static final int IconY = 21;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 80;
    public static final int LampSize = 6;
    public static final int LampU = 192;
    public static final int LampV = 80;
    public static final int LampX = 134;
    public static final int LampY = 62;
    public static final int TallLampY = 82;
    public static final int LabelY = 74;
    public static final int TallLabelY = 96;
    public static final int ScreenText = 0x9CF0B0;
    public static final int ScreenDim = 0x4E7A5C;
    public static final int ScreenGrey = 0x6A7270;
    public static final int ButtonText = 0xFFF4E0;
    public static final int ButtonDimText = 0x8A8F94;

    private int Ticks;

    public LaunchPlatformScreen(LaunchPlatformMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = tall() ? TallPanelHeight : PanelHeight;
        inventoryLabelY = tall() ? TallLabelY : LabelY;
    }

    private boolean tall() {
        return menu.getKind() == LaunchPlatformKind.Rocket;
    }

    private int buttonY() {
        return tall() ? TallButtonY : ButtonY;
    }

    private int lampY() {
        return tall() ? TallLampY : LampY;
    }

    private ResourceLocation texture() {
        return tall() ? RocketTexture : SatelliteTexture;
    }

    private String key() {
        return "gui.scp_unleashed." + menu.getKind().getName();
    }

    @Nullable
    private RocketDestinations.Entry selected() {
        return tall() ? RocketDestinations.clientAt(menu.getDestination()) : null;
    }

    private int fuelCost() {
        RocketDestinations.Entry Entry = selected();
        return Entry == null ? menu.getKind().getFuelCost() : Entry.Destination().Fuel();
    }

    private int powerCost() {
        RocketDestinations.Entry Entry = selected();
        return Entry == null ? menu.getKind().getPowerCost() : Entry.Destination().Power();
    }

    private int choiceCount() {
        Level World = Minecraft.getInstance().level;
        int Count = 0;
        for (RocketDestinations.Entry Entry : RocketDestinations.client()) {
            if (World == null || !Entry.Destination().Dimension().equals(World.dimension())) {
                Count++;
            }
        }
        return Count;
    }

    private boolean canCycle() {
        return tall() && choiceCount() > 1 && menu.getStatus() != LaunchStatus.Launching;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
    }

    private HoverArea cellArea(int Index) {
        return new HoverArea(CellX + Index % 2 * CellWidth - 1, CellY + Index / 2 * CellHeight - 1, CellWidth, CellHeight);
    }

    private HoverArea buttonArea() {
        return new HoverArea(ButtonX, buttonY(), ButtonWidth, ButtonHeight);
    }

    private HoverArea arrowArea(boolean Next) {
        return new HoverArea(Next ? NextX : PreviousX, SelectorY, ArrowWidth, SelectorHeight);
    }

    private boolean hovered(HoverArea Area, double MouseX, double MouseY) {
        return Area.contains(MouseX - leftPos, MouseY - topPos);
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        ResourceLocation Texture = texture();
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        LaunchPlatformKind Kind = menu.getKind();
        int Fill = Math.round(Mth.clamp(menu.getFuel() / (float) Kind.getFuelCapacity(), 0.0f, 1.0f) * TankHeight);
        if (Fill > 0) {
            FissionGuiFluids.drawFluid(Graphics, OilRegistry.HeavyFuel.getSource(), leftPos + TankX, topPos + TankY + TankHeight - Fill, TankWidth, Fill);
        }
        int Power = Math.round(Mth.clamp(menu.getPower() / (float) Math.max(1, powerCost()), 0.0f, 1.0f) * BarHeight);
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
        List<LaunchCheck> Checks = Kind.getChecks();
        for (int Index = 0; Index < Checks.size(); Index++) {
            boolean Passed = menu.passes(Checks.get(Index));
            int X = leftPos + CellX + Index % 2 * CellWidth;
            int Y = topPos + CellY + Index / 2 * CellHeight + 3;
            Graphics.blit(Texture, X, Y, LedU + (Passed ? 0 : LedSize), LedV, LedSize, LedSize);
        }
        LaunchStatus Status = menu.getStatus();
        if (tall()) {
            boolean Present = menu.passes(LaunchCheck.Payload);
            Graphics.blit(Texture, leftPos + IconX, topPos + IconY, IconU + (Present ? IconWidth : 0), IconV, IconWidth, IconHeight);
            renderSelector(Graphics, Texture, MouseX, MouseY);
        }
        boolean Ready = Status == LaunchStatus.Ready;
        int Row = !Ready ? 2 : hovered(buttonArea(), MouseX, MouseY) ? 1 : 0;
        Graphics.blit(Texture, leftPos + ButtonX, topPos + buttonY(), ButtonU, ButtonV + Row * ButtonHeight, ButtonWidth, ButtonHeight);
        int Lamp = switch (Status.getLamp()) {
            case 1 -> Status == LaunchStatus.Launching ? (Ticks % 10 < 5 ? 0 : -1) : 0;
            case 2 -> Ticks % 20 < 12 ? 1 : -1;
            default -> -1;
        };
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + lampY(), LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
    }

    private void renderSelector(GuiGraphics Graphics, ResourceLocation Texture, int MouseX, int MouseY) {
        boolean Active = canCycle();
        for (int Side = 0; Side < 2; Side++) {
            boolean Next = Side == 1;
            int Row = !Active ? 2 : hovered(arrowArea(Next), MouseX, MouseY) ? 1 : 0;
            Graphics.blit(Texture, leftPos + (Next ? NextX : PreviousX), topPos + SelectorY, ArrowU + Side * ArrowWidth, ArrowV + Row * SelectorHeight, ArrowWidth, SelectorHeight);
        }
        RocketDestinations.Entry Entry = selected();
        if (Entry == null) {
            return;
        }
        if (!Entry.Available()) {
            Graphics.setColor(0.45f, 0.45f, 0.45f, 1.0f);
        }
        Graphics.blit(Entry.Destination().Icon(), leftPos + DestinationIconX, topPos + DestinationIconY, 0, 0, DestinationIconSize, DestinationIconSize,
                DestinationIconSize, DestinationIconSize);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        super.renderLabels(Graphics, MouseX, MouseY);
        List<LaunchCheck> Checks = menu.getKind().getChecks();
        for (int Index = 0; Index < Checks.size(); Index++) {
            LaunchCheck Check = Checks.get(Index);
            int X = CellX + Index % 2 * CellWidth + LedSize + 2;
            int Y = CellY + Index / 2 * CellHeight + 2;
            Graphics.drawString(font, Component.translatable(labelKey(Check)), X, Y, menu.passes(Check) ? ScreenText : ScreenDim, false);
        }
        if (tall()) {
            RocketDestinations.Entry Entry = selected();
            Component Name = Entry == null ? Component.translatable("gui.scp_unleashed.rocketry.destination.none") : Entry.Destination().Name();
            int Colour = Entry != null && Entry.Available() ? ScreenText : ScreenGrey;
            int Room = DisplayX + DisplayWidth - NameX - 2;
            String Shown = font.plainSubstrByWidth(Name.getString(), Room);
            Graphics.drawString(font, Shown, Entry == null ? DisplayX + 3 : NameX, NameY, Colour, false);
        }
        LaunchStatus Status = menu.getStatus();
        Component Label = Component.translatable(buttonKey(Status));
        int Width = font.width(Label);
        boolean Ready = Status == LaunchStatus.Ready;
        Graphics.drawString(font, Label, ButtonX + (ButtonWidth - Width) / 2, buttonY() + 2, Ready ? ButtonText : ButtonDimText, Ready);
    }

    private String labelKey(LaunchCheck Check) {
        if (Check == LaunchCheck.Payload) {
            return key() + ".payload";
        }
        return Check.labelKey();
    }

    private String buttonKey(LaunchStatus Status) {
        return switch (Status) {
            case Ready -> "gui.scp_unleashed.rocketry.button.launch";
            case Launching -> "gui.scp_unleashed.rocketry.button.launching";
            case Empty -> key() + ".button.empty";
            default -> "gui.scp_unleashed.rocketry.button.not_ready";
        };
    }

    private void click(int Button, boolean Allowed) {
        Minecraft Client = Minecraft.getInstance();
        if (Allowed && Client.gameMode != null) {
            Client.gameMode.handleInventoryButtonClick(menu.containerId, Button);
            Client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, Button == LaunchPlatformMenu.LaunchButton ? 0.8f : 1.2f));
        } else {
            Client.getSoundManager().play(SimpleSoundInstance.forUI(RocketryRegistry.Refused.get(), 1.0f, 0.7f));
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            if (hovered(buttonArea(), MouseX, MouseY)) {
                click(LaunchPlatformMenu.LaunchButton, menu.isReady());
                return true;
            }
            if (tall() && hovered(arrowArea(false), MouseX, MouseY)) {
                click(LaunchPlatformMenu.PreviousButton, canCycle());
                return true;
            }
            if (tall() && hovered(arrowArea(true), MouseX, MouseY)) {
                click(LaunchPlatformMenu.NextButton, canCycle());
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
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
        List<Component> Lines = tooltipAt(MouseX - leftPos, MouseY - topPos);
        if (Lines != null && !Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    @Nullable
    private List<Component> tooltipAt(double GuiX, double GuiY) {
        LaunchPlatformKind Kind = menu.getKind();
        List<Component> Lines = new ArrayList<>();
        if (new HoverArea(TankX - 1, TankY - 1, TankWidth + 2, TankHeight + 2).contains(GuiX, GuiY)) {
            int Amount = menu.getFuel();
            if (Amount <= 0) {
                Lines.add(Component.translatable("gui.scp_unleashed.oil.tank_empty", Component.translatable("gui.scp_unleashed.rocketry.fuel"), Kind.getFuelCapacity()));
            } else {
                Lines.add(Component.translatable("gui.scp_unleashed.oil.tank", new FluidStack(OilRegistry.HeavyFuel.getSource(), 1).getHoverName(), Amount, Kind.getFuelCapacity()));
            }
            Lines.add(Component.translatable("gui.scp_unleashed.rocketry.fuel_cost", fuelCost()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(key() + ".fuel_hint").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        if (new HoverArea(BarX - 1, BarY - 1, BarWidth + 2, BarHeight + 2).contains(GuiX, GuiY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.rocketry.charge", menu.getPower(), powerCost()));
            Lines.add(Component.translatable("gui.scp_unleashed.rocketry.charging", menu.getReceived(), Kind.getChargeRate()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(key() + ".power_hint").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        List<LaunchCheck> Checks = Kind.getChecks();
        for (int Index = 0; Index < Checks.size(); Index++) {
            if (cellArea(Index).contains(GuiX, GuiY)) {
                return checkTooltip(Checks.get(Index));
            }
        }
        if (tall() && new HoverArea(PreviousX, SelectorY, NextX + ArrowWidth - PreviousX, SelectorHeight).contains(GuiX, GuiY)) {
            return destinationTooltip(arrowArea(false).contains(GuiX, GuiY) || arrowArea(true).contains(GuiX, GuiY));
        }
        if (buttonArea().contains(GuiX, GuiY) || new HoverArea(LampX - 1, lampY() - 1, LampSize + 2, LampSize + 2).contains(GuiX, GuiY)) {
            LaunchStatus Status = menu.getStatus();
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            Lines.add(Component.translatable("gui.scp_unleashed.rocketry.cost", fuelCost(), powerCost()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(key() + ".button_hint").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        if (tall() && new HoverArea(BayX, BayY, BayWidth, BayHeight).contains(GuiX, GuiY)) {
            return checkTooltip(LaunchCheck.Payload);
        }
        return null;
    }

    private List<Component> destinationTooltip(boolean OnArrow) {
        List<Component> Lines = new ArrayList<>();
        RocketDestinations.Entry Entry = selected();
        if (Entry == null) {
            Lines.add(Component.translatable("gui.scp_unleashed.rocketry.destination.none").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        RocketDestination Shown = Entry.Destination();
        Lines.add(Component.translatable("gui.scp_unleashed.rocketry.destination", Shown.Name()).withStyle(Entry.Available() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        Lines.add(Component.translatable("gui.scp_unleashed.rocketry.cost", Shown.Fuel(), Shown.Power()).withStyle(ChatFormatting.GRAY));
        if (!Entry.Available()) {
            Lines.add(Component.translatable("gui.scp_unleashed.rocketry.destination.missing").withStyle(ChatFormatting.RED));
        }
        if (OnArrow) {
            Lines.add(Component.translatable(canCycle() ? "gui.scp_unleashed.rocketry.destination.cycle" : "gui.scp_unleashed.rocketry.destination.only")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return Lines;
    }

    private List<Component> checkTooltip(LaunchCheck Check) {
        List<Component> Lines = new ArrayList<>();
        boolean Passed = menu.passes(Check);
        LaunchPlatformKind Kind = menu.getKind();
        Lines.add(Component.translatable(labelKey(Check)).withStyle(Passed ? ChatFormatting.GREEN : ChatFormatting.RED));
        Component Detail = switch (Check) {
            case Payload -> Component.translatable(key() + (Passed ? ".payload_ok" : ".payload_missing"));
            case Crew -> Component.translatable(Passed ? "gui.scp_unleashed.rocketry.crew_ok" : "gui.scp_unleashed.rocketry.crew_missing");
            case Fuel -> Component.translatable("gui.scp_unleashed.rocketry.fuel_need", menu.getFuel(), fuelCost());
            case Power -> Component.translatable("gui.scp_unleashed.rocketry.power_need", menu.getPower(), powerCost());
            case Sky -> Component.translatable(Passed ? "gui.scp_unleashed.rocketry.sky_ok" : "gui.scp_unleashed.rocketry.sky_blocked");
            case Route -> routeLine(Passed);
        };
        Lines.add(Detail.copy().withStyle(ChatFormatting.GRAY));
        if (Check == LaunchCheck.Fuel && Kind == LaunchPlatformKind.Rocket) {
            Lines.add(Component.translatable("gui.scp_unleashed.rocketry.fuel_own").withStyle(ChatFormatting.DARK_GRAY));
        }
        return Lines;
    }

    private Component routeLine(boolean Passed) {
        RocketDestinations.Entry Entry = selected();
        if (!Passed || Entry == null) {
            return Component.translatable(Entry != null && !Entry.Available() ? "gui.scp_unleashed.rocketry.destination.missing" : "gui.scp_unleashed.rocketry.route_none");
        }
        return Component.translatable("gui.scp_unleashed.rocketry.route", Entry.Destination().Name());
    }
}
