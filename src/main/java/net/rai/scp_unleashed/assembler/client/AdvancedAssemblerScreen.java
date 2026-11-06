package net.rai.scp_unleashed.assembler.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.assembler.AssemblerMenu;
import net.rai.scp_unleashed.assembler.AssemblerStatus;
import net.rai.scp_unleashed.assembler.AssemblingRecipe;
import net.rai.scp_unleashed.assembler.AssemblingRecipes;

import java.util.ArrayList;
import java.util.List;

public class AdvancedAssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/advanced_assembler.png");
    public static final int ArrowX = 89;
    public static final int ArrowY = 39;
    public static final int ArrowWidth = 30;
    public static final int ArrowHeight = 17;
    public static final int ArrowU = 176;
    public static final int ArrowV = 0;
    public static final int BarX = 153;
    public static final int BarY = 21;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 17;
    public static final int LampX = 11;
    public static final int LampY = 25;
    public static final int LampSpacing = 13;
    public static final int LampSize = 8;
    public static final int LampU = 208;
    public static final int LampV = 0;
    public static final int LcdX = 87;
    public static final int LcdY = 61;
    public static final int LcdWidth = 34;
    public static final int LcdHeight = 11;
    public static final int LcdColour = 0xFF8CFFB4;
    public static final int LcdDimColour = 0xFF3E8A5E;
    public static final int LcdWarnColour = 0xFFFFB347;
    public static final int GhostOverlay = 0x308B8B8B;
    public static final int ImageHeight = 176;
    public static final int InventoryLabelY = 83;
    private static final String Prefix = "gui.scp_unleashed.advanced_assembler.";

    private ItemStack Preview = ItemStack.EMPTY;

    public AdvancedAssemblerScreen(AssemblerMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = ImageHeight;
        inventoryLabelY = InventoryLabelY;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        RecipeHolder<AssemblingRecipe> Found = AssemblingRecipes.find(Minecraft.getInstance().level, menu.getInputs());
        Preview = Found == null ? ItemStack.EMPTY : Found.value().getResult();
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Filled = Math.round(menu.getProgress() * ArrowWidth);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowU, ArrowV, Filled, ArrowHeight);
        }
        int Power = Math.min(BarHeight, Math.round((float) menu.getReceived() * BarHeight / menu.getDraw()));
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
        AssemblerStatus Status = menu.getStatus();
        boolean Blink = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) / 8 % 2 == 0;
        lamp(Graphics, 0, Status != AssemblerStatus.Off);
        lamp(Graphics, 1, Status == AssemblerStatus.Working);
        lamp(Graphics, 2, Status == AssemblerStatus.Blocked && Blink);
        lcd(Graphics, Status);
        if (menu.getOutput().isEmpty() && !Preview.isEmpty()) {
            int X = leftPos + AssemblerMenu.OutputX;
            int Y = topPos + AssemblerMenu.OutputY;
            Graphics.renderFakeItem(Preview, X, Y);
            Graphics.fill(RenderType.guiGhostRecipeOverlay(), X, Y, X + 16, Y + 16, GhostOverlay);
        }
    }

    private void lamp(GuiGraphics Graphics, int Index, boolean On) {
        if (On) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY + Index * LampSpacing, LampU + Index * LampSize, LampV, LampSize, LampSize);
        }
    }

    private void lcd(GuiGraphics Graphics, AssemblerStatus Status) {
        Component Text;
        int Colour = LcdColour;
        if (Status == AssemblerStatus.Blocked) {
            Text = Component.translatable(Prefix + "lcd.full");
            Colour = LcdWarnColour;
        } else if (Status == AssemblerStatus.Off) {
            Text = Component.translatable(menu.getTotalTicks() > 0 ? Prefix + "lcd.no_power" : Prefix + "lcd.off");
            Colour = menu.getTotalTicks() > 0 ? LcdWarnColour : LcdDimColour;
        } else if (menu.getTotalTicks() > 0) {
            Text = Component.literal(Math.round(menu.getProgress() * 100.0f) + "%");
        } else {
            Text = Component.translatable(Prefix + "lcd.ready");
        }
        int Width = font.width(Text);
        Graphics.drawString(font, Text, leftPos + LcdX + (LcdWidth - Width + 1) / 2, topPos + LcdY + 2, Colour, false);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        if (isHovering(BarX, BarY, BarWidth, BarHeight, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, powerLines(), MouseX, MouseY);
        } else if (isHovering(ArrowX, ArrowY, ArrowWidth, ArrowHeight, MouseX, MouseY) || isHovering(LcdX, LcdY, LcdWidth, LcdHeight, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, RecipeHint.with(progressLines(), isHovering(ArrowX, ArrowY, ArrowWidth, ArrowHeight, MouseX, MouseY)), MouseX, MouseY);
        } else if (isHovering(LampX, LampY, LampSize, LampSpacing * 2 + LampSize, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, List.of(statusLine(menu.getStatus())), MouseX, MouseY);
        }
    }

    private List<Component> powerLines() {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable(Prefix + "power", menu.getReceived(), menu.getDemand()));
        Lines.add(Component.translatable(Prefix + "draw_active", menu.getDraw()).withStyle(ChatFormatting.GRAY));
        return Lines;
    }

    private List<Component> progressLines() {
        List<Component> Lines = new ArrayList<>();
        int Total = menu.getTotalTicks();
        if (Total <= 0) {
            Lines.add(Component.translatable(Prefix + "no_recipe").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        Lines.add(Component.translatable(Prefix + "progress", Math.round(menu.getProgress() * 100.0f)));
        float Rate = Math.min(1.0f, (float) menu.getReceived() / menu.getDraw());
        if (menu.getStatus() == AssemblerStatus.Working && Rate > 0.0f) {
            float Seconds = (1.0f - menu.getProgress()) * Total / Rate / 20.0f;
            Lines.add(Component.translatable(Prefix + "remaining", String.format("%.1f", Seconds)).withStyle(ChatFormatting.GRAY));
            if (Rate < 0.99f) {
                Lines.add(Component.translatable(Prefix + "slowed", Math.round(Rate * 100.0f)).withStyle(ChatFormatting.GOLD));
            }
        } else {
            Lines.add(statusLine(menu.getStatus()));
        }
        return Lines;
    }

    private Component statusLine(AssemblerStatus Status) {
        ChatFormatting Colour = switch (Status) {
            case Working -> ChatFormatting.GOLD;
            case Idle -> ChatFormatting.GREEN;
            case Blocked -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_GRAY;
        };
        String Key = Status == AssemblerStatus.Off && menu.getTotalTicks() > 0 ? "no_power" : Status.getSerializedName();
        return Component.translatable(Prefix + "status." + Key).withStyle(Colour);
    }
}
