package net.rai.scp_unleashed.fusion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fusion.FusionMenu;
import net.rai.scp_unleashed.fusion.FusionNotice;
import net.rai.scp_unleashed.fusion.FusionPhase;
import net.rai.scp_unleashed.fusion.FusionReactorBlock;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;
import net.rai.scp_unleashed.registry.ModSounds;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FusionScreen extends AbstractContainerScreen<FusionMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fusion/reactor.png");
    public static final ResourceLocation PlasmaTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fusion/plasma.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 222;
    public static final int FuelX = 10;
    public static final int FuelY = 38;
    public static final int FuelWidth = 12;
    public static final int FuelHeight = 40;
    public static final int FuelU = 224;
    public static final int FuelV = 0;
    public static final int GaugeY = 18;
    public static final int GaugeWidth = 16;
    public static final int GaugeHeight = 60;
    public static final int WaterX = 29;
    public static final int WaterU = 176;
    public static final int ChargeX = 131;
    public static final int ChargeU = 192;
    public static final int OutputX = 152;
    public static final int OutputU = 208;
    public static final int GaugeV = 0;
    public static final int ViewX = 51;
    public static final int ViewY = 18;
    public static final int ViewWidth = 74;
    public static final int ViewHeight = 38;
    public static final int PlasmaColumns = 3;
    public static final int PlasmaFrames = 16;
    public static final int PlasmaSheet = 256;
    public static final int StabilityX = 51;
    public static final int StabilityY = 60;
    public static final int StabilityWidth = 74;
    public static final int StabilityHeight = 4;
    public static final int StabilityU = 176;
    public static final int StabilityV = 62;
    public static final int ButtonY = 67;
    public static final int StartX = 50;
    public static final int StopX = 89;
    public static final int ButtonWidth = 37;
    public static final int ButtonHeight = 13;
    public static final int StartU = 176;
    public static final int StopU = 213;
    public static final int ButtonV = 68;
    public static final int LampX = 162;
    public static final int LampY = 5;
    public static final int LampSize = 7;
    public static final int LampU = 176;
    public static final int LampV = 107;
    public static final int ScreenX = 11;
    public static final int ScreenY = 87;
    public static final int ScreenLine = 11;
    public static final int ScreenGreen = 0xFF7CF0A8;
    public static final int ScreenDim = 0xFF3E8A62;
    public static final int ScreenAmber = 0xFFFFC452;
    public static final int ScreenRed = 0xFFFF6A5A;
    public static final int ButtonText = 0xFFF2F2F2;
    public static final int ButtonDisabledText = 0xFF8C8C8C;

    private int Ticks;
    private int Pressed = -1;

    public FusionScreen(FusionMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 128;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
    }

    private boolean canStart() {
        return !menu.isEnabled() && menu.getPhase() != FusionPhase.Quenching;
    }

    private boolean canStop() {
        return menu.isEnabled() || menu.getPhase() == FusionPhase.Igniting || menu.getPhase() == FusionPhase.Running;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Fuel = Math.round(menu.getFuelFraction() * FuelHeight);
        if (Fuel > 0) {
            Graphics.blit(Texture, leftPos + FuelX, topPos + FuelY + FuelHeight - Fuel, FuelU, FuelV + FuelHeight - Fuel, FuelWidth, Fuel);
        }
        gauge(Graphics, WaterX, WaterU, menu.getWater() / (float) FusionReactorBlockEntity.WaterCapacity);
        gauge(Graphics, ChargeX, ChargeU, menu.getCharge() / (float) FusionReactorBlockEntity.StartupEnergy);
        gauge(Graphics, OutputX, OutputU, menu.getDelivered() / (float) FusionReactorBlockEntity.MaxOutput);
        plasma(Graphics, PartialTick);
        int Stable = Math.round(menu.getStability() * StabilityWidth);
        if (Stable > 0) {
            Graphics.blit(Texture, leftPos + StabilityX, topPos + StabilityY, StabilityU, StabilityV, Stable, StabilityHeight);
        }
        button(Graphics, StartX, StartU, canStart(), MouseX, MouseY, FusionMenu.StartButton);
        button(Graphics, StopX, StopU, canStop(), MouseX, MouseY, FusionMenu.StopButton);
        Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + lampIndex() * LampSize, LampV, LampSize, LampSize);
    }

    private void gauge(GuiGraphics Graphics, int X, int U, float Fraction) {
        int Filled = Math.round(Mth.clamp(Fraction, 0.0f, 1.0f) * GaugeHeight);
        if (Fraction > 0.0f && Filled == 0) {
            Filled = 1;
        }
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + X, topPos + GaugeY + GaugeHeight - Filled, U, GaugeV + GaugeHeight - Filled, GaugeWidth, Filled);
        }
    }

    private void plasma(GuiGraphics Graphics, float PartialTick) {
        FusionPhase Phase = menu.getPhase();
        float Alpha;
        float Red = 1.0f;
        float Green = 1.0f;
        float Blue = 1.0f;
        if (Phase.isLit()) {
            Alpha = menu.getStability();
            if (Phase == FusionPhase.Igniting) {
                Alpha = Math.min(1.0f, Alpha + 0.25f * (0.5f + 0.5f * Mth.sin((Ticks + PartialTick) * 1.7f)));
            }
        } else if (Phase == FusionPhase.Charging) {
            Alpha = 0.12f + 0.3f * (menu.getCharge() / (float) FusionReactorBlockEntity.StartupEnergy);
            Red = 0.45f;
            Green = 0.7f;
        } else {
            return;
        }
        if (Alpha <= 0.01f) {
            return;
        }
        int Frame = Math.floorMod((int) ((Ticks + PartialTick) / 2.0f), PlasmaFrames);
        RenderSystem.enableBlend();
        Graphics.setColor(Red, Green, Blue, Mth.clamp(Alpha, 0.0f, 1.0f));
        Graphics.blit(PlasmaTexture, leftPos + ViewX, topPos + ViewY, (Frame % PlasmaColumns) * ViewWidth, (Frame / PlasmaColumns) * ViewHeight,
                ViewWidth, ViewHeight, PlasmaSheet, PlasmaSheet);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void button(GuiGraphics Graphics, int X, int U, boolean Enabled, int MouseX, int MouseY, int Id) {
        int Row = !Enabled ? 2 : Pressed == Id || isHovering(X, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY) ? 1 : 0;
        Graphics.blit(Texture, leftPos + X, topPos + ButtonY, U, ButtonV + Row * ButtonHeight, ButtonWidth, ButtonHeight);
    }

    private int lampIndex() {
        return switch (menu.getPhase()) {
            case Running -> 1;
            case Charging -> Ticks % 20 < 12 ? 4 : 0;
            case Igniting -> Ticks % 6 < 3 ? 5 : 0;
            case Quenching -> Ticks % 10 < 5 ? 2 : 0;
            default -> menu.getNotice() == FusionNotice.NoFuel || menu.getNotice() == FusionNotice.NoWater ? (Ticks % 30 < 15 ? 3 : 0) : 0;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        super.renderLabels(Graphics, MouseX, MouseY);
        label(Graphics, Component.translatable("gui.scp_unleashed.fusion_reactor.start"), StartX, canStart());
        label(Graphics, Component.translatable("gui.scp_unleashed.fusion_reactor.stop"), StopX, canStop());
        List<Line> Lines = screenLines();
        for (int Index = 0; Index < Lines.size(); Index++) {
            Graphics.drawString(font, Lines.get(Index).Text(), ScreenX, ScreenY + Index * ScreenLine, Lines.get(Index).Colour(), false);
        }
    }

    private void label(GuiGraphics Graphics, Component Text, int X, boolean Enabled) {
        int Sink = Enabled ? 0 : 1;
        Graphics.drawString(font, Text, X + (ButtonWidth - font.width(Text)) / 2, ButtonY + 3 + Sink, Enabled ? ButtonText : ButtonDisabledText, Enabled);
    }

    private List<Line> screenLines() {
        List<Line> Lines = new ArrayList<>();
        FusionPhase Phase = menu.getPhase();
        int Charge = menu.getCharge();
        int Percent = (int) ((long) Charge * 100 / FusionReactorBlockEntity.StartupEnergy);
        switch (Phase) {
            case Charging -> {
                if (Charge >= FusionReactorBlockEntity.StartupEnergy) {
                    boolean Fuelled = menu.getFuelUnits() > 0 || menu.getFuelFraction() > 0.0f;
                    String Key = !Fuelled ? "charged_fuel" : menu.getWater() < FusionReactorBlockEntity.IgnitionWater ? "charged_water" : "charged";
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen." + Key), ScreenAmber));
                } else {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.charging", Percent), ScreenGreen));
                }
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.intake", FusionReactorBlock.grouped(menu.getIntake())), ScreenGreen));
                if (menu.getInputBus() <= 0) {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.no_input"), ScreenRed));
                } else {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.charge", FusionReactorBlock.grouped(Charge / 1000), FusionReactorBlock.grouped(FusionReactorBlockEntity.StartupEnergy / 1000)), ScreenDim));
                }
            }
            case Igniting -> {
                int Progress = Math.min(100, menu.getPhaseTicks() * 100 / FusionReactorBlockEntity.IgnitionTicks);
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.igniting", Progress), ScreenAmber));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.stability", Math.round(menu.getStability() * 100.0f)), ScreenGreen));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.dumping"), ScreenDim));
            }
            case Running -> {
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.running", Math.round(menu.getStability() * 100.0f)), ScreenGreen));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.output", FusionReactorBlock.grouped(menu.getDelivered()), FusionReactorBlock.grouped(FusionReactorBlockEntity.MaxOutput)), ScreenGreen));
                Lines.add(busLine());
            }
            case Quenching -> {
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.quenching"), ScreenAmber));
                Lines.add(new Line(Component.translatable(menu.getNotice().getTranslationKey()), ScreenAmber));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.restart"), ScreenDim));
            }
            default -> {
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.offline"), ScreenDim));
                FusionNotice Notice = menu.getNotice();
                if (Notice != FusionNotice.None) {
                    Lines.add(new Line(Component.translatable(Notice.getTranslationKey()), Notice == FusionNotice.Stopped ? ScreenDim : ScreenRed));
                } else {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.startup", FusionReactorBlock.grouped(FusionReactorBlockEntity.StartupEnergy)), ScreenGreen));
                }
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.press_start"), ScreenDim));
            }
        }
        return Lines;
    }

    private Line busLine() {
        int Rating = menu.getOutputBus();
        if (Rating <= 0) {
            return new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.no_output"), ScreenRed);
        }
        if (Rating < FusionReactorBlockEntity.MaxOutput) {
            return new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.bus_limited", FusionReactorBlock.grouped(Rating)), ScreenAmber);
        }
        return new Line(Component.translatable("gui.scp_unleashed.fusion_reactor.screen.bus_ok", FusionReactorBlock.grouped(Math.min(Rating, 999999))), ScreenDim);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        List<Component> Lines = tooltipLines(MouseX, MouseY);
        if (Lines.isEmpty()) {
            renderTooltip(Graphics, MouseX, MouseY);
        } else {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> tooltipLines(int MouseX, int MouseY) {
        List<Component> Lines = new ArrayList<>();
        if (isHovering(FuelX - 1, FuelY - 1, FuelWidth + 2, FuelHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.fuel", menu.getFuelUnits()));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.fuel_current", Math.round(menu.getFuelFraction() * 100.0f)).withStyle(ChatFormatting.GRAY));
            long Minutes = ((long) Math.round(menu.getFuelFraction() * FusionReactorBlockEntity.FuelTicks) + (long) menu.getFuelUnits() * FusionReactorBlockEntity.FuelTicks) / 1200L;
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.fuel_time", Minutes).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.fuel_rate", FusionReactorBlockEntity.FuelTicks / 1200).withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(WaterX - 1, GaugeY - 1, GaugeWidth + 2, GaugeHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.water", FusionReactorBlock.grouped(menu.getWater()), FusionReactorBlock.grouped(FusionReactorBlockEntity.WaterCapacity)));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.water_rate", FusionReactorBlockEntity.WaterPerTick).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.water_inlet").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(ChargeX - 1, GaugeY - 1, GaugeWidth + 2, GaugeHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.charge", FusionReactorBlock.grouped(menu.getCharge()), FusionReactorBlock.grouped(FusionReactorBlockEntity.StartupEnergy)));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.intake", FusionReactorBlock.grouped(menu.getIntake())).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.input_bus", FusionReactorBlock.grouped(menu.getInputBus()), FusionReactorBlock.grouped(FusionReactorBlockEntity.MaxChargeRate)).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.input_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(OutputX - 1, GaugeY - 1, GaugeWidth + 2, GaugeHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.output", FusionReactorBlock.grouped(menu.getDelivered()), FusionReactorBlock.grouped(FusionReactorBlockEntity.MaxOutput)));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.output_bus", FusionReactorBlock.grouped(menu.getOutputBus())).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.output_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(ViewX - 1, ViewY - 1, ViewWidth + 2, StabilityY + StabilityHeight - ViewY + 2, MouseX, MouseY)) {
            FusionPhase Phase = menu.getPhase();
            Lines.add(Component.translatable(Phase.getTranslationKey()).withStyle(Phase.getColour()));
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.stability", Math.round(menu.getStability() * 100.0f)).withStyle(ChatFormatting.GRAY));
            return RecipeHint.with(Lines, isHovering(ViewX, ViewY, ViewWidth, ViewHeight, MouseX, MouseY));
        } else if (isHovering(StartX, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.start_hint"));
        } else if (isHovering(StopX, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.fusion_reactor.stop_hint"));
        }
        return Lines;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isHovering(StartX, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY) && canStart()) {
                press(FusionMenu.StartButton);
                return true;
            }
            if (isHovering(StopX, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY) && canStop()) {
                press(FusionMenu.StopButton);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed >= 0 && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PanelRelease.get(), 1.0f, 0.3f));
        }
        Pressed = -1;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    private void press(int Id) {
        Pressed = Id;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PanelPress.get(), Id == FusionMenu.StartButton ? 1.0f : 0.9f, 0.35f));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Id);
        }
    }

    private record Line(Component Text, int Colour) {
    }
}
