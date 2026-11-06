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
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.SonicVibratorBlockEntity;
import net.rai.scp_unleashed.fission.SonicVibratorMenu;
import net.rai.scp_unleashed.fission.VibratorStatus;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SonicVibratorScreen extends AbstractContainerScreen<SonicVibratorMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/sonic_vibrator.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int InputX = 31;
    public static final int OutputX = 125;
    public static final int TankY = 19;
    public static final int TankWidth = 16;
    public static final int TankHeight = 50;
    public static final int ChamberX = 60;
    public static final int ChamberY = 18;
    public static final int ChamberWidth = 52;
    public static final int ChamberHeight = 50;
    public static final int EmitterX = 77;
    public static final int EmitterY = 21;
    public static final int TendrilWidth = 18;
    public static final int TendrilHeight = 12;
    public static final int TendrilFrames = 4;
    public static final int TendrilU = 176;
    public static final int TendrilV = 0;
    public static final int RingU = 176;
    public static final int RingV = 48;
    public static final int RingSize = 32;
    public static final int RingCentreX = 86;
    public static final int RingCentreY = 40;
    public static final int GlowU = 208;
    public static final int GlowV = 48;
    public static final int GlowSize = 24;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 80;
    public static final int LampX = 50;
    public static final int LampY = 60;
    public static final int LampSize = 6;
    public static final int LampU = 192;
    public static final int LampV = 80;
    public static final int InArrowX = 49;
    public static final int OutArrowX = 114;
    public static final int ArrowY = 40;
    public static final int ArrowWidth = 10;
    public static final int ArrowHeight = 7;
    public static final int ArrowU = 192;
    public static final int ArrowV = 88;
    public static final HoverArea RecipeArea = new HoverArea(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

    private enum HoverSpot {
        InputTank(InputX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        OutputTank(OutputX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        PowerBar(BarX - 1, BarY - 1, BarWidth + 2, BarHeight + 2),
        Chamber(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2),
        StatusLamp(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2);

        private final HoverArea Area;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.Area = new HoverArea(X, Y, Width, Height);
        }
    }

    private float Intensity;
    private float PreviousIntensity;
    private float Phase;
    private float PreviousPhase;
    private int Ticks;

    public SonicVibratorScreen(SonicVibratorMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 72;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        float Target = menu.getStatus().isWorking() ? Mth.clamp(menu.getReceived() / (float) SonicVibratorBlockEntity.drawFor(menu.getUpgrades()), 0.0f, 1.0f) : 0.0f;
        PreviousIntensity = Intensity;
        PreviousPhase = Phase;
        Intensity += (Target - Intensity) * (Target > Intensity ? 0.08f : 0.04f);
        Phase += 0.2f + Intensity;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int InputFill = Math.round((float) menu.getInput() / SonicVibratorBlockEntity.TankCapacity * TankHeight);
        if (InputFill > 0) {
            FissionGuiFluids.drawFluid(Graphics, FissionRegistry.WasteWater.get(), leftPos + InputX, topPos + TankY + TankHeight - InputFill, TankWidth, InputFill);
        }
        int OutputFill = Math.round((float) menu.getOutput() / SonicVibratorBlockEntity.TankCapacity * TankHeight);
        if (OutputFill > 0) {
            FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, leftPos + OutputX, topPos + TankY + TankHeight - OutputFill, TankWidth, OutputFill);
        }
        float Shown = Mth.lerp(PartialTick, PreviousIntensity, Intensity);
        float Wave = Mth.lerp(PartialTick, PreviousPhase, Phase);
        renderChamber(Graphics, Shown, Wave);
        int Power = Math.round(Mth.clamp(menu.getReceived() / (float) SonicVibratorBlockEntity.drawFor(menu.getUpgrades()), 0.0f, 1.0f) * BarHeight);
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
        if (Shown > 0.05f) {
            int Lit = Math.round(Shown * ArrowWidth);
            Graphics.blit(Texture, leftPos + InArrowX, topPos + ArrowY, ArrowU, ArrowV, Lit, ArrowHeight);
            Graphics.blit(Texture, leftPos + OutArrowX, topPos + ArrowY, ArrowU, ArrowV, Lit, ArrowHeight);
        }
        int Lamp = switch (menu.getStatus()) {
            case Working -> 0;
            case LowPower, Blocked -> Ticks % 20 < 12 ? 1 : -1;
            case Unpowered -> Ticks % 20 < 10 ? 2 : -1;
            default -> -1;
        };
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
    }

    private void renderChamber(GuiGraphics Graphics, float Shown, float Wave) {
        int Left = leftPos + ChamberX;
        int Top = topPos + ChamberY;
        Graphics.enableScissor(Left, Top, Left + ChamberWidth, Top + ChamberHeight);
        RenderSystem.enableBlend();
        if (Shown > 0.02f) {
            for (int Ring = 0; Ring < 3; Ring++) {
                float Age = (Wave * 0.05f + Ring / 3.0f) % 1.0f;
                int Size = Math.max(4, Math.round(6 + Age * (RingSize + 10)));
                Graphics.setColor(1.0f, 1.0f, 1.0f, Shown * (1.0f - Age) * 0.85f);
                Graphics.blit(Texture, leftPos + RingCentreX - Size / 2, topPos + RingCentreY - Size / 2, Size, Size, RingU, RingV, RingSize, RingSize, 256, 256);
            }
            float Pulse = 0.55f + 0.45f * Mth.sin(Wave * 0.9f);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Shown * Pulse);
            Graphics.blit(Texture, leftPos + RingCentreX - GlowSize / 2, topPos + EmitterY - 4, GlowU, GlowV, GlowSize, GlowSize);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int Frame = Shown > 0.02f ? Math.floorMod((int) (Wave * 0.6f), TendrilFrames) : 0;
        Graphics.blit(Texture, leftPos + EmitterX, topPos + EmitterY, TendrilU, TendrilV + Frame * TendrilHeight, TendrilWidth, TendrilHeight);
        RenderSystem.disableBlend();
        Graphics.disableScissor();
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
            case InputTank -> {
                Lines.add(Component.translatable("gui.scp_unleashed.sonic_vibrator.input", menu.getInput(), SonicVibratorBlockEntity.TankCapacity));
                Lines.add(Component.translatable("gui.scp_unleashed.sonic_vibrator.input_hint").withStyle(ChatFormatting.GRAY));
            }
            case OutputTank -> {
                Lines.add(Component.translatable("gui.scp_unleashed.sonic_vibrator.output", menu.getOutput(), SonicVibratorBlockEntity.TankCapacity));
                Lines.add(Component.translatable("gui.scp_unleashed.sonic_vibrator.output_hint").withStyle(ChatFormatting.GRAY));
            }
            case PowerBar -> {
                Lines.add(Component.translatable("gui.scp_unleashed.sonic_vibrator.power", menu.getReceived(), menu.getDemand()));
                Lines.add(Component.translatable("gui.scp_unleashed.sonic_vibrator.draws_active", SonicVibratorBlockEntity.drawFor(menu.getUpgrades())).withStyle(ChatFormatting.GRAY));
                if (menu.getUpgrades() > 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.machine.speed_upgrades", menu.getUpgrades(), UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case Chamber, StatusLamp -> {
                VibratorStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                Lines.add(Component.translatable("gui.scp_unleashed.sonic_vibrator.rate", SonicVibratorBlockEntity.rateFor(menu.getUpgrades())).withStyle(ChatFormatting.GRAY));
                return RecipeHint.with(Lines, RecipeArea.contains(GuiX, GuiY));
            }
        }
        return Lines;
    }
}
