package net.rai.scp_unleashed.centrifuge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.centrifuge.CentrifugeBlockEntity;
import net.rai.scp_unleashed.centrifuge.CentrifugeMenu;
import net.rai.scp_unleashed.centrifuge.CentrifugeStatus;

import java.util.ArrayList;
import java.util.List;

public class CentrifugeScreen extends AbstractContainerScreen<CentrifugeMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/centrifuge.png");
    public static final ResourceLocation RotorTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/centrifuge_rotor.png");
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 0;
    public static final int ArrowX = 86;
    public static final int ArrowY = 37;
    public static final int ArrowWidth = 17;
    public static final int ArrowHeight = 15;
    public static final int ArrowU = 192;
    public static final int ArrowV = 0;
    public static final int RotorX = 55;
    public static final int RotorY = 32;
    public static final int RotorSize = 24;
    public static final int RotorColumns = 10;
    public static final int RotorSheetWidth = 240;
    public static final int RotorSheetHeight = 96;
    public static final int RotorFrames = 32;
    public static final int BlurFrames = 2;
    public static final float RotorPeriod = 360.0f;
    public static final float BlurStart = 0.55f;
    public static final float GlowStart = 0.08f;
    public static final int GlowU = 216;
    public static final int GlowV = 0;
    public static final int LampX = 91;
    public static final int LampY = 56;
    public static final int LampSize = 6;
    public static final int LampU = 192;
    public static final int LampV = 16;
    public static final int ChamberX = 51;
    public static final int ChamberY = 28;
    public static final int ChamberSize = 32;

    private float Spin;
    private float PreviousSpin;
    private float Angle;
    private float PreviousAngle;
    private int Ticks;

    public CentrifugeScreen(CentrifugeMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        CentrifugeStatus Status = menu.getStatus();
        float Target = Status.isWorking() ? Mth.clamp(menu.getReceived() / (float) CentrifugeBlockEntity.WorkDraw, 0.0f, 1.0f) : 0.0f;
        PreviousSpin = Spin;
        if (Spin < Target) {
            Spin = Math.min(Target, Spin + (Target - Spin) * CentrifugeBlockEntity.SpinUpRate + CentrifugeBlockEntity.SpinUpFloor);
        } else if (Spin > Target) {
            Spin = Math.max(Target, Spin - (Spin - Target) * CentrifugeBlockEntity.SpinDownRate - CentrifugeBlockEntity.SpinDownFloor);
        }
        PreviousAngle = Angle;
        Angle += Spin * CentrifugeBlockEntity.MaxSpinDegrees;
        if (Angle >= RotorPeriod) {
            Angle -= RotorPeriod;
            PreviousAngle -= RotorPeriod;
        }
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float Shown = Mth.lerp(PartialTick, PreviousAngle, Angle);
        if (Mth.lerp(PartialTick, PreviousSpin, Spin) > GlowStart) {
            Graphics.blit(Texture, leftPos + RotorX, topPos + RotorY, GlowU, GlowV, RotorSize, RotorSize);
        }
        int Frame = Math.floorMod((int) Math.floor(Shown / (RotorPeriod / RotorFrames)), RotorFrames);
        rotorFrame(Graphics, Frame);
        float Blur = Mth.clamp((Mth.lerp(PartialTick, PreviousSpin, Spin) - BlurStart) / (1.0f - BlurStart), 0.0f, 1.0f);
        if (Blur > 0.0f) {
            RenderSystem.enableBlend();
            Graphics.setColor(1.0f, 1.0f, 1.0f, Blur * 0.85f);
            rotorFrame(Graphics, RotorFrames + Ticks % BlurFrames);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
        int Filled = Math.round(menu.getProgress() * ArrowWidth);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowU, ArrowV, Filled, ArrowHeight);
        }
        int Lamp = lampIndex(menu.getStatus());
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
        int Power = Math.round(Mth.clamp(menu.getReceived() / (float) CentrifugeBlockEntity.WorkDraw, 0.0f, 1.0f) * BarHeight);
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
    }

    private void rotorFrame(GuiGraphics Graphics, int Frame) {
        Graphics.blit(RotorTexture, leftPos + RotorX, topPos + RotorY, (Frame % RotorColumns) * RotorSize, (Frame / RotorColumns) * RotorSize,
                RotorSize, RotorSize, RotorSheetWidth, RotorSheetHeight);
    }

    private int lampIndex(CentrifugeStatus Status) {
        return switch (Status) {
            case Spinning -> 0;
            case LowPower, Blocked -> Ticks % 20 < 12 ? 1 : -1;
            case Unpowered -> Ticks % 20 < 10 ? 2 : -1;
            default -> -1;
        };
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        CentrifugeStatus Status = menu.getStatus();
        List<Component> Lines = new ArrayList<>();
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("container.scp_unleashed.centrifuge.power", menu.getReceived(), menu.getDemand()));
            Lines.add(Component.translatable("container.scp_unleashed.centrifuge.draws_active", CentrifugeBlockEntity.WorkDraw).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(ChamberX, ChamberY, ChamberSize, ChamberSize, MouseX, MouseY) || isHovering(LampX, LampY, LampSize, LampSize, MouseX, MouseY)) {
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            if (Status != CentrifugeStatus.Idle) {
                Lines.add(Component.translatable("container.scp_unleashed.centrifuge.progress", Math.round(menu.getProgress() * 100.0f)));
            }
        }
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }
}
