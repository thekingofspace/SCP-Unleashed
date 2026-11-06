package net.rai.scp_unleashed.dclass.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;
import net.rai.scp_unleashed.dclass.CloningMenu;
import net.rai.scp_unleashed.dclass.CloningStatus;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CloningScreen extends AbstractContainerScreen<CloningMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/dclass/cloning_machine.png");
    public static final int TankX = 50;
    public static final int TankY = 18;
    public static final int TankWidth = 64;
    public static final int TankHeight = 50;
    public static final int LcdX = 50;
    public static final int LcdY = 70;
    public static final int LcdWidth = 64;
    public static final int LcdHeight = 11;
    public static final int GaugeX = 120;
    public static final int GaugeY = 18;
    public static final int GaugeWidth = 8;
    public static final int GaugeHeight = 63;
    public static final int GaugeU = 176;
    public static final int GaugeV = 0;
    public static final int BarX = 153;
    public static final int BarY = 21;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 184;
    public static final int BarV = 0;
    public static final int LampX = 136;
    public static final int LampY = 22;
    public static final int LampSpacing = 13;
    public static final int LampSize = 8;
    public static final int LampU = 200;
    public static final int LampV = 0;
    public static final float FigureScale = 18.5f;
    public static final float FigureFloor = 3.25f;
    public static final float FigureBob = 1.0f;
    public static final float FigureTurn = 20.0f;
    public static final float FigureSwing = 12.0f;
    public static final float FigureFacing = 180.0f;
    public static final float FrontLayer = 100.0f;
    public static final int GlassU = 176;
    public static final int GlassV = 80;
    public static final int FluidColour = 0x78D06A58;
    public static final int FluidTopColour = 0xA0F0A088;
    public static final int BubbleColour = 0xB0FFE8E0;
    public static final int LcdColour = 0xFF8CFFB4;
    public static final int LcdDimColour = 0xFF3E8A5E;
    public static final int LcdWarnColour = 0xFFFFB347;
    public static final int ImageHeight = 178;
    public static final int InventoryLabelY = 84;
    private static final String Prefix = "gui.scp_unleashed.cloning_machine.";

    public CloningScreen(CloningMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = ImageHeight;
        inventoryLabelY = InventoryLabelY;
    }

    private float progress(float Partial) {
        CloningMachineBlockEntity Tank = menu.tank();
        return Tank != null ? Tank.shownProgress(Partial) : menu.getProgress();
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float Partial, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        tank(Graphics, Partial);
        int Grown = Math.round(progress(Partial) * GaugeHeight);
        if (Grown > 0) {
            Graphics.blit(Texture, leftPos + GaugeX, topPos + GaugeY + GaugeHeight - Grown, GaugeU, GaugeV + GaugeHeight - Grown, GaugeWidth, Grown);
        }
        int Power = Math.min(BarHeight, Math.round((float) menu.getReceived() * BarHeight / CloningMachineBlockEntity.WorkingDraw));
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
        CloningStatus Status = menu.getStatus();
        boolean Blink = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) / 8 % 2 == 0;
        lamp(Graphics, 0, Status != CloningStatus.Off);
        lamp(Graphics, 1, Status == CloningStatus.Growing || Status == CloningStatus.Releasing);
        lamp(Graphics, 2, (Status == CloningStatus.Blocked || Status == CloningStatus.TrayFull) && Blink);
        lcd(Graphics, Status);
    }

    private void tank(GuiGraphics Graphics, float Partial) {
        int X0 = leftPos + TankX;
        int Y0 = topPos + TankY;
        CloningMachineBlockEntity Tank = menu.tank();
        float Fill = Tank == null ? (menu.getKind() == CloningKind.None ? 0.0f : 1.0f) : Tank.fill(Partial);
        Graphics.enableScissor(X0, Y0, X0 + TankWidth, Y0 + TankHeight);
        DClassLike Figure = Tank == null || Tank.growing() == CloningKind.None ? null : CloningMachineRenderer.figure(Tank);
        if (Figure != null) {
            figure(Graphics, Tank, Figure, X0, Y0, Partial);
        }
        Graphics.pose().pushPose();
        Graphics.pose().translate(0.0f, 0.0f, FrontLayer);
        int Level = Math.round(Fill * TankHeight);
        if (Level > 0) {
            int Top = Y0 + TankHeight - Level;
            Graphics.fill(X0, Top, X0 + TankWidth, Y0 + TankHeight, FluidColour);
            Graphics.fill(X0, Top, X0 + TankWidth, Top + 1, FluidTopColour);
            int Ticks = Tank == null ? 0 : Tank.clientTicks();
            for (int Index = 0; Index < 7; Index++) {
                int Column = X0 + 4 + (Index * 23) % (TankWidth - 8);
                int Rise = (Ticks / 2 + Index * 17) % Math.max(1, Level);
                int Row = Y0 + TankHeight - 2 - Rise;
                if (Row > Top + 1) {
                    int Size = Index % 3 == 0 ? 2 : 1;
                    Graphics.fill(Column, Row, Column + Size, Row + Size, BubbleColour);
                }
            }
        }
        Graphics.disableScissor();
        Graphics.blit(Texture, X0, Y0, GlassU, GlassV, TankWidth, TankHeight);
        Graphics.pose().popPose();
    }

    private void figure(GuiGraphics Graphics, CloningMachineBlockEntity Tank, DClassLike Figure, int X0, int Y0, float Partial) {
        float Grown = progress(Partial);
        float Eased = Grown * Grown * (3.0f - 2.0f * Grown);
        float Scale = FigureScale * (CloningMachineRenderer.SeedScale + (1.0f - CloningMachineRenderer.SeedScale) * Eased);
        float Time = (Tank.clientTicks() + Partial) * 0.06f;
        float Bottom = Y0 + TankHeight - FigureFloor - (Mth.sin(Time) + 1.0f) * 0.5f * FigureBob;
        float Spin = FigureTurn + Mth.sin(Time * 0.5f) * FigureSwing;
        Quaternionf Turn = new Quaternionf().rotateZ(Mth.PI).rotateY(Spin * Mth.DEG_TO_RAD);
        float Yaw = Figure.getYRot();
        float YawO = Figure.yRotO;
        float Body = Figure.yBodyRot;
        float BodyO = Figure.yBodyRotO;
        float Head = Figure.yHeadRot;
        float HeadO = Figure.yHeadRotO;
        Figure.setYRot(FigureFacing);
        Figure.yRotO = FigureFacing;
        Figure.yBodyRot = FigureFacing;
        Figure.yBodyRotO = FigureFacing;
        Figure.yHeadRot = FigureFacing;
        Figure.yHeadRotO = FigureFacing;
        InventoryScreen.renderEntityInInventory(Graphics, X0 + TankWidth / 2.0f, Bottom, Scale, new Vector3f(), Turn, null, Figure);
        Figure.setYRot(Yaw);
        Figure.yRotO = YawO;
        Figure.yBodyRot = Body;
        Figure.yBodyRotO = BodyO;
        Figure.yHeadRot = Head;
        Figure.yHeadRotO = HeadO;
    }

    private void lamp(GuiGraphics Graphics, int Index, boolean On) {
        if (On) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY + Index * LampSpacing, LampU + Index * LampSize, LampV, LampSize, LampSize);
        }
    }

    private void lcd(GuiGraphics Graphics, CloningStatus Status) {
        Component Text;
        int Colour = LcdColour;
        switch (Status) {
            case Off -> {
                Text = Component.translatable(menu.getKind() != CloningKind.None ? Prefix + "lcd.no_power" : Prefix + "lcd.off");
                Colour = menu.getKind() != CloningKind.None ? LcdWarnColour : LcdDimColour;
            }
            case Blocked -> {
                Text = Component.translatable(Prefix + "lcd.blocked");
                Colour = LcdWarnColour;
            }
            case TrayFull -> {
                Text = Component.translatable(Prefix + "lcd.tray");
                Colour = LcdWarnColour;
            }
            case Releasing -> Text = Component.translatable(Prefix + "lcd.release");
            case Growing -> Text = Component.literal(Math.round(progress(0.0f) * 100.0f) + "%");
            default -> {
                Text = Component.translatable(Prefix + "lcd.ready");
                Colour = LcdDimColour;
            }
        }
        int Width = font.width(Text);
        Graphics.drawString(font, Text, leftPos + LcdX + (LcdWidth - Width + 1) / 2, topPos + LcdY + 2, Colour, false);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        if (hoveredSlot != null && hoveredSlot.hasItem() || !menu.getCarried().isEmpty()) {
            renderTooltip(Graphics, MouseX, MouseY);
            return;
        }
        List<Component> Lines = null;
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY)) {
            Lines = powerLines();
        } else if (isHovering(GaugeX, GaugeY, GaugeWidth, GaugeHeight, MouseX, MouseY) || isHovering(LcdX, LcdY, LcdWidth, LcdHeight, MouseX, MouseY)
                || isHovering(TankX, TankY, TankWidth, TankHeight, MouseX, MouseY)) {
            Lines = progressLines();
        } else if (isHovering(LampX, LampY, LampSize, LampSpacing * 2 + LampSize, MouseX, MouseY)) {
            Lines = List.of(statusLine(menu.getStatus()));
        }
        if (Lines != null) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        } else {
            renderTooltip(Graphics, MouseX, MouseY);
        }
    }

    private List<Component> powerLines() {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable(Prefix + "power", menu.getReceived(), menu.getDemand()));
        Lines.add(Component.translatable(Prefix + "draw", CloningMachineBlockEntity.WorkingDraw).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + "idle_draw").withStyle(ChatFormatting.DARK_GRAY));
        return Lines;
    }

    private List<Component> progressLines() {
        List<Component> Lines = new ArrayList<>();
        CloningKind Kind = menu.getKind();
        if (Kind == CloningKind.None) {
            Lines.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
            Lines.add(statusLine(menu.getStatus()));
            return Lines;
        }
        CloningMachineBlockEntity Tank = menu.tank();
        Lines.add(Component.translatable(Prefix + "growing." + Kind.getKey(), Tank == null ? "" : Tank.sampleName()));
        float Progress = menu.getProgress();
        Lines.add(Component.translatable(Prefix + "progress", Math.round(Progress * 100.0f)).withStyle(ChatFormatting.GOLD));
        float Rate = Math.min(1.0f, (float) menu.getReceived() / CloningMachineBlockEntity.WorkingDraw);
        if (menu.getStatus() == CloningStatus.Growing && Rate > 0.0f) {
            float Seconds = (1.0f - Progress) * CloningMachineBlockEntity.CycleTicks / Rate / 20.0f;
            Lines.add(Component.translatable(Prefix + "remaining", String.format("%.0f", Seconds)).withStyle(ChatFormatting.GRAY));
            if (Rate < 0.99f) {
                Lines.add(Component.translatable(Prefix + "slowed", Math.round(Rate * 100.0f)).withStyle(ChatFormatting.GOLD));
            }
        } else {
            Lines.add(statusLine(menu.getStatus()));
        }
        return Lines;
    }

    private Component statusLine(CloningStatus Status) {
        ChatFormatting Colour = switch (Status) {
            case Growing, Releasing -> ChatFormatting.GOLD;
            case Idle -> ChatFormatting.GREEN;
            case Blocked, TrayFull -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_GRAY;
        };
        return Component.translatable(Prefix + "status." + Status.getKey()).withStyle(Colour);
    }
}
