package net.rai.scp_unleashed.processing.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.processing.MachineStatus;
import net.rai.scp_unleashed.processing.ProcessingMenu;

import java.util.ArrayList;
import java.util.List;

public abstract class ProcessingScreen<T extends ProcessingMenu> extends AbstractContainerScreen<T> {
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int LampX = 158;
    public static final int LampY = 5;
    public static final int LampSize = 6;
    public static final int TicksPerSecond = 20;

    private final ResourceLocation Texture;
    private final int BarU;
    private final int BarV;
    private final int LampU;
    private final int LampV;

    protected ProcessingScreen(T Menu, Inventory PlayerInventory, Component Title, ResourceLocation Texture, int BarU, int BarV, int LampU, int LampV) {
        super(Menu, PlayerInventory, Title);
        this.Texture = Texture;
        this.BarU = BarU;
        this.BarV = BarV;
        this.LampU = LampU;
        this.LampV = LampV;
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    protected ResourceLocation getTexture() {
        return Texture;
    }

    protected abstract void renderMachine(GuiGraphics Graphics, float PartialTick);

    protected abstract boolean isOverProgress(int MouseX, int MouseY);

    protected boolean isOverRecipeArea(int MouseX, int MouseY) {
        return isOverProgress(MouseX, MouseY);
    }

    protected abstract String getActionKey();

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderMachine(Graphics, PartialTick);
        int Filled = Math.round(menu.getLoad() * BarHeight);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Filled, BarU, BarV + BarHeight - Filled, BarWidth, Filled);
        }
        Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + menu.getStatus().ordinal() * LampSize, LampV, LampSize, LampSize);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY) || isHovering(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, powerLines(), MouseX, MouseY);
        } else if (isOverProgress(MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, RecipeHint.with(progressLines(), isOverRecipeArea(MouseX, MouseY)), MouseX, MouseY);
        }
    }

    private List<Component> powerLines() {
        List<Component> Lines = new ArrayList<>();
        MachineStatus Status = menu.getStatus();
        Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (menu.getDemand() > 0) {
            Lines.add(Component.translatable("container.scp_unleashed.processing.power", menu.getReceived(), menu.getWorkingDraw()));
        } else {
            Lines.add(Component.translatable("container.scp_unleashed.processing.idle_draw", menu.getWorkDraw()).withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }

    private List<Component> progressLines() {
        List<Component> Lines = new ArrayList<>();
        int Total = menu.getTotalTicks();
        if (Total <= 0) {
            Lines.add(Component.translatable("container.scp_unleashed.processing.waiting").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        float Progress = menu.getProgress();
        Lines.add(Component.translatable(getActionKey(), Math.round(Progress * 100.0f)));
        float Load = menu.getLoad();
        if (Load > 0.0f && menu.getStatus() != MachineStatus.Blocked) {
            float Seconds = (1.0f - Progress) * Total / Load / TicksPerSecond;
            Lines.add(Component.translatable("container.scp_unleashed.processing.remaining", String.format("%.1f", Seconds)).withStyle(ChatFormatting.GRAY));
        }
        Lines.add(Component.translatable("container.scp_unleashed.processing.duration", String.format("%.1f", Total / (float) TicksPerSecond)).withStyle(ChatFormatting.DARK_GRAY));
        return Lines;
    }
}
