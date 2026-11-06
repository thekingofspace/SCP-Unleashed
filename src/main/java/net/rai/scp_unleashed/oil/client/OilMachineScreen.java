package net.rai.scp_unleashed.oil.client;

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
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.client.GasGui;
import net.rai.scp_unleashed.oil.CombustionGeneratorBlockEntity;
import net.rai.scp_unleashed.oil.OilMachineBlockEntity;
import net.rai.scp_unleashed.oil.OilMachineKind;
import net.rai.scp_unleashed.oil.OilMachineMenu;
import net.rai.scp_unleashed.oil.OilStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class OilMachineScreen extends AbstractContainerScreen<OilMachineMenu> {
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int TankY = 19;
    public static final int TankWidth = 16;
    public static final int TankHeight = 50;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 80;
    public static final int LampSize = 6;
    public static final int LampU = 192;
    public static final int LampV = 80;
    public static final int ArrowWidth = 10;
    public static final int ArrowHeight = 7;
    public static final int ArrowU = 192;
    public static final int ArrowV = 88;
    public static final int MeterHeight = 3;
    public static final int MeterU = 176;
    public static final int MeterV = 16;
    public static final int GasTank = -1;

    protected record TankSpot(int X, int Y, int Height, int Tank) {
        static TankSpot full(int X, int Tank) {
            return new TankSpot(X, TankY, TankHeight, Tank);
        }

        boolean isGas() {
            return Tank == GasTank;
        }

        HoverArea area() {
            return new HoverArea(X - 1, Y - 1, TankWidth + 2, Height + 2);
        }
    }

    protected float Heat;
    protected float PreviousHeat;
    protected int Ticks;

    protected OilMachineScreen(OilMachineMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 72;
    }

    protected abstract ResourceLocation texture();

    protected abstract List<TankSpot> tanks();

    protected abstract HoverArea chamber();

    protected abstract int[] lamp();

    protected abstract int[][] arrows();

    protected abstract int[] meter();

    protected abstract void renderChamber(GuiGraphics Graphics, float Shown, float Time);

    protected int drawRating() {
        return menu.getPowerMax();
    }

    protected String key() {
        return "gui.scp_unleashed." + menu.getKind().getName();
    }

    protected static ResourceLocation guiTexture(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/oil/" + Name + ".png");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        PreviousHeat = Heat;
        float Target = menu.getStatus().isWorking() ? 1.0f : 0.0f;
        Heat += (Target - Heat) * (Target > Heat ? 0.06f : 0.03f);
    }

    protected boolean isGenerator() {
        return menu.getKind() == OilMachineKind.Generator;
    }

    protected int powerShare() {
        if (isGenerator()) {
            return Math.round(Mth.clamp(menu.getProgress() / (float) OilMachineBlockEntity.ProgressScale, 0.0f, 1.0f) * BarHeight);
        }
        int Demand = menu.getPowerMax();
        return Demand <= 0 ? 0 : Math.round(Mth.clamp(menu.getPower() / (float) Demand, 0.0f, 1.0f) * BarHeight);
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        ResourceLocation Texture = texture();
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (TankSpot Spot : tanks()) {
            if (Spot.isGas()) {
                int Fill = Math.round((float) menu.getGas() / Math.max(1, menu.getKind().getGasCapacity()) * Spot.Height());
                if (Fill > 0) {
                    GasGui.drawGas(Graphics, GasRegistry.ToxicGas.get(), leftPos + Spot.X(), topPos + Spot.Y() + Spot.Height() - Fill, TankWidth, Fill);
                }
                continue;
            }
            Fluid Liquid = menu.getFluid(Spot.Tank());
            int Fill = Math.round((float) menu.getAmount(Spot.Tank()) / Math.max(1, menu.getKind().capacity(Spot.Tank())) * Spot.Height());
            if (Fill > 0 && Liquid != Fluids.EMPTY) {
                FissionGuiFluids.drawFluid(Graphics, Liquid, leftPos + Spot.X(), topPos + Spot.Y() + Spot.Height() - Fill, TankWidth, Fill);
            }
        }
        float Shown = Mth.lerp(PartialTick, PreviousHeat, Heat);
        int[] Meter = meter();
        int Progress = isGenerator() ? 0 : Math.round(menu.getProgress() / (float) OilMachineBlockEntity.ProgressScale * Meter[2]);
        if (Progress > 0) {
            Graphics.blit(Texture, leftPos + Meter[0], topPos + Meter[1], MeterU, MeterV, Progress, MeterHeight);
        }
        renderChamber(Graphics, Shown, Ticks + PartialTick);
        int Power = powerShare();
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
        if (Shown > 0.05f) {
            int Lit = Math.round(Shown * ArrowWidth);
            for (int[] Arrow : arrows()) {
                Graphics.blit(Texture, leftPos + Arrow[0], topPos + Arrow[1], ArrowU, ArrowV, Lit, ArrowHeight);
            }
        }
        int Lamp = switch (menu.getStatus().getLamp()) {
            case 1 -> 0;
            case 2 -> Ticks % 20 < 12 ? 1 : -1;
            case 3 -> Ticks % 20 < 10 ? 2 : -1;
            default -> -1;
        };
        if (Lamp >= 0) {
            int[] Spot = lamp();
            Graphics.blit(Texture, leftPos + Spot[0], topPos + Spot[1], LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
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
        List<Component> Lines = tooltipAt(GuiX, GuiY);
        if (Lines != null && !Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    @Nullable
    protected List<Component> tooltipAt(double GuiX, double GuiY) {
        List<TankSpot> Spots = tanks();
        for (int Index = 0; Index < Spots.size(); Index++) {
            TankSpot Spot = Spots.get(Index);
            if (Spot.area().contains(GuiX, GuiY)) {
                return tankTooltip(Spot);
            }
        }
        if (new HoverArea(BarX - 1, BarY - 1, BarWidth + 2, BarHeight + 2).contains(GuiX, GuiY)) {
            return powerTooltip();
        }
        int[] Spot = lamp();
        HoverArea Chamber = chamber();
        if (Chamber.contains(GuiX, GuiY) || new HoverArea(Spot[0] - 1, Spot[1] - 1, LampSize + 2, LampSize + 2).contains(GuiX, GuiY)) {
            List<Component> Lines = new ArrayList<>();
            OilStatus Status = menu.getStatus();
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            if (!isGenerator() && menu.getProgress() > 0) {
                Lines.add(Component.translatable("gui.scp_unleashed.oil.progress", menu.getProgress() / 10).withStyle(ChatFormatting.GRAY));
            }
            appendChamberLines(Lines);
            return RecipeHint.with(Lines, Chamber.contains(GuiX, GuiY));
        }
        return null;
    }

    protected void appendChamberLines(List<Component> Lines) {
    }

    protected List<Component> tankTooltip(TankSpot Spot) {
        List<Component> Lines = new ArrayList<>();
        OilMachineKind Kind = menu.getKind();
        if (Spot.isGas()) {
            int Amount = menu.getGas();
            if (Amount <= 0) {
                Lines.add(Component.translatable("gui.scp_unleashed.oil.gas_empty", Kind.getGasCapacity()));
            } else {
                Lines.add(Component.translatable("gui.scp_unleashed.oil.tank", GasRegistry.ToxicGas.get().getName(), Amount, Kind.getGasCapacity()));
            }
            Lines.add(Component.translatable(key() + ".exhaust_hint").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        Fluid Liquid = menu.getFluid(Spot.Tank());
        int Amount = menu.getAmount(Spot.Tank());
        int Capacity = Kind.capacity(Spot.Tank());
        if (Liquid == Fluids.EMPTY || Amount <= 0) {
            Lines.add(Component.translatable("gui.scp_unleashed.oil.tank_empty", Component.translatable(key() + ".tank" + Spot.Tank()), Capacity));
        } else {
            Lines.add(Component.translatable("gui.scp_unleashed.oil.tank", new FluidStack(Liquid, 1).getHoverName(), Amount, Capacity));
        }
        Lines.add(Component.translatable(key() + ".tank" + Spot.Tank() + "_hint").withStyle(ChatFormatting.GRAY));
        return Lines;
    }

    protected List<Component> powerTooltip() {
        List<Component> Lines = new ArrayList<>();
        if (isGenerator()) {
            Lines.add(Component.translatable("gui.scp_unleashed.oil.generating", menu.getPower()));
            int Stored = Math.round(menu.getProgress() / (float) OilMachineBlockEntity.ProgressScale * CombustionGeneratorBlockEntity.Capacity);
            Lines.add(Component.translatable("gui.scp_unleashed.oil.buffer", Stored, CombustionGeneratorBlockEntity.Capacity).withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        Lines.add(Component.translatable("gui.scp_unleashed.oil.power", menu.getPower(), menu.getPowerMax()));
        Lines.add(Component.translatable("gui.scp_unleashed.oil.draws", drawRating()).withStyle(ChatFormatting.GRAY));
        return Lines;
    }
}
