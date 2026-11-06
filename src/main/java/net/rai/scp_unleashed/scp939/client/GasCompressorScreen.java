package net.rai.scp_unleashed.scp939.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp939.compressor.CompressorStatus;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlockEntity;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorMenu;

import java.util.ArrayList;
import java.util.List;

public class GasCompressorScreen extends AbstractContainerScreen<GasCompressorMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/gas_compressor.png");
    public static final int GaugeX = 65;
    public static final int GaugeY = 35;
    public static final int GaugeWidth = 46;
    public static final int GaugeHeight = 16;
    public static final int GaugeU = 176;
    public static final int GaugeV = 0;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 20;
    public static final int LampX = 158;
    public static final int LampY = 5;
    public static final int LampSize = 6;
    public static final int LampU = 196;
    public static final int LampV = 20;
    public static final int IntakeX = 20;
    public static final int IntakeY = 21;
    public static final int IntakeWidth = 6;
    public static final int IntakeHeight = 44;
    public static final int IntakeU = 196;
    public static final int IntakeV = 28;
    public static final int TicksPerSecond = 20;

    public GasCompressorScreen(GasCompressorMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Width = Math.round(menu.getProgress() * GaugeWidth);
        if (Width > 0) {
            Graphics.blit(Texture, leftPos + GaugeX, topPos + GaugeY, GaugeU, GaugeV, Width, GaugeHeight);
        }
        int Filled = Math.round(menu.getLoad() * BarHeight);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Filled, BarU, BarV + BarHeight - Filled, BarWidth, Filled);
        }
        int Intake = Math.round(Math.min(1.0f, menu.getSources() / GasCompressorBlockEntity.MaxSources) * IntakeHeight);
        if (Intake > 0) {
            Graphics.blit(Texture, leftPos + IntakeX, topPos + IntakeY + IntakeHeight - Intake, IntakeU, IntakeV + IntakeHeight - Intake, IntakeWidth, Intake);
        }
        Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + menu.getStatus().getLamp() * LampSize, LampV, LampSize, LampSize);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY) || isHovering(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, powerLines(), MouseX, MouseY);
        } else if (isHovering(GaugeX, GaugeY, GaugeWidth, GaugeHeight, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, progressLines(), MouseX, MouseY);
        } else if (isHovering(IntakeX - 1, IntakeY - 1, IntakeWidth + 2, IntakeHeight + 2, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, intakeLines(), MouseX, MouseY);
        }
    }

    private List<Component> powerLines() {
        List<Component> Lines = new ArrayList<>();
        CompressorStatus Status = menu.getStatus();
        Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        Lines.add(Component.translatable("container.scp_unleashed.gas_compressor.power", menu.getReceived(), GasCompressorBlockEntity.WorkDraw)
                .withStyle(menu.getDemand() > 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        return Lines;
    }

    private List<Component> progressLines() {
        List<Component> Lines = new ArrayList<>();
        int Total = menu.getTotalTicks();
        if (Total <= 0) {
            Lines.add(Component.translatable("container.scp_unleashed.gas_compressor.waiting").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        float Progress = menu.getProgress();
        Lines.add(Component.translatable("container.scp_unleashed.gas_compressor.progress", Math.round(Progress * 100.0f)));
        float Load = menu.getLoad();
        if (Load > 0.0f && menu.getStatus() != CompressorStatus.Blocked) {
            float Seconds = (1.0f - Progress) * Total / Load / TicksPerSecond;
            Lines.add(Component.translatable("container.scp_unleashed.processing.remaining", String.format("%.1f", Seconds)).withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }

    private List<Component> intakeLines() {
        List<Component> Lines = new ArrayList<>();
        float Sources = menu.getSources();
        String Key = Sources <= 0.0f ? "none" : Sources < 1.5f ? "faint" : Sources < 3.0f ? "steady" : "strong";
        Lines.add(Component.translatable("container.scp_unleashed.gas_compressor.intake", Component.translatable("container.scp_unleashed.gas_compressor.intake." + Key)));
        return Lines;
    }
}
