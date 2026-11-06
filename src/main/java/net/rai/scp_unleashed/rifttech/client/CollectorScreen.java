package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.CollectorStatus;
import net.rai.scp_unleashed.rifttech.CollectorTuning;
import net.rai.scp_unleashed.rifttech.RiftCollectorMenu;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CollectorScreen extends AbstractContainerScreen<RiftCollectorMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/rifttech/rift_collector.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 188;
    public static final int RadarX = 8;
    public static final int RadarY = 18;
    public static final int RadarSize = 58;
    public static final int RadarCentreX = 37;
    public static final int RadarCentreY = 47;
    public static final int RadarRadius = 25;
    public static final int LcdX = 70;
    public static final int LcdY = 18;
    public static final int LcdWidth = 82;
    public static final int LcdHeight = 30;
    public static final int ProgressX = 70;
    public static final int ProgressY = 51;
    public static final int ProgressWidth = 82;
    public static final int ProgressHeight = 4;
    public static final int ProgressU = 0;
    public static final int ProgressV = 192;
    public static final int PowerX = 156;
    public static final int PowerY = 18;
    public static final int PowerWidth = 12;
    public static final int PowerHeight = 58;
    public static final int PowerU = 176;
    public static final int PowerV = 0;
    public static final int LensX = 73;
    public static final int LensY = 60;
    public static final int LensSize = 14;
    public static final int LensStep = 17;
    public static final int LensU = 190;
    public static final int LensV = 0;
    public static final int LampX = 73;
    public static final int LampY = 21;
    public static final int LampSize = 6;
    public static final int LampU = 204;
    public static final int LampV = 0;
    public static final int ReadoutColour = 0xFF8CFFA0;
    public static final int ReadoutDim = 0xFF3F7A4C;

    private int Ticks;
    private float Sweep;
    private float SweepOld;

    public CollectorScreen(RiftCollectorMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 95;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        SweepOld = Sweep;
        if (menu.getStatus().isPowered()) {
            Sweep += menu.getStatus() == CollectorStatus.Collecting ? 9.0f : 4.0f;
            if (Sweep >= 360.0f) {
                Sweep -= 360.0f;
                SweepOld -= 360.0f;
            }
        }
    }

    private int targetColour() {
        if (menu.isApocalyptic()) {
            return ApocalypticRiftEntity.Colour;
        }
        FragmentRarity Grade = menu.getTargetRarity();
        return Grade == null ? 0xFF8CFFA0 : Grade.getColour();
    }

    private static int argb(float Alpha, int Colour) {
        return (Mth.clamp(Math.round(Alpha * 255.0f), 0, 255) << 24) | (Colour & 0xFFFFFF);
    }

    private static int scaled(int Colour, float Factor) {
        int Red = Mth.clamp(Math.round(((Colour >> 16) & 255) * Factor), 0, 255);
        int Green = Mth.clamp(Math.round(((Colour >> 8) & 255) * Factor), 0, 255);
        int Blue = Mth.clamp(Math.round((Colour & 255) * Factor), 0, 255);
        return (Red << 16) | (Green << 8) | Blue;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        CollectorStatus Status = menu.getStatus();
        RenderSystem.enableBlend();
        radar(Graphics, PartialTick, Status);
        float Progress = menu.getProgress();
        int Filled = Math.round(Progress * ProgressWidth);
        if (Filled > 0 && Status.hasTarget()) {
            int Colour = targetColour();
            float Beat = 0.8f + 0.2f * Mth.sin((Ticks + PartialTick) * 0.3f);
            Graphics.setColor(((Colour >> 16) & 255) / 255.0f * Beat, ((Colour >> 8) & 255) / 255.0f * Beat, (Colour & 255) / 255.0f * Beat, 1.0f);
            Graphics.blit(Texture, leftPos + ProgressX, topPos + ProgressY, ProgressU, ProgressV, Filled, ProgressHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        int Demand = menu.getDemand();
        int Power = Demand <= 0 ? 0 : Math.round(Mth.clamp(menu.getReceived() / (float) Demand, 0.0f, 1.0f) * PowerHeight);
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + PowerX, topPos + PowerY + PowerHeight - Power, PowerU, PowerV + PowerHeight - Power, PowerWidth, Power);
        }
        for (int Index = 0; Index < UpgradeType.Range.getLimit(); Index++) {
            if (Index < menu.getRangeUpgrades()) {
                Graphics.blit(Texture, leftPos + LensX, topPos + LensY + Index * LensStep, LensU, LensV, LensSize, LensSize);
            }
        }
        int Lamp = lampIndex(Status);
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
        RenderSystem.disableBlend();
    }

    private int lampIndex(CollectorStatus Status) {
        return switch (Status) {
            case Collecting -> 0;
            case Tracking -> Ticks % 10 < 6 ? 1 : -1;
            case LowPower, Full, Blocked -> Ticks % 20 < 12 ? 1 : -1;
            case Unpowered -> Ticks % 20 < 10 ? 2 : -1;
            default -> 3;
        };
    }

    private void radar(GuiGraphics Graphics, float PartialTick, CollectorStatus Status) {
        VertexConsumer Consumer = Graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f Matrix = Graphics.pose().last().pose();
        int Cx = leftPos + RadarCentreX;
        int Cy = topPos + RadarCentreY;
        boolean Powered = Status.isPowered();
        if (Powered) {
            float Angle = Mth.lerp(PartialTick, SweepOld, Sweep);
            for (int Trail = 0; Trail < 4; Trail++) {
                line(Consumer, Matrix, Cx, Cy, Angle - Trail * 6.0f, RadarRadius - 1, argb(0.45f - Trail * 0.1f, 0x8CFFA0));
            }
        }
        if (Status.hasTarget()) {
            float Scale = RadarRadius / (float) menu.getRange();
            int X = Cx + Math.round(Mth.clamp(menu.getOffsetX() * Scale, -RadarRadius, RadarRadius));
            int Y = Cy + Math.round(Mth.clamp(menu.getOffsetZ() * Scale, -RadarRadius, RadarRadius));
            int Colour = targetColour();
            float Blink = 0.75f + 0.25f * Mth.sin((Ticks + PartialTick) * 0.4f);
            int Halo = argb(0.9f, scaled(Colour, Blink * 0.7f));
            for (int OffsetY = -1; OffsetY <= 1; OffsetY++) {
                for (int OffsetX = -1; OffsetX <= 1; OffsetX++) {
                    if (OffsetX != 0 || OffsetY != 0) {
                        pixel(Consumer, Matrix, X + OffsetX, Y + OffsetY, Halo);
                    }
                }
            }
            pixel(Consumer, Matrix, X, Y, argb(1.0f, scaled(Colour, 1.25f)));
            if (Status == CollectorStatus.Collecting || Status == CollectorStatus.LowPower) {
                ray(Consumer, Matrix, Cx, Cy, X, Y, argb(0.55f, Colour));
            }
        }
        int Centre = argb(1.0f, Powered ? 0xDCE6E0 : 0x6C7470);
        for (int OffsetY = -1; OffsetY <= 0; OffsetY++) {
            for (int OffsetX = -1; OffsetX <= 0; OffsetX++) {
                pixel(Consumer, Matrix, Cx + OffsetX, Cy + OffsetY, Centre);
            }
        }
        Graphics.flush();
    }

    private static void pixel(VertexConsumer Consumer, Matrix4f Matrix, int X, int Y, int Colour) {
        Consumer.addVertex(Matrix, X, Y, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X, Y + 1, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X + 1, Y + 1, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X + 1, Y, 0.0f).setColor(Colour);
    }

    private static void line(VertexConsumer Consumer, Matrix4f Matrix, int Cx, int Cy, float Degrees, int Length, int Colour) {
        double Angle = Math.toRadians(Degrees);
        double DirectionX = Math.sin(Angle);
        double DirectionY = -Math.cos(Angle);
        int LastX = Integer.MIN_VALUE;
        int LastY = Integer.MIN_VALUE;
        for (int Step = 2; Step <= Length; Step++) {
            int X = Cx + (int) Math.round(DirectionX * Step);
            int Y = Cy + (int) Math.round(DirectionY * Step);
            if (X == LastX && Y == LastY) {
                continue;
            }
            LastX = X;
            LastY = Y;
            pixel(Consumer, Matrix, X, Y, Colour);
        }
    }

    private static void ray(VertexConsumer Consumer, Matrix4f Matrix, int FromX, int FromY, int ToX, int ToY, int Colour) {
        int Steps = Math.max(Math.abs(ToX - FromX), Math.abs(ToY - FromY));
        for (int Step = 2; Step < Steps - 1; Step++) {
            float Along = Step / (float) Steps;
            pixel(Consumer, Matrix, Math.round(FromX + (ToX - FromX) * Along), Math.round(FromY + (ToY - FromY) * Along), Colour);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
        CollectorStatus Status = menu.getStatus();
        Integer StatusColour = Status.getColour().getColor();
        Graphics.drawString(font, Component.translatable(Status.getTranslationKey()), LcdX + 11, LcdY + 3, 0xFF000000 | (StatusColour == null ? 0xAAAAAA : StatusColour), false);
        Graphics.drawString(font, readout(Status), LcdX + 3, LcdY + 14, Status.isPowered() ? ReadoutColour : ReadoutDim, false);
        String Range = Component.translatable("gui.scp_unleashed.rift_collector.range_short", menu.getRange()).getString();
        Graphics.drawString(font, Range, LcdX + LcdWidth - 3 - font.width(Range), LcdY + 3, ReadoutDim, false);
    }

    private Component readout(CollectorStatus Status) {
        if (Status.hasTarget()) {
            return Component.translatable("gui.scp_unleashed.rift_collector.readout", rarityName(), Math.round(menu.getProgress() * 100.0f));
        }
        return Component.translatable("gui.scp_unleashed.rift_collector.collected", menu.getCollected());
    }

    private MutableComponent rarityName() {
        String Label = menu.isApocalyptic() ? ApocalypticRiftEntity.Label : menu.getTargetRarity() == null ? "faint" : menu.getTargetRarity().getSerializedName();
        return Component.translatable("hud.scp_unleashed.extraction.rarity." + Label).withStyle(Style.EMPTY.withColor(targetColour() & 0xFFFFFF));
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        List<Component> Lines = tooltip(MouseX, MouseY);
        if (Lines.isEmpty()) {
            renderTooltip(Graphics, MouseX, MouseY);
        } else {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> tooltip(int MouseX, int MouseY) {
        List<Component> Lines = new ArrayList<>();
        CollectorStatus Status = menu.getStatus();
        if (isHovering(PowerX - 1, PowerY - 1, PowerWidth + 2, PowerHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.power", menu.getReceived(), menu.getDemand()));
            Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.power_idle", CollectorTuning.IdleDraw).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.power_work", CollectorTuning.drawFor(FragmentRarity.Faint, false),
                    CollectorTuning.drawFor(FragmentRarity.Abyssal, false)).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(RadarX, RadarY, RadarSize, RadarSize, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.radar", menu.getRange()));
            Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.radar_hint").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LcdX, LcdY, LcdWidth, LcdHeight + 8, MouseX, MouseY)) {
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            Lines.add(Component.translatable(Status.getHintKey()).withStyle(ChatFormatting.GRAY));
            if (Status.hasTarget()) {
                Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.target", rarityName(), String.format(Locale.ROOT, "%.1f", menu.getDistance())));
                Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.progress", Math.round(menu.getProgress() * 100.0f)).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(LensX, LensY, LensSize, LensStep + LensSize, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.lenses", menu.getRangeUpgrades(), UpgradeType.Range.getLimit()));
            Lines.add(Component.translatable("gui.scp_unleashed.rift_collector.lenses_hint", CollectorTuning.RangePerUpgrade).withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }
}
