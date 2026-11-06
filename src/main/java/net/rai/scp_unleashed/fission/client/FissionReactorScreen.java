package net.rai.scp_unleashed.fission.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fission.FissionReactorBlockEntity;
import net.rai.scp_unleashed.fission.FissionReactorMenu;
import net.rai.scp_unleashed.fission.FissionStatus;
import net.rai.scp_unleashed.fission.ReactorThermal;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FissionReactorScreen extends AbstractContainerScreen<FissionReactorMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/reactor.png");
    public static final ResourceLocation CoreTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/reactor_core.png");
    public static final int TextureSize = 256;
    public static final int PanelWidth = 208;
    public static final int PanelHeight = 222;
    public static final int InventoryLabelY = 129;
    public static final int CoreX = 90;
    public static final int CoreY = 20;
    public static final int CoreWidth = 56;
    public static final int CoreHeight = 44;
    public static final int CoreSheet = 64;
    public static final int[] FuelRodX = {9, 19, 29, 39};
    public static final int FuelRodY = 12;
    public static final int FuelRodWidth = 5;
    public static final int FuelRodHeight = 28;
    public static final int[] ControlRodX = {15, 25, 35};
    public static final int ControlRodWidth = 3;
    public static final int ControlRodHeight = 36;
    public static final int ControlTop = 3;
    public static final int ControlTravel = 30;
    public static final int GlowX = 4;
    public static final int GlowY = 6;
    public static final int GlowWidth = 48;
    public static final int GlowHeight = 36;
    public static final int FuelLitU = 0;
    public static final int FuelDimU = 6;
    public static final int ControlRodU = 12;
    public static final int GlowU = 16;
    public static final int HotGlowV = 36;
    public static final int LowerX = 90;
    public static final int RaiseX = 134;
    public static final int LimitY = 67;
    public static final int SmallWidth = 12;
    public static final int SmallHeight = 11;
    public static final int LcdX = 104;
    public static final int LcdWidth = 28;
    public static final int ToggleX = 90;
    public static final int ToggleY = 82;
    public static final int ToggleWidth = 56;
    public static final int ToggleHeight = 18;
    public static final int CoolantLampX = 91;
    public static final int VentLampX = 119;
    public static final int StatusLampY = 106;
    public static final int StatusIconWidth = 24;
    public static final int LampSize = 6;
    public static final int HeatX = 151;
    public static final int WasteX = 169;
    public static final int GasX = 189;
    public static final int GaugeY = 21;
    public static final int GaugeHeight = 62;
    public static final int HeatWidth = 12;
    public static final int WasteWidth = 16;
    public static final int GasWidth = 10;
    public static final int ReadoutX = 152;
    public static final int TemperatureY = 95;
    public static final int OutputY = 108;
    public static final int ReadoutWidth = 48;
    public static final int ReadoutHeight = 11;
    public static final int ArrowX = 35;
    public static final int ArrowY = 100;
    public static final int ArrowWidth = 24;
    public static final int ArrowHeight = 16;
    public static final int BurnX = 78;
    public static final int BurnY = 24;
    public static final int BurnWidth = 4;
    public static final int BurnHeight = 34;
    public static final int HeatFillU = 210;
    public static final int GasFillU = 224;
    public static final int BurnFillU = 236;
    public static final int FillV = 0;
    public static final int ArrowFillU = 210;
    public static final int ArrowFillV = 64;
    public static final int LampSpriteU = 210;
    public static final int LampSpriteV = 82;
    public static final int MarkerU = 242;
    public static final int MarkerV = 0;
    public static final int MarkerWidth = 4;
    public static final int MarkerHeight = 5;
    public static final int ToggleV = 224;
    public static final int SmallV = 242;
    public static final int LcdColour = 0xFF5CFF8A;
    public static final int LcdDimColour = 0xFF2A7A44;
    public static final int LcdWarnColour = 0xFFFFB13B;
    public static final int LcdHotColour = 0xFFFF5040;
    public static final int TitleColour = 0xFF404040;
    public static final int WasteTint = 0xFFFFFFFF;
    public static final int GaugeIconY = 86;
    public static final int GaugeIconSize = 8;
    public static final int TrefoilX = 41;
    public static final int TrefoilY = 87;
    public static final int TrefoilSize = 11;
    public static final int BoltX = 146;
    public static final HoverArea RecipeArea = new HoverArea(ArrowX, ArrowY, ArrowWidth, ArrowHeight);

    private enum HoverSpot {
        HeatGauge(HeatX - 1, GaugeY - 1, HeatWidth + MarkerWidth + 1, GaugeIconY + GaugeIconSize - GaugeY + 1),
        WasteGauge(WasteX - 1, GaugeY - 1, WasteWidth + 2, GaugeIconY + GaugeIconSize - GaugeY + 1),
        GasGauge(GasX - 1, GaugeY - 1, GasWidth + 2, GaugeIconY + GaugeIconSize - GaugeY + 1),
        TemperatureLcd(ReadoutX, TemperatureY, ReadoutWidth, ReadoutHeight),
        OutputLcd(BoltX, OutputY, ReadoutX + ReadoutWidth - BoltX, ReadoutHeight),
        Toggle(ToggleX, ToggleY, ToggleWidth, ToggleHeight),
        LimitLower(LowerX, LimitY, SmallWidth, SmallHeight),
        LimitDisplay(LcdX, LimitY, LcdWidth, SmallHeight),
        LimitRaise(RaiseX, LimitY, SmallWidth, SmallHeight),
        Core(CoreX, CoreY, CoreWidth, CoreHeight),
        CoolantLamp(CoolantLampX - 1, StatusLampY - 1, StatusIconWidth, LampSize + 2),
        VentLamp(VentLampX - 1, StatusLampY - 1, StatusIconWidth, LampSize + 2),
        BurnGauge(BurnX - 1, BurnY - 1, BurnWidth + 2, BurnHeight + 2),
        Irradiation(ArrowX, TrefoilY - 1, ArrowWidth, ArrowY + ArrowHeight - TrefoilY + 1);

        private final HoverArea Area;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.Area = new HoverArea(X, Y, Width, Height);
        }
    }

    private float ShownPower;
    private float PreviousShownPower;
    private int Ticks;

    public FissionReactorScreen(FissionReactorMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelX = FissionReactorMenu.InventoryX;
        inventoryLabelY = InventoryLabelY;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        PreviousShownPower = ShownPower;
        ShownPower += (menu.getPower() - ShownPower) * 0.15f;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderCore(Graphics, PartialTick);
        renderGauges(Graphics);
        renderControls(Graphics, MouseX, MouseY);
        renderLamps(Graphics);
        float Rod = menu.getRodFraction();
        if (Rod > 0.0f) {
            int Filled = Math.max(1, Math.round(Rod * BurnHeight));
            Graphics.blit(Texture, leftPos + BurnX, topPos + BurnY + BurnHeight - Filled, BurnFillU, FillV + BurnHeight - Filled, BurnWidth, Filled);
        }
        int Arrow = Math.round(menu.getIrradiation() * ArrowWidth);
        if (Arrow > 0) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowFillU, ArrowFillV, Arrow, ArrowHeight);
        }
    }

    private void renderCore(GuiGraphics Graphics, float PartialTick) {
        float Power = Mth.lerp(PartialTick, PreviousShownPower, ShownPower);
        int Left = leftPos + CoreX;
        int Top = topPos + CoreY;
        Graphics.enableScissor(Left, Top, Left + CoreWidth, Top + CoreHeight);
        boolean Fuelled = menu.getRodFraction() >= 0.0f;
        for (int RodX : FuelRodX) {
            Graphics.blit(CoreTexture, Left + RodX, Top + FuelRodY, Fuelled && Power > 0.01f ? FuelLitU : FuelDimU, 0, FuelRodWidth, FuelRodHeight, CoreSheet, CoreSheet);
        }
        if (Power > 0.01f) {
            RenderSystem.enableBlend();
            float Flicker = 0.85f + 0.15f * Mth.sin((Ticks + PartialTick) * 0.35f);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Mth.clamp(Power * Flicker, 0.0f, 1.0f));
            Graphics.blit(CoreTexture, Left + GlowX, Top + GlowY, GlowU, 0, GlowWidth, GlowHeight, CoreSheet, CoreSheet);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
        int Insertion = Math.round((1.0f - Mth.clamp(Power, 0.0f, 1.0f)) * ControlTravel);
        for (int RodX : ControlRodX) {
            int Bottom = Top + ControlTop + Insertion + 6;
            Graphics.blit(CoreTexture, Left + RodX, Bottom - ControlRodHeight, ControlRodU, 0, ControlRodWidth, ControlRodHeight, CoreSheet, CoreSheet);
        }
        ReactorThermal Thermal = menu.getThermal();
        if (Thermal == ReactorThermal.Critical && Ticks % 10 < 6 || Thermal == ReactorThermal.Warning && Ticks % 20 < 10) {
            RenderSystem.enableBlend();
            Graphics.setColor(1.0f, 1.0f, 1.0f, Thermal == ReactorThermal.Critical ? 0.75f : 0.4f);
            Graphics.blit(CoreTexture, Left + GlowX, Top + GlowY, GlowU, HotGlowV, GlowWidth, CoreSheet - HotGlowV, CoreSheet, CoreSheet);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
        Graphics.disableScissor();
    }

    private void renderGauges(GuiGraphics Graphics) {
        float Heat = ReactorThermal.fraction(menu.getTemperature());
        int HeatFill = Math.round(Heat * GaugeHeight);
        if (HeatFill > 0) {
            Graphics.blit(Texture, leftPos + HeatX, topPos + GaugeY + GaugeHeight - HeatFill, HeatFillU, FillV + GaugeHeight - HeatFill, HeatWidth, HeatFill);
        }
        float Settle = ReactorThermal.fraction(menu.get(FissionReactorBlockEntity.DataSettle));
        if (menu.getPower() > 0.0f || menu.isRunning()) {
            int MarkerY = topPos + GaugeY + GaugeHeight - Math.round(Settle * GaugeHeight) - MarkerHeight / 2;
            Graphics.blit(Texture, leftPos + HeatX + HeatWidth, MarkerY, MarkerU, MarkerV, MarkerWidth, MarkerHeight);
        }
        int WasteFill = Math.round((float) menu.getWaste() / FissionReactorBlockEntity.WasteCapacity * GaugeHeight);
        if (WasteFill > 0) {
            FissionGuiFluids.drawFluid(Graphics, WasteWaterExtensions.Still, WasteTint, leftPos + WasteX, topPos + GaugeY + GaugeHeight - WasteFill, WasteWidth, WasteFill);
        }
        int GasFill = Math.round((float) menu.getGas() / FissionReactorBlockEntity.GasCapacity * GaugeHeight);
        if (GasFill > 0) {
            Graphics.blit(Texture, leftPos + GasX, topPos + GaugeY + GaugeHeight - GasFill, GasFillU, FillV + GaugeHeight - GasFill, GasWidth, GasFill);
        }
    }

    private void renderControls(GuiGraphics Graphics, int MouseX, int MouseY) {
        HoverSpot Spot = spotAt(MouseX - leftPos, MouseY - topPos);
        boolean Running = menu.isRunning();
        int ToggleU = (Running ? 2 : 0) * ToggleWidth + (Spot == HoverSpot.Toggle ? ToggleWidth : 0);
        Graphics.blit(Texture, leftPos + ToggleX, topPos + ToggleY, ToggleU, ToggleV, ToggleWidth, ToggleHeight);
        Graphics.blit(Texture, leftPos + LowerX, topPos + LimitY, Spot == HoverSpot.LimitLower ? SmallWidth : 0, SmallV, SmallWidth, SmallHeight);
        Graphics.blit(Texture, leftPos + RaiseX, topPos + LimitY, 2 * SmallWidth + (Spot == HoverSpot.LimitRaise ? SmallWidth : 0), SmallV, SmallWidth, SmallHeight);
    }

    private void renderLamps(GuiGraphics Graphics) {
        int Exchangers = menu.get(FissionReactorBlockEntity.DataExchangers);
        int Flowing = menu.get(FissionReactorBlockEntity.DataFlowing);
        int Ice = menu.get(FissionReactorBlockEntity.DataIce);
        int Flow = menu.get(FissionReactorBlockEntity.DataFlow);
        int Coolant;
        if (Exchangers > 0 && Flow >= 100 && Flowing > 0) {
            Coolant = 0;
        } else if (Exchangers > 0 && Flowing > 0 || Ice > 0) {
            Coolant = 1;
        } else {
            Coolant = Ticks % 20 < 10 ? 2 : -1;
        }
        lamp(Graphics, CoolantLampX, Coolant);
        int Venting = menu.get(FissionReactorBlockEntity.DataVenting);
        int Vent = switch (Venting) {
            case FissionReactorBlockEntity.VentOpen -> 0;
            case FissionReactorBlockEntity.VentDissolved -> 3;
            case FissionReactorBlockEntity.VentLeaking -> Ticks % 10 < 6 ? 2 : -1;
            default -> menu.getGas() > 0 ? 1 : -1;
        };
        lamp(Graphics, VentLampX, Vent);
    }

    private void lamp(GuiGraphics Graphics, int X, int Colour) {
        if (Colour >= 0) {
            Graphics.blit(Texture, leftPos + X, topPos + StatusLampY, LampSpriteU + Colour * LampSize, LampSpriteV, LampSize, LampSize);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, TitleColour, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TitleColour, false);
        Component Status = statusLine();
        Graphics.drawString(font, Status, PanelWidth - 8 - font.width(Status), titleLabelY, statusColour(), false);
        lcd(Graphics, Component.literal(menu.getLimit() + "%"), LcdX, LimitY, LcdWidth, SmallHeight, LcdColour);
        int Temperature = menu.getTemperature();
        int TemperatureColour = switch (menu.getThermal()) {
            case Critical -> Ticks % 10 < 6 ? LcdHotColour : LcdWarnColour;
            case Warning -> LcdWarnColour;
            default -> LcdColour;
        };
        lcd(Graphics, Component.translatable("gui.scp_unleashed.fission_reactor.lcd.temperature", Temperature), ReadoutX, TemperatureY, ReadoutWidth, ReadoutHeight, TemperatureColour);
        int Output = menu.getOutput();
        lcd(Graphics, Component.translatable("gui.scp_unleashed.fission_reactor.lcd.output", Output), ReadoutX, OutputY, ReadoutWidth, ReadoutHeight, Output > 0 ? LcdColour : LcdDimColour);
        Component Toggle = Component.translatable(menu.isRunning() ? "gui.scp_unleashed.fission_reactor.scram" : "gui.scp_unleashed.fission_reactor.start");
        Graphics.drawString(font, Toggle, ToggleX + (ToggleWidth - font.width(Toggle)) / 2, ToggleY + (ToggleHeight - 8) / 2, 0xFFFFFFFF, true);
    }

    private void lcd(GuiGraphics Graphics, Component Text, int X, int Y, int Width, int Height, int Colour) {
        int TextWidth = font.width(Text);
        Graphics.drawString(font, Text, X + Width - 2 - TextWidth, Y + (Height - 7) / 2, Colour, false);
    }

    private Component statusLine() {
        ReactorThermal Thermal = menu.getThermal();
        if (Thermal != ReactorThermal.Normal) {
            return Component.translatable(Thermal.getTranslationKey());
        }
        return Component.translatable(menu.getStatus().getTranslationKey());
    }

    private int statusColour() {
        ReactorThermal Thermal = menu.getThermal();
        if (Thermal == ReactorThermal.Critical) {
            return Ticks % 10 < 6 ? 0xFFE02020 : 0xFF801010;
        }
        if (Thermal == ReactorThermal.Warning) {
            return 0xFFC07000;
        }
        return switch (menu.getStatus()) {
            case Running -> 0xFF207A20;
            case LoadFollowing -> 0xFF206A8A;
            case Offline -> 0xFF606060;
            case Interlocked -> 0xFFB01818;
            default -> 0xFFA06000;
        };
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
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            HoverSpot Spot = spotAt(MouseX - leftPos, MouseY - topPos);
            int Id = -1;
            if (Spot == HoverSpot.Toggle) {
                Id = FissionReactorBlockEntity.ButtonToggle;
            } else if (Spot == HoverSpot.LimitLower) {
                Id = FissionReactorBlockEntity.ButtonLower;
            } else if (Spot == HoverSpot.LimitRaise) {
                Id = FissionReactorBlockEntity.ButtonRaise;
            }
            if (Id >= 0 && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Id);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, Id == FissionReactorBlockEntity.ButtonToggle ? 0.8f : 1.2f));
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
        double GuiX = MouseX - leftPos;
        double GuiY = MouseY - topPos;
        HoverSpot Spot = spotAt(GuiX, GuiY);
        if (Spot != null) {
            Graphics.renderComponentTooltip(font, tooltipFor(Spot, GuiX, GuiY), MouseX, MouseY);
        }
    }

    private List<Component> tooltipFor(HoverSpot Spot, double GuiX, double GuiY) {
        List<Component> Lines = new ArrayList<>();
        switch (Spot) {
            case HeatGauge, TemperatureLcd -> heatLines(Lines);
            case WasteGauge -> wasteLines(Lines);
            case GasGauge, VentLamp -> gasLines(Lines);
            case CoolantLamp -> coolantLines(Lines);
            case BurnGauge -> rodLines(Lines);
            case Toggle -> toggleLines(Lines);
            case LimitLower, LimitDisplay, LimitRaise -> limitLines(Lines);
            case OutputLcd -> outputLines(Lines);
            case Core -> coreLines(Lines);
            case Irradiation -> {
                irradiationLines(Lines);
                return RecipeHint.with(Lines, RecipeArea.contains(GuiX, GuiY));
            }
        }
        return Lines;
    }

    private void heatLines(List<Component> Lines) {
        ReactorThermal Thermal = menu.getThermal();
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.heat", menu.getTemperature(), Math.round(ReactorThermal.MeltdownLevel)).withStyle(Thermal.getColour()));
        int Settle = menu.get(FissionReactorBlockEntity.DataSettle);
        if (menu.getPower() > 0.0f || menu.isRunning()) {
            ChatFormatting SettleColour = Settle >= ReactorThermal.MeltdownLevel ? ChatFormatting.RED : ReactorThermal.of(Settle).getColour();
            Lines.add(Component.translatable(Settle >= ReactorThermal.MeltdownLevel ? "gui.scp_unleashed.fission_reactor.settle_unsafe" : "gui.scp_unleashed.fission_reactor.settle", Settle).withStyle(SettleColour));
        }
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.rates", tenths(menu.get(FissionReactorBlockEntity.DataHeating)), tenths(menu.get(FissionReactorBlockEntity.DataCooling))).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.limits", Math.round(ReactorThermal.WarningLevel), Math.round(ReactorThermal.CriticalLevel)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private void wasteLines(List<Component> Lines) {
        boolean Full = menu.getStatus() == FissionStatus.WasteFull;
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.waste", menu.getWaste(), FissionReactorBlockEntity.WasteCapacity));
        Lines.add(Component.translatable(Full ? "gui.scp_unleashed.fission_reactor.waste_full" : "gui.scp_unleashed.fission_reactor.waste_hint")
                .withStyle(Full ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }

    private void gasLines(List<Component> Lines) {
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.gas", Math.round(menu.getGas() * 100.0f / FissionReactorBlockEntity.GasCapacity)));
        int Venting = menu.get(FissionReactorBlockEntity.DataVenting);
        String Key = switch (Venting) {
            case FissionReactorBlockEntity.VentOpen -> "gui.scp_unleashed.fission_reactor.gas_vented";
            case FissionReactorBlockEntity.VentDissolved -> "gui.scp_unleashed.fission_reactor.gas_dissolved";
            case FissionReactorBlockEntity.VentLeaking -> "gui.scp_unleashed.fission_reactor.gas_leaking";
            default -> "gui.scp_unleashed.fission_reactor.gas_sealed";
        };
        ChatFormatting VentColour = switch (Venting) {
            case FissionReactorBlockEntity.VentLeaking -> ChatFormatting.RED;
            case FissionReactorBlockEntity.VentOpen -> ChatFormatting.GREEN;
            case FissionReactorBlockEntity.VentDissolved -> ChatFormatting.AQUA;
            default -> ChatFormatting.GOLD;
        };
        Lines.add(Component.translatable(Key).withStyle(VentColour));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.gas_hint").withStyle(ChatFormatting.GRAY));
    }

    private void coolantLines(List<Component> Lines) {
        int Exchangers = menu.get(FissionReactorBlockEntity.DataExchangers);
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.coolant"));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.exchangers", Exchangers, menu.get(FissionReactorBlockEntity.DataFlowing))
                .withStyle(Exchangers > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        if (Exchangers > 0 && menu.get(FissionReactorBlockEntity.DataFlow) < 100) {
            Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.flow", menu.get(FissionReactorBlockEntity.DataFlow)).withStyle(ChatFormatting.GOLD));
        }
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.ice", menu.get(FissionReactorBlockEntity.DataIce)).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.coolant_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void rodLines(List<Component> Lines) {
        float Rod = menu.getRodFraction();
        if (Rod < 0.0f) {
            Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.rod_empty"));
            return;
        }
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.rod", Math.round(Rod * 100.0f)));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.rod_time", menu.get(FissionReactorBlockEntity.DataRodMinutes)).withStyle(ChatFormatting.GRAY));
    }

    private void toggleLines(List<Component> Lines) {
        Lines.add(Component.translatable(menu.isRunning() ? "gui.scp_unleashed.fission_reactor.scram_hint" : "gui.scp_unleashed.fission_reactor.start_hint"));
        if (menu.getStatus() == FissionStatus.Interlocked) {
            Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.interlocked_hint").withStyle(ChatFormatting.RED));
        }
    }

    private void limitLines(List<Component> Lines) {
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.limit", menu.getLimit()));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.limit_hint").withStyle(ChatFormatting.GRAY));
    }

    private void outputLines(List<Component> Lines) {
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.output", menu.getOutput(), menu.get(FissionReactorBlockEntity.DataMaxOutput)));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.output_hint").withStyle(ChatFormatting.GRAY));
    }

    private void coreLines(List<Component> Lines) {
        FissionStatus Status = menu.getStatus();
        Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.power", Math.round(menu.getPower() * 100.0f), menu.getLimit()).withStyle(ChatFormatting.GRAY));
    }

    private void irradiationLines(List<Component> Lines) {
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.irradiation", Math.round(menu.getIrradiation() * 100.0f)));
        Lines.add(Component.translatable("gui.scp_unleashed.fission_reactor.irradiation_hint").withStyle(ChatFormatting.GRAY));
    }

    private static String tenths(int Value) {
        return String.format(Locale.ROOT, "%.1f", Value / 10.0f);
    }
}
