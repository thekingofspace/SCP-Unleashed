package net.rai.scp_unleashed.space.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.space.AirCanisterItem;
import net.rai.scp_unleashed.space.AirInfuserBlockEntity;
import net.rai.scp_unleashed.space.AirInfuserMenu;
import net.rai.scp_unleashed.space.AirInfuserStatus;
import net.rai.scp_unleashed.space.SpaceRegistry;

import java.util.ArrayList;
import java.util.List;

public class AirInfuserScreen extends AbstractContainerScreen<AirInfuserMenu> {
    public static final ResourceLocation Texture = SpaceRegistry.location("textures/gui/space/air_infuser.png");
    public static final int WindowX = 70;
    public static final int WindowY = 16;
    public static final int WindowWidth = 36;
    public static final int WindowHeight = 56;
    public static final int GaugeX = 100;
    public static final int GaugeY = 20;
    public static final int GaugeWidth = 4;
    public static final int GaugeHeight = 48;
    public static final int GaugeU = 176;
    public static final int GaugeV = 0;
    public static final int FanX = 80;
    public static final int FanY = 17;
    public static final int FanSize = 16;
    public static final int FanU = 184;
    public static final int FanFrames = 4;
    public static final int LampX = 158;
    public static final int LampY = 5;
    public static final int LampSize = 6;
    public static final int LampU = 200;
    public static final int LampV = 0;
    public static final int ArrowInX = 50;
    public static final int ArrowOutX = 108;
    public static final int ArrowY = 38;
    public static final int ArrowWidth = 16;
    public static final int ArrowHeight = 12;
    public static final int ArrowU = 200;
    public static final int ArrowV = 8;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 64;
    public static final int TicksPerSecond = 20;

    private int Spin;
    private float Speed;

    public AirInfuserScreen(AirInfuserMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    private boolean running() {
        AirInfuserStatus Status = menu.getStatus();
        return Status == AirInfuserStatus.Running || Status == AirInfuserStatus.Underpowered;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        float Target = running() ? 0.35F + 0.65F * menu.getLoad() : 0.0F;
        Speed += (Target - Speed) * 0.15F;
        Spin += Math.round(Speed * 3.0F);
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Frame = Math.floorMod(Spin / 3, FanFrames);
        Graphics.blit(Texture, leftPos + FanX, topPos + FanY, FanU, Frame * FanSize, FanSize, FanSize);
        int Fill = Math.round(menu.getFill() * GaugeHeight);
        if (menu.isLoaded() && Fill > 0) {
            Graphics.blit(Texture, leftPos + GaugeX, topPos + GaugeY + GaugeHeight - Fill, GaugeU, GaugeV + GaugeHeight - Fill, GaugeWidth, Fill);
        }
        if (running()) {
            Graphics.blit(Texture, leftPos + ArrowInX, topPos + ArrowY, ArrowU, ArrowV, ArrowWidth, ArrowHeight);
        }
        if (menu.isLoaded() && menu.getFill() >= 1.0F) {
            Graphics.blit(Texture, leftPos + ArrowOutX, topPos + ArrowY, ArrowU, ArrowV, ArrowWidth, ArrowHeight);
        }
        int Energy = Math.round(menu.getLoad() * BarHeight);
        if (Energy > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Energy, BarU, BarV + BarHeight - Energy, BarWidth, Energy);
        }
        Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + menu.getStatus().ordinal() * LampSize, LampV, LampSize, LampSize);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            return;
        }
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY) || isHovering(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, powerLines(), MouseX, MouseY);
        } else if (isHovering(GaugeX - 1, GaugeY - 1, GaugeWidth + 2, GaugeHeight + 2, MouseX, MouseY) || isHovering(FanX, FanY, FanSize, FanSize, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, RecipeHint.with(fillLines(), true), MouseX, MouseY);
        }
    }

    private List<Component> powerLines() {
        List<Component> Lines = new ArrayList<>();
        AirInfuserStatus Status = menu.getStatus();
        Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (menu.getDemand() > 0) {
            Lines.add(Component.translatable("container.scp_unleashed.processing.power", menu.getReceived(), menu.getWorkingDraw()));
        } else {
            Lines.add(Component.translatable("container.scp_unleashed.processing.idle_draw", menu.getWorkingDraw()).withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }

    private List<Component> fillLines() {
        List<Component> Lines = new ArrayList<>();
        if (!menu.isLoaded()) {
            Lines.add(Component.translatable("container.scp_unleashed.air_infuser.empty").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        Lines.add(Component.translatable("container.scp_unleashed.air_infuser.fill", Math.round(menu.getFill() * 100.0F)));
        float Load = menu.getLoad();
        if (Load > 0.0F && running()) {
            int Missing = AirCanisterItem.Capacity - menu.getAir();
            float Seconds = Missing * AirInfuserBlockEntity.fillTicks(menu.getUpgrades()) / (float) AirCanisterItem.Capacity / Load / TicksPerSecond;
            Lines.add(Component.translatable("container.scp_unleashed.processing.remaining", String.format("%.1f", Seconds)).withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }
}
