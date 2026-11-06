package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunBenchMenu;
import net.rai.scp_unleashed.guns.GunCraftingPlan;
import net.rai.scp_unleashed.guns.GunCraftingRecipe;
import net.rai.scp_unleashed.guns.GunCraftingRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GunBenchScreen extends AbstractContainerScreen<GunBenchMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/guns/gun_bench.png");
    public static final int TextureWidth = 512;
    public static final int TextureHeight = 256;
    public static final int ImageWidth = 276;
    public static final int ImageHeight = 166;
    public static final int ListX = 6;
    public static final int ListY = 19;
    public static final int EntryWidth = 85;
    public static final int EntryHeight = 22;
    public static final int EntryPitch = 23;
    public static final int VisibleEntries = 6;
    public static final int EntryU = 0;
    public static final int EntryV = 166;
    public static final int IconX = 3;
    public static final int IconY = 3;
    public static final int NameX = 21;
    public static final int NameY = 7;
    public static final int NameWidth = 63;
    public static final boolean NameShadow = true;
    public static final int ScrollX = 96;
    public static final int ScrollY = 18;
    public static final int ScrollHeight = 139;
    public static final int ScrollerWidth = 6;
    public static final int ScrollerHeight = 27;
    public static final int ScrollerU = 352;
    public static final int ScrollerV = 166;
    public static final int PartsX = 110;
    public static final int PartsY = 20;
    public static final int PartsColumns = 3;
    public static final int PartPitch = 18;
    public static final int ArrowX = 168;
    public static final int ArrowY = 29;
    public static final int ArrowWidth = 24;
    public static final int ArrowHeight = 16;
    public static final int ArrowU = 276;
    public static final int ArrowV = 42;
    public static final int MissingU = 300;
    public static final int MissingV = 42;
    public static final int ButtonX = 168;
    public static final int ButtonY = 55;
    public static final int ButtonWidth = 52;
    public static final int ButtonHeight = 14;
    public static final int ButtonU = 276;
    public static final int ButtonV = 0;
    public static final int TitleX = 8;
    public static final int TitleY = 6;
    public static final int InventoryLabelX = 108;
    public static final int InventoryLabelY = 72;
    public static final int GhostOverlay = 0x308B8B8B;
    public static final int NameColour = 0xFFF0ECE0;
    public static final int DimColour = 0xFFA9A59D;
    public static final int ButtonColour = 0xFFF4EFE0;
    public static final int ButtonDimColour = 0xFF8F8C86;
    public static final double MarqueeMinimumSeconds = 3.0;
    public static final double MarqueeSecondsPerPixel = 0.5;
    public static final String Ellipsis = "...";
    private static final String Prefix = "gui.scp_unleashed.gun_bench.";

    private List<RecipeHolder<GunCraftingRecipe>> Recipes = List.of();
    private final List<Boolean> Affordable = new ArrayList<>();
    private int Selected;
    private int Scroll;
    private boolean Dragging;

    public GunBenchScreen(GunBenchMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = ImageWidth;
        imageHeight = ImageHeight;
        titleLabelX = TitleX;
        titleLabelY = TitleY;
        inventoryLabelX = InventoryLabelX;
        inventoryLabelY = InventoryLabelY;
    }

    @Override
    protected void init() {
        super.init();
        refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    private void refresh() {
        Recipes = GunCraftingRecipes.all(Minecraft.getInstance().level);
        Affordable.clear();
        for (RecipeHolder<GunCraftingRecipe> Holder : Recipes) {
            Affordable.add(GunCraftingRecipes.canAfford(Holder.value(), menu.getUser()));
        }
        Selected = Mth.clamp(Selected, 0, Math.max(0, Recipes.size() - 1));
        Scroll = Mth.clamp(Scroll, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, Recipes.size() - VisibleEntries);
    }

    @Nullable
    private GunCraftingRecipe selectedRecipe() {
        return Recipes.isEmpty() ? null : Recipes.get(Selected).value();
    }

    private boolean canAssemble() {
        GunCraftingRecipe Formula = selectedRecipe();
        return Formula != null && Affordable.get(Selected) && menu.canAccept(Formula.getResult());
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, TextureWidth, TextureHeight);
        for (int Row = 0; Row < VisibleEntries; Row++) {
            int Index = Scroll + Row;
            if (Index >= Recipes.size()) {
                break;
            }
            int X = leftPos + ListX;
            int Y = topPos + ListY + Row * EntryPitch;
            boolean Hovered = MouseX >= X && MouseX < X + EntryWidth && MouseY >= Y && MouseY < Y + EntryHeight;
            int State = Index == Selected ? 2 : Hovered ? 1 : 0;
            Graphics.blit(Texture, X, Y, EntryU + State * EntryWidth, EntryV, EntryWidth, EntryHeight, TextureWidth, TextureHeight);
            ItemStack Result = Recipes.get(Index).value().getResult();
            Graphics.renderItem(Result, X + IconX, Y + IconY);
            name(Graphics, Result.getHoverName(), X + NameX, Y + NameY, Affordable.get(Index) ? NameColour : DimColour, State > 0);
        }
        boolean Scrolls = maxScroll() > 0;
        int Travel = ScrollHeight - ScrollerHeight;
        int Offset = Scrolls ? Math.round((float) Scroll / maxScroll() * Travel) : 0;
        Graphics.blit(Texture, leftPos + ScrollX, topPos + ScrollY + Offset, ScrollerU + (Scrolls ? 0 : ScrollerWidth), ScrollerV, ScrollerWidth, ScrollerHeight, TextureWidth, TextureHeight);
        GunCraftingRecipe Formula = selectedRecipe();
        if (Formula == null) {
            return;
        }
        List<ItemStack> Supplies = GunCraftingRecipes.supplies(menu.getUser());
        boolean Creative = menu.getUser().getAbilities().instabuild;
        List<SizedIngredient> Inputs = Formula.getInputs();
        for (int Index = 0; Index < Inputs.size(); Index++) {
            int X = leftPos + PartsX + Index % PartsColumns * PartPitch;
            int Y = topPos + PartsY + Index / PartsColumns * PartPitch;
            SizedIngredient Need = Inputs.get(Index);
            if (!Creative && GunCraftingPlan.available(Need, Supplies) < Need.count()) {
                Graphics.blit(Texture, X - 1, Y - 1, MissingU, MissingV, 18, 18, TextureWidth, TextureHeight);
            }
            ItemStack Shown = cycled(Need);
            Graphics.renderItem(Shown, X, Y);
            Graphics.renderItemDecorations(font, Shown, X, Y);
        }
        boolean Ready = canAssemble();
        if (Ready) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowU, ArrowV, ArrowWidth, ArrowHeight, TextureWidth, TextureHeight);
        }
        boolean OverButton = isHovering(ButtonX, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY);
        int ButtonState = !Ready ? 2 : OverButton ? 1 : 0;
        Graphics.blit(Texture, leftPos + ButtonX, topPos + ButtonY, ButtonU, ButtonV + ButtonState * ButtonHeight, ButtonWidth, ButtonHeight, TextureWidth, TextureHeight);
        Component Label = Component.translatable(Prefix + "assemble");
        Graphics.drawString(font, Label, leftPos + ButtonX + (ButtonWidth - font.width(Label)) / 2, topPos + ButtonY + 3, Ready ? ButtonColour : ButtonDimColour, Ready);
        if (menu.getOutput().isEmpty()) {
            int X = leftPos + GunBenchMenu.OutputX;
            int Y = topPos + GunBenchMenu.OutputY;
            Graphics.renderFakeItem(Formula.getResult(), X, Y);
            Graphics.fill(RenderType.guiGhostRecipeOverlay(), X, Y, X + 16, Y + 16, GhostOverlay);
        }
    }

    private void name(GuiGraphics Graphics, Component Text, int X, int Y, int Colour, boolean Active) {
        int Width = font.width(Text);
        if (Width <= NameWidth) {
            Graphics.drawString(font, Text, X, Y, Colour, NameShadow);
            return;
        }
        if (!Active) {
            String Shortened = font.plainSubstrByWidth(Text.getString(), NameWidth - font.width(Ellipsis)).stripTrailing() + Ellipsis;
            Graphics.drawString(font, Shortened, X, Y, Colour, NameShadow);
            return;
        }
        int Overflow = Width - NameWidth;
        double Period = Math.max(Overflow * MarqueeSecondsPerPixel, MarqueeMinimumSeconds);
        double Phase = Math.sin(Math.PI / 2.0 * Math.cos(Math.PI * 2.0 * (Util.getMillis() / 1000.0) / Period)) / 2.0 + 0.5;
        int Shift = (int) Math.round(Mth.lerp(Phase, Overflow, 0.0));
        Graphics.enableScissor(X, Y - 1, X + NameWidth, Y + font.lineHeight + 1);
        Graphics.drawString(font, Text, X - Shift, Y, Colour, NameShadow);
        Graphics.disableScissor();
    }

    private ItemStack cycled(SizedIngredient Need) {
        ItemStack[] Options = Need.getItems();
        if (Options.length == 0) {
            return ItemStack.EMPTY;
        }
        long Time = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        return Options[(int) (Time / 20L % Options.length)];
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        int Entry = entryAt(MouseX, MouseY);
        if (Entry >= 0) {
            List<Component> Lines = new ArrayList<>(getTooltipFromContainerItem(Recipes.get(Entry).value().getResult()));
            if (!Affordable.get(Entry)) {
                Lines.add(Component.translatable(Prefix + "missing").withStyle(ChatFormatting.RED));
            }
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
            return;
        }
        GunCraftingRecipe Formula = selectedRecipe();
        if (Formula == null) {
            return;
        }
        List<SizedIngredient> Inputs = Formula.getInputs();
        for (int Index = 0; Index < Inputs.size(); Index++) {
            int X = PartsX + Index % PartsColumns * PartPitch;
            int Y = PartsY + Index / PartsColumns * PartPitch;
            if (isHovering(X - 1, Y - 1, 18, 18, MouseX, MouseY)) {
                SizedIngredient Need = Inputs.get(Index);
                ItemStack Shown = cycled(Need);
                List<Component> Lines = new ArrayList<>(getTooltipFromContainerItem(Shown));
                int Have = GunCraftingPlan.available(Need, GunCraftingRecipes.supplies(menu.getUser()));
                Lines.add(Component.translatable(Prefix + "have", Have, Need.count()).withStyle(Have >= Need.count() ? ChatFormatting.GREEN : ChatFormatting.RED));
                Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
                return;
            }
        }
        if (isHovering(ButtonX, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            String Key = canAssemble() ? "ready" : !Affordable.get(Selected) ? "missing" : "full";
            Graphics.renderTooltip(font, Component.translatable(Prefix + Key), MouseX, MouseY);
        }
    }

    private int entryAt(double MouseX, double MouseY) {
        double X = MouseX - leftPos - ListX;
        double Y = MouseY - topPos - ListY;
        if (X < 0 || X >= EntryWidth || Y < 0 || Y >= VisibleEntries * EntryPitch) {
            return -1;
        }
        int Row = (int) (Y / EntryPitch);
        if (Y - Row * EntryPitch >= EntryHeight) {
            return -1;
        }
        int Index = Scroll + Row;
        return Index < Recipes.size() ? Index : -1;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            int Entry = entryAt(MouseX, MouseY);
            if (Entry >= 0) {
                Selected = Entry;
                click();
                return true;
            }
            if (isHovering(ButtonX, ButtonY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
                if (canAssemble() && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Selected);
                    click();
                }
                return true;
            }
            if (maxScroll() > 0 && isHovering(ScrollX, ScrollY, ScrollerWidth, ScrollHeight, MouseX, MouseY)) {
                Dragging = true;
                dragTo(MouseY);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseDragged(double MouseX, double MouseY, int Button, double DragX, double DragY) {
        if (Dragging) {
            dragTo(MouseY);
            return true;
        }
        return super.mouseDragged(MouseX, MouseY, Button, DragX, DragY);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        Dragging = false;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double AmountX, double AmountY) {
        if (maxScroll() > 0 && MouseX < leftPos + ImageWidth / 2.5) {
            Scroll = Mth.clamp(Scroll - (int) Math.signum(AmountY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, AmountX, AmountY);
    }

    private void dragTo(double MouseY) {
        float Position = (float) (MouseY - topPos - ScrollY - ScrollerHeight / 2.0) / (ScrollHeight - ScrollerHeight);
        Scroll = Mth.clamp(Math.round(Position * maxScroll()), 0, maxScroll());
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
