package net.rai.scp_unleashed.advmaterials.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsBenchBlockEntity;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsMenu;
import net.rai.scp_unleashed.advmaterials.BenchStatus;
import net.rai.scp_unleashed.client.RecipeHint;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdvancedMaterialsScreen extends AbstractContainerScreen<AdvancedMaterialsMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/advanced_materials_bench.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 0;
    public static final int ArrowX = 78;
    public static final int ArrowY = 36;
    public static final int ArrowWidth = 17;
    public static final int ArrowHeight = 15;
    public static final int ArrowU = 192;
    public static final int ArrowV = 0;
    public static final int LampX = 83;
    public static final int LampY = 58;
    public static final int LampSize = 6;
    public static final int LampU = 192;
    public static final int LampV = 16;
    public static final int CoilX = 74;
    public static final int CoilY = 17;
    public static final int CoilWidth = 24;
    public static final int CoilHeight = 14;
    public static final int CoilU = 192;
    public static final int CoilV = 24;
    public static final int CoilFrames = 4;
    public static final float MillisPerTick = 50.0f;

    private enum HoverSpot {
        PowerBar(BarX - 1, BarY - 1, BarWidth + 2, BarHeight + 2),
        Arrow(ArrowX, ArrowY, ArrowWidth, ArrowHeight),
        Coil(CoilX, CoilY, CoilWidth, CoilHeight),
        StatusLamp(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2);

        private final int X;
        private final int Y;
        private final int Width;
        private final int Height;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.X = X;
            this.Y = Y;
            this.Width = Width;
            this.Height = Height;
        }

        private boolean contains(double GuiX, double GuiY) {
            return GuiX >= X && GuiX < X + Width && GuiY >= Y && GuiY < Y + Height;
        }
    }

    public AdvancedMaterialsScreen(AdvancedMaterialsMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Filled = Math.round(menu.getProgress() * ArrowWidth);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowU, ArrowV, Filled, ArrowHeight);
        }
        int Frame = coilFrame();
        if (Frame >= 0) {
            Graphics.blit(Texture, leftPos + CoilX, topPos + CoilY, CoilU, CoilV + Frame * CoilHeight, CoilWidth, CoilHeight);
        }
        int Lamp = lampIndex(menu.getStatus());
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
        int Power = menu.getDemand() <= 0 ? 0 : Math.round(Mth.clamp(menu.getReceived() / (float) Math.max(1, menu.getDemand()), 0.0f, 1.0f) * BarHeight);
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
    }

    private int coilFrame() {
        if (!menu.getStatus().isWorking()) {
            return -1;
        }
        long Ticks = (long) (Util.getMillis() / MillisPerTick);
        return (int) (Ticks / 3 % CoilFrames);
    }

    private int lampIndex(BenchStatus Status) {
        long Ticks = (long) (Util.getMillis() / MillisPerTick);
        return switch (Status) {
            case Working -> 0;
            case LowPower, Blocked -> Ticks % 20 < 12 ? 1 : -1;
            case Unpowered -> Ticks % 20 < 10 ? 2 : -1;
            default -> -1;
        };
    }

    @Nullable
    private static HoverSpot spotAt(double GuiX, double GuiY) {
        for (HoverSpot Spot : HoverSpot.values()) {
            if (Spot.contains(GuiX, GuiY)) {
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
            case PowerBar -> {
                Lines.add(Component.translatable("container.scp_unleashed.advanced_materials_bench.power", menu.getReceived(), menu.getDemand()));
                int Draw = menu.getDraw() > 0 ? menu.getDraw() : AdvancedMaterialsBenchBlockEntity.WorkDraw;
                Lines.add(Component.translatable("container.scp_unleashed.advanced_materials_bench.draws", Draw).withStyle(ChatFormatting.GRAY));
            }
            case Arrow, Coil, StatusLamp -> {
                BenchStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                if (Status != BenchStatus.Idle) {
                    Lines.add(Component.translatable("container.scp_unleashed.advanced_materials_bench.progress", Math.round(menu.getProgress() * 100.0f)));
                }
                if (menu.getTime() > 0) {
                    Lines.add(Component.translatable("container.scp_unleashed.advanced_materials_bench.time", String.format("%.1f", menu.getTime() / 20.0f)).withStyle(ChatFormatting.GRAY));
                }
                return RecipeHint.with(Lines, Spot == HoverSpot.Arrow);
            }
        }
        return Lines;
    }
}
