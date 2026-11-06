package net.rai.scp_unleashed.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.CoalGeneratorBlockEntity;
import net.rai.scp_unleashed.menu.CoalGeneratorMenu;

import java.util.List;

public class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/coal_generator.png");
    public static final int[] FlameX = {45, 81, 117};
    public static final int FlameY = 36;
    public static final int FlameSize = 14;
    public static final int FlameU = 176;
    public static final int FlameV = 0;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 14;

    public CoalGeneratorScreen(CoalGeneratorMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int Slot = 0; Slot < CoalGeneratorBlockEntity.SlotCount; Slot++) {
            float Fraction = menu.burnFraction(Slot);
            if (Fraction > 0.0f) {
                int Height = Math.max(1, Math.round(Fraction * FlameSize));
                Graphics.blit(Texture, leftPos + FlameX[Slot], topPos + FlameY + FlameSize - Height, FlameU, FlameV + FlameSize - Height, FlameSize, Height);
            }
        }
        int Filled = Math.round((float) menu.getStored() * BarHeight / CoalGeneratorBlockEntity.Capacity);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Filled, BarU, BarV + BarHeight - Filled, BarWidth, Filled);
        }
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("container.scp_unleashed.coal_generator.stored", menu.getStored(), CoalGeneratorBlockEntity.Capacity),
                    Component.translatable("container.scp_unleashed.coal_generator.rate", menu.getBurningSlots() * CoalGeneratorBlockEntity.PowerPerSlot)), MouseX, MouseY);
        }
    }
}
