package net.rai.scp_unleashed.sulfur.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fission.client.FissionGuiFluids;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlockEntity;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankMenu;
import net.rai.scp_unleashed.sulfur.tank.TankStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IndustrialTankScreen extends AbstractContainerScreen<IndustrialTankMenu> {
    public static final ResourceLocation Texture = SulfurRegistry.location("textures/gui/sulfur/industrial_tank.png");
    public static final String Prefix = "gui.scp_unleashed.industrial_tank.";
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int WaterX = 31;
    public static final int AcidX = 125;
    public static final int TankY = 19;
    public static final int TankWidth = 16;
    public static final int TankHeight = 50;
    public static final int ChamberX = 60;
    public static final int ChamberY = 18;
    public static final int ChamberWidth = 52;
    public static final int ChamberHeight = 50;
    public static final int PoolTop = 44;
    public static final int ShaftX = 85;
    public static final int ShaftTop = 26;
    public static final int PaddleY = 56;
    public static final int PaddleHeight = 3;
    public static final int[] PaddleWidths = {14, 10, 2, 10};
    public static final int PaddleFrameTicks = 2;
    public static final int ShaftLight = 0xFF9AA2AC;
    public static final int ShaftDark = 0xFF545A63;
    public static final int PaddleLight = 0xFFDAB058;
    public static final int PaddleDark = 0xFF8C6428;
    public static final int BubbleU = 182;
    public static final int BubbleV = 8;
    public static final int BubbleSize = 3;
    public static final int Bubbles = 7;
    public static final int MeterX = 66;
    public static final int MeterY = 21;
    public static final int MeterWidth = 40;
    public static final int MeterHeight = 3;
    public static final int MeterU = 176;
    public static final int MeterV = 16;
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
    public static final int TicksPerSecond = 20;
    public static final HoverArea RecipeArea = new HoverArea(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

    private enum HoverSpot {
        WaterTank(WaterX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        AcidTank(AcidX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        PowerBar(BarX - 1, BarY - 1, BarWidth + 2, BarHeight + 2),
        StatusLamp(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2),
        Chamber(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

        private final HoverArea Area;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.Area = new HoverArea(X, Y, Width, Height);
        }
    }

    private float Stir;
    private float PreviousStir;
    private int Ticks;

    public IndustrialTankScreen(IndustrialTankMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 72;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        PreviousStir = Stir;
        float Target = menu.getStatus().isWorking() ? 1.0f : 0.0f;
        Stir += (Target - Stir) * (Target > Stir ? 0.08f : 0.04f);
    }

    private static int column(int Amount, int Capacity) {
        return Amount <= 0 ? 0 : Math.max(1, Math.round((float) Amount / Capacity * TankHeight));
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Water = column(menu.getWater(), IndustrialTankBlockEntity.WaterCapacity);
        if (Water > 0) {
            FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, leftPos + WaterX, topPos + TankY + TankHeight - Water, TankWidth, Water);
        }
        int Acid = column(menu.getAcid(), IndustrialTankBlockEntity.AcidCapacity);
        if (Acid > 0) {
            FissionGuiFluids.drawFluid(Graphics, SulfurRegistry.Acid.get(), leftPos + AcidX, topPos + TankY + TankHeight - Acid, TankWidth, Acid);
        }
        float Shown = Mth.lerp(PartialTick, PreviousStir, Stir);
        renderChamber(Graphics, Shown, Ticks + PartialTick);
        int Power = Math.round(menu.getLoad() * BarHeight);
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
        if (Shown > 0.05f) {
            int Lit = Math.round(Shown * ArrowWidth);
            Graphics.blit(Texture, leftPos + InArrowX, topPos + ArrowY, ArrowU, ArrowV, Lit, ArrowHeight);
            Graphics.blit(Texture, leftPos + OutArrowX, topPos + ArrowY, ArrowU, ArrowV, Lit, ArrowHeight);
        }
        TankStatus Status = menu.getStatus();
        int Lamp = Status.getLamp();
        boolean Blink = Status != TankStatus.Running && Ticks % 20 >= 12;
        if (Lamp >= 0 && !Blink) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
    }

    private void renderChamber(GuiGraphics Graphics, float Shown, float Time) {
        int Left = leftPos + ChamberX;
        int Top = topPos + ChamberY;
        int Bottom = Top + ChamberHeight;
        int Pool = topPos + PoolTop;
        float Mix = menu.getProgress() / (float) IndustrialTankBlockEntity.ProgressSteps;
        if (menu.getWater() > 0 || Mix > 0.0f) {
            FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, Left, Pool, ChamberWidth, Bottom - Pool);
            if (Mix > 0.0f) {
                int Alpha = Math.round(Mth.clamp(0.3f + Mix * 0.7f, 0.0f, 1.0f) * 255.0f);
                FissionGuiFluids.drawFluid(Graphics, SulfuricAcidExtensions.Still, (Alpha << 24) | 0xFFFFFF, Left, Pool, ChamberWidth, Bottom - Pool);
            }
        }
        int Progress = Math.round(Mix * MeterWidth);
        if (Progress > 0) {
            Graphics.blit(Texture, leftPos + MeterX, topPos + MeterY, MeterU, MeterV, Progress, MeterHeight);
        }
        int Shaft = leftPos + ShaftX;
        Graphics.fill(Shaft, topPos + ShaftTop, Shaft + 1, topPos + PaddleY, ShaftLight);
        Graphics.fill(Shaft + 1, topPos + ShaftTop, Shaft + 2, topPos + PaddleY, ShaftDark);
        int Frame = Shown > 0.05f ? (int) (Time / PaddleFrameTicks) % PaddleWidths.length : 0;
        int Half = PaddleWidths[Frame] / 2;
        Graphics.fill(Shaft + 1 - Half, topPos + PaddleY, Shaft + 1 + Half, topPos + PaddleY + PaddleHeight - 1, PaddleLight);
        Graphics.fill(Shaft + 1 - Half, topPos + PaddleY + PaddleHeight - 1, Shaft + 1 + Half, topPos + PaddleY + PaddleHeight, PaddleDark);
        if (Shown <= 0.02f) {
            return;
        }
        Graphics.enableScissor(Left, Pool, Left + ChamberWidth, Bottom);
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
        int Rise = Bottom - Pool;
        for (int Index = 0; Index < Bubbles; Index++) {
            float Speed = 0.5f + (Index * 37 % 7) * 0.07f;
            float Travel = (Time * Speed + Index * 6.1f) % Rise;
            int X = Left + 3 + (Index * 19 + 4) % (ChamberWidth - 6);
            int Y = Bottom - BubbleSize - Math.round(Travel);
            Graphics.blit(Texture, X, Y, BubbleU, BubbleV, BubbleSize, BubbleSize);
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
            case WaterTank -> {
                int Water = menu.getWater();
                Lines.add(Water <= 0 ? Component.translatable(Prefix + "water_empty", IndustrialTankBlockEntity.WaterCapacity)
                        : Component.translatable(Prefix + "water", Water, IndustrialTankBlockEntity.WaterCapacity));
                Lines.add(Component.translatable(Prefix + "water_hint").withStyle(ChatFormatting.GRAY));
            }
            case AcidTank -> {
                int Acid = menu.getAcid();
                Lines.add(Acid <= 0 ? Component.translatable(Prefix + "acid_empty", IndustrialTankBlockEntity.AcidCapacity)
                        : Component.translatable(Prefix + "acid", Acid, IndustrialTankBlockEntity.AcidCapacity));
                Lines.add(Component.translatable(Prefix + "acid_hint").withStyle(ChatFormatting.GRAY));
            }
            case PowerBar -> {
                Lines.add(Component.translatable(Prefix + "power", menu.getReceived(), menu.getDemand()));
                Lines.add(Component.translatable(Prefix + "draws", IndustrialTankBlockEntity.WorkDraw).withStyle(ChatFormatting.GRAY));
            }
            case StatusLamp, Chamber -> {
                TankStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                int Progress = menu.getProgress();
                if (Progress > 0) {
                    Lines.add(Component.translatable(Prefix + "progress", Progress / 10).withStyle(ChatFormatting.GRAY));
                }
                float Load = menu.getLoad();
                if (Status.isWorking() && Load > 0.0f) {
                    float Seconds = (1.0f - Progress / (float) IndustrialTankBlockEntity.ProgressSteps) * IndustrialTankBlockEntity.BatchTicks / Load / TicksPerSecond;
                    Lines.add(Component.translatable("container.scp_unleashed.processing.remaining", String.format(Locale.ROOT, "%.1f", Seconds)).withStyle(ChatFormatting.GRAY));
                }
                return RecipeHint.with(Lines, RecipeArea.contains(GuiX, GuiY));
            }
        }
        return Lines;
    }
}
