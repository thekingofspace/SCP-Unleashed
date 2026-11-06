package net.rai.scp_unleashed.instability.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.instability.InstabilityMenu;
import net.rai.scp_unleashed.instability.InstabilityReactorBlockEntity;
import net.rai.scp_unleashed.instability.ReactorFuel;
import net.rai.scp_unleashed.instability.ReactorStatus;
import net.rai.scp_unleashed.instability.ReactorTime;

import java.util.ArrayList;
import java.util.List;

public class InstabilityScreen extends AbstractContainerScreen<InstabilityMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/instability/reactor.png");
    public static final ResourceLocation CoreTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/instability/core.png");
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 0;
    public static final int BurnX = 13;
    public static final int BurnY = 51;
    public static final int BurnWidth = 32;
    public static final int BurnHeight = 5;
    public static final int BurnU = 194;
    public static final int BurnV = 0;
    public static final int ClickX = BurnX - 1;
    public static final int ClickY = BurnY - 1;
    public static final int ClickWidth = BurnWidth + 2;
    public static final int ClickHeight = BurnHeight + 2;
    public static final int LampX = 139;
    public static final int LampY = 62;
    public static final int LampSize = 5;
    public static final int LampU = 194;
    public static final int LampV = 6;
    public static final int CoreX = 51;
    public static final int CoreY = 21;
    public static final int CoreSize = 44;
    public static final int CoreColumns = 5;
    public static final int CoreFrames = 24;
    public static final int CoreSheet = 256;
    public static final int HotRow = 5;
    public static final int WindowX = 49;
    public static final int WindowY = 19;
    public static final int WindowSize = 48;
    public static final int DialX = 102;
    public static final int DialY = 18;
    public static final int DialWidth = 42;
    public static final int DialHeight = 25;
    public static final int PivotX = 123;
    public static final int PivotY = 39;
    public static final int NeedleLength = 15;
    public static final int LcdX = 102;
    public static final int LcdY = 47;
    public static final int LcdWidth = 42;
    public static final int LcdHeight = 12;
    public static final int LcdText = 0xFF8CFFA0;
    public static final int LcdDim = 0xFF2E6B3A;
    public static final int NeedleColour = 0xFFB02818;
    public static final int HubColour = 0xFF3A3A3A;
    public static final int SlotX = 20;
    public static final int SlotY = 26;

    private float Stability = 1.0f;
    private float PreviousStability = 1.0f;
    private int Ticks;

    public InstabilityScreen(InstabilityMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        PreviousStability = Stability;
        float Target = menu.getStatus() == ReactorStatus.Off ? 1.0f : menu.getStability();
        Stability += (Target - Stability) * 0.2f;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        ReactorStatus Status = menu.getStatus();
        ReactorFuel Burning = menu.getBurning();
        core(Graphics, Status, Burning, PartialTick);
        int Burn = Math.round(menu.getBurnFraction() * BurnWidth);
        if (Burn > 0 && Burning != null) {
            tint(Graphics, Burning.getColour(), 1.0f);
            Graphics.blit(Texture, leftPos + BurnX, topPos + BurnY, BurnU, BurnV, Burn, BurnHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        int Filled = Math.round((float) menu.getStored() * BarHeight / InstabilityReactorBlockEntity.Capacity);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Filled, BarU, BarV + BarHeight - Filled, BarWidth, Math.min(Filled, BarHeight));
        }
        int Lamp = switch (Status) {
            case Running -> 0;
            case Idle -> 1;
            default -> -1;
        };
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * (LampSize + 1), LampV, LampSize, LampSize);
        }
        needle(Graphics, Mth.lerp(PartialTick, PreviousStability, Stability));
    }

    private void core(GuiGraphics Graphics, ReactorStatus Status, ReactorFuel Burning, float PartialTick) {
        if (Status == ReactorStatus.Off) {
            return;
        }
        ReactorFuel Queued = ReactorFuel.of(menu.getSlot(0).getItem());
        int Colour = Burning != null ? Burning.getColour() : Queued != null ? Queued.getColour() : FragmentRarity.Faint.getColour() & 0xFFFFFF;
        float Strength = Status == ReactorStatus.Running ? 1.0f : 0.55f;
        int Frame = (int) ((Ticks + PartialTick) / 2.0f) % CoreFrames;
        RenderSystem.enableBlend();
        tint(Graphics, Colour, Strength);
        coreFrame(Graphics, Frame, 0);
        Graphics.setColor(1.0f, 1.0f, 1.0f, Strength * 0.85f);
        coreFrame(Graphics, Frame, HotRow);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void coreFrame(GuiGraphics Graphics, int Frame, int RowOffset) {
        int Column = Frame % CoreColumns;
        int Row = Frame / CoreColumns + RowOffset;
        Graphics.blit(CoreTexture, leftPos + CoreX, topPos + CoreY, Column * CoreSize, Row * CoreSize, CoreSize, CoreSize, CoreSheet, CoreSheet * 2);
    }

    private static void tint(GuiGraphics Graphics, int Rgb, float Alpha) {
        Graphics.setColor(((Rgb >> 16) & 255) / 255.0f, ((Rgb >> 8) & 255) / 255.0f, (Rgb & 255) / 255.0f, Alpha);
    }

    private void needle(GuiGraphics Graphics, float Shown) {
        double Angle = Math.PI * (1.0 - Mth.clamp(Shown, 0.0f, 1.0f));
        double DirectionX = Math.cos(Angle);
        double DirectionY = -Math.sin(Angle);
        for (int Step = 2; Step <= NeedleLength; Step++) {
            int X = leftPos + PivotX + (int) Math.round(DirectionX * Step);
            int Y = topPos + PivotY + (int) Math.round(DirectionY * Step);
            Graphics.fill(X, Y, X + 1, Y + 1, NeedleColour);
        }
        Graphics.fill(leftPos + PivotX - 1, topPos + PivotY - 1, leftPos + PivotX + 2, topPos + PivotY + 2, HubColour);
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        super.renderLabels(Graphics, MouseX, MouseY);
        ReactorStatus Status = menu.getStatus();
        String Reading = Status == ReactorStatus.Off ? "----" : String.format("%,d", menu.getOutput());
        int Colour = Status == ReactorStatus.Running ? LcdText : LcdDim;
        Graphics.drawString(font, Reading, LcdX + LcdWidth - 2 - font.width(Reading), LcdY + 2, Colour, false);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        List<Component> Lines = new ArrayList<>();
        boolean OverClickArea = false;
        ReactorStatus Status = menu.getStatus();
        ReactorFuel Burning = menu.getBurning();
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.stored", String.format("%,d", menu.getStored()),
                    String.format("%,d", InstabilityReactorBlockEntity.Capacity)));
        } else if (isHovering(ClickX, ClickY, ClickWidth, ClickHeight, MouseX, MouseY)) {
            burnLines(Lines, Burning);
            OverClickArea = true;
        } else if (isHovering(WindowX, WindowY, WindowSize, WindowSize, MouseX, MouseY)) {
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            burnLines(Lines, Burning);
            int Queued = menu.getSlot(0).getItem().getCount();
            if (Queued > 0) {
                Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.queued", Queued).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(DialX, DialY, DialWidth, DialHeight, MouseX, MouseY)) {
            if (Status == ReactorStatus.Off) {
                Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.stability_off").withStyle(ChatFormatting.GRAY));
            } else {
                int Percent = Math.round(menu.getStability() * 100.0f);
                ChatFormatting Colour = Percent >= 90 ? ChatFormatting.GREEN : Percent >= 70 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
                Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.stability", Percent).withStyle(Colour));
            }
            Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.stability_hint").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LcdX, LcdY, LcdWidth, LcdHeight + 6, MouseX, MouseY)) {
            Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.output", String.format("%,d", menu.getOutput())));
            if (Burning != null) {
                Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.rated", String.format("%,d", Burning.getOutput())).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(SlotX, SlotY, 18, 18, MouseX, MouseY) && menu.getSlot(0).getItem().isEmpty()) {
            Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.fuel_hint").withStyle(ChatFormatting.GRAY));
        }
        if (Lines.isEmpty()) {
            renderTooltip(Graphics, MouseX, MouseY);
        } else {
            Graphics.renderComponentTooltip(font, RecipeHint.with(Lines, OverClickArea), MouseX, MouseY);
        }
    }

    private void burnLines(List<Component> Lines, ReactorFuel Burning) {
        if (Burning == null) {
            Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack Shown = new ItemStack(BuiltInRegistries.ITEM.get(Burning.getItemId()));
        Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.burning", Shown.getHoverName())
                .withStyle(Style.EMPTY.withColor(Burning.getColour())));
        Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.time_left", ReactorTime.clock(menu.getBurnSeconds()), ReactorTime.clock(menu.getTotalSeconds())));
        Lines.add(Component.translatable("container.scp_unleashed.instability_reactor.rated", String.format("%,d", Burning.getOutput())).withStyle(ChatFormatting.GRAY));
    }
}
