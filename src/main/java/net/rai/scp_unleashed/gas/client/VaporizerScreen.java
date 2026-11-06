package net.rai.scp_unleashed.gas.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fission.client.FissionGuiFluids;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.gas.Gas;
import net.rai.scp_unleashed.gas.VaporizerBlockEntity;
import net.rai.scp_unleashed.gas.VaporizerMenu;
import net.rai.scp_unleashed.gas.VaporizerStatus;
import net.rai.scp_unleashed.gas.VaporizingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VaporizerScreen extends AbstractContainerScreen<VaporizerMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/gas/liquid_vaporizer.png");
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
    public static final int CoilX = 64;
    public static final int CoilY = 58;
    public static final int CoilWidth = 44;
    public static final int CoilHeight = 6;
    public static final int CoilU = 176;
    public static final int CoilV = 0;
    public static final int CoilDarkV = 24;
    public static final int PoolTop = 46;
    public static final int WispU = 176;
    public static final int WispV = 8;
    public static final int WispWidth = 6;
    public static final int WispHeight = 7;
    public static final int Wisps = 5;
    public static final int BubbleU = 182;
    public static final int BubbleV = 8;
    public static final int BubbleSize = 3;
    public static final int Bubbles = 6;
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
    public static final int MeterX = 66;
    public static final int MeterY = 21;
    public static final int MeterWidth = 40;
    public static final int MeterHeight = 3;
    public static final int MeterU = 176;
    public static final int MeterV = 16;
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

    private float Heat;
    private float PreviousHeat;
    private int Ticks;

    public VaporizerScreen(VaporizerMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 72;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        PreviousHeat = Heat;
        float Target = menu.getStatus().isWorking() ? 1.0f : 0.0f;
        Heat += (Target - Heat) * (Target > Heat ? 0.06f : 0.03f);
    }

    private int powerShare() {
        int Demand = menu.getDemand();
        return Demand <= 0 ? 0 : Math.round(Mth.clamp(menu.getReceived() / (float) Demand, 0.0f, 1.0f) * BarHeight);
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        Fluid Liquid = menu.getFluid();
        int InputFill = Math.round((float) menu.getInput() / VaporizerBlockEntity.TankCapacity * TankHeight);
        if (InputFill > 0 && Liquid != Fluids.EMPTY) {
            FissionGuiFluids.drawFluid(Graphics, Liquid, leftPos + InputX, topPos + TankY + TankHeight - InputFill, TankWidth, InputFill);
        }
        Gas Kind = menu.getGasKind();
        int OutputFill = Math.round((float) menu.getGas() / VaporizerBlockEntity.GasCapacity * TankHeight);
        if (OutputFill > 0 && Kind != null) {
            GasGui.drawGas(Graphics, Kind, leftPos + OutputX, topPos + TankY + TankHeight - OutputFill, TankWidth, OutputFill);
        }
        float Shown = Mth.lerp(PartialTick, PreviousHeat, Heat);
        renderChamber(Graphics, Liquid, Kind, Shown, Ticks + PartialTick);
        int Power = powerShare();
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

    private void renderChamber(GuiGraphics Graphics, Fluid Liquid, @Nullable Gas Kind, float Shown, float Time) {
        int Left = leftPos + ChamberX;
        int Top = topPos + ChamberY;
        int Bottom = Top + ChamberHeight;
        if (Liquid != Fluids.EMPTY && menu.getInput() > 0) {
            FissionGuiFluids.drawFluid(Graphics, Liquid, Left, topPos + PoolTop, ChamberWidth, Bottom - (topPos + PoolTop));
        }
        int Progress = Math.round(menu.getProgress() / (float) VaporizerBlockEntity.ProgressSteps * MeterWidth);
        if (Progress > 0) {
            Graphics.blit(Texture, leftPos + MeterX, topPos + MeterY, MeterU, MeterV, Progress, MeterHeight);
        }
        Graphics.blit(Texture, leftPos + CoilX, topPos + CoilY, CoilU, CoilDarkV, CoilWidth, CoilHeight);
        Graphics.enableScissor(Left, Top, Left + ChamberWidth, Bottom);
        RenderSystem.enableBlend();
        if (Shown > 0.02f) {
            Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
            Graphics.blit(Texture, leftPos + CoilX, topPos + CoilY, CoilU, CoilV, CoilWidth, CoilHeight);
            int Rise = PoolTop - WispHeight - (MeterY + MeterHeight + 2);
            for (int Index = 0; Index < Bubbles; Index++) {
                float Speed = 0.6f + (Index * 37 % 7) * 0.08f;
                float Travel = (Time * Speed + Index * 5.3f) % (CoilY - PoolTop);
                int X = Left + 4 + (Index * 17 + 5) % (ChamberWidth - 8);
                int Y = topPos + CoilY - Math.round(Travel);
                Graphics.blit(Texture, X, Y, BubbleU, BubbleV, BubbleSize, BubbleSize);
            }
            int Colour = Kind == null ? 0xFFFFFF : Kind.getColour();
            for (int Index = 0; Index < Wisps; Index++) {
                float Life = (Time * 0.04f + Index / (float) Wisps) % 1.0f;
                int X = Left + 6 + Index * 9 + Math.round(Mth.sin(Time * 0.13f + Index * 1.7f) * 2.0f);
                int Y = topPos + PoolTop - WispHeight - Math.round(Life * Rise);
                Graphics.setColor((Colour >> 16 & 255) / 255.0f, (Colour >> 8 & 255) / 255.0f, (Colour & 255) / 255.0f, Shown * (1.0f - Life * Life));
                Graphics.blit(Texture, X, Y, WispU, WispV, WispWidth, WispHeight);
            }
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
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
                Fluid Liquid = menu.getFluid();
                if (Liquid == Fluids.EMPTY || menu.getInput() <= 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.input_empty", VaporizerBlockEntity.TankCapacity));
                } else {
                    Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.input", new FluidStack(Liquid, 1).getHoverName(), menu.getInput(), VaporizerBlockEntity.TankCapacity));
                }
                Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.input_hint").withStyle(ChatFormatting.GRAY));
            }
            case OutputTank -> {
                Gas Kind = menu.getGasKind();
                if (Kind == null || menu.getGas() <= 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.output_empty", VaporizerBlockEntity.GasCapacity));
                } else {
                    Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.output", Kind.getName(), menu.getGas(), VaporizerBlockEntity.GasCapacity));
                }
                Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.output_hint").withStyle(ChatFormatting.GRAY));
            }
            case PowerBar -> {
                Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.power", menu.getReceived(), menu.getDemand()));
                Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.draws", VaporizingRecipe.DefaultPower).withStyle(ChatFormatting.GRAY));
            }
            case Chamber, StatusLamp -> {
                VaporizerStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                if (menu.getProgress() > 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.liquid_vaporizer.progress", menu.getProgress() / 10).withStyle(ChatFormatting.GRAY));
                }
                return RecipeHint.with(Lines, RecipeArea.contains(GuiX, GuiY));
            }
        }
        return Lines;
    }
}
