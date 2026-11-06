package net.rai.scp_unleashed.props.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
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
import net.rai.scp_unleashed.props.DecorationCategory;
import net.rai.scp_unleashed.props.DecorationRecipe;
import net.rai.scp_unleashed.props.DecorationWorkbenchMenu;
import net.rai.scp_unleashed.props.PropsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DecorationWorkbenchScreen extends AbstractContainerScreen<DecorationWorkbenchMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/decorbench/decoration_workbench.png");
    public static final int GuiWidth = 176;
    public static final int GuiHeight = 210;
    public static final int GridX = 41;
    public static final int GridY = 37;
    public static final int Columns = 6;
    public static final int Rows = 4;
    public static final int Cell = 18;
    public static final int CellU = 176;
    public static final int NormalV = 0;
    public static final int HoveredV = 18;
    public static final int SelectedV = 36;
    public static final int ShortV = 54;
    public static final int TabX = 41;
    public static final int TabY = 16;
    public static final int TabSize = 18;
    public static final int TabNormalU = 194;
    public static final int TabHoveredU = 212;
    public static final int TabSelectedU = 230;
    public static final int TabV = 18;
    public static final int TrackX = 153;
    public static final int TrackY = 37;
    public static final int TrackHeight = 72;
    public static final int ScrollerWidth = 12;
    public static final int ScrollerHeight = 15;
    public static final int ScrollerU = 194;
    public static final int IdleScrollerU = 206;
    public static final int ScrollerV = 0;
    public static final int GhostV = 72;
    public static final int ConcreteGhostU = 176;
    public static final int MaterialGhostU = 192;
    public static final int DyeGhostU = 208;
    public static final int GlowGhostU = 224;
    public static final int PaneGhostU = 240;
    public static final int CountX = 21;
    public static final int MaterialCountY = 35;
    public static final int ExtraCountY = 63;
    public static final int ArrowX = 16;
    public static final int ArrowY = 73;
    public static final int ArrowWidth = 8;
    public static final int ArrowHeight = 9;
    public static final long GhostPeriod = 1500L;
    private static final List<DecorationCategory> TabCategories = List.of(DecorationCategory.Building, DecorationCategory.Lighting, DecorationCategory.Structures,
            DecorationCategory.Props, DecorationCategory.Decals);
    private static final int[] MaterialGhosts = {ConcreteGhostU, MaterialGhostU};
    private static final int[] ExtraGhosts = {DyeGhostU, GlowGhostU, PaneGhostU};
    private static final int TextColour = 0xFF404040;
    private static final int ShortColour = 0xFFB02E26;
    private static final int DimColour = 0x70402828;
    private static final int GhostColour = 0xA08B8B8B;
    private static int LastTab;

    private final List<Integer> Visible = new ArrayList<>();
    private int Tab;
    private float ScrollOffset;
    private boolean Scrolling;

    public DecorationWorkbenchScreen(DecorationWorkbenchMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = GuiWidth;
        imageHeight = GuiHeight;
        inventoryLabelY = imageHeight - 94;
        Tab = LastTab;
        refreshVisible();
    }

    private static int tabCount() {
        return TabCategories.size() + 1;
    }

    private boolean inTab(DecorationRecipe Plan, int Index) {
        return Index == 0 || Plan.Category() == TabCategories.get(Index - 1);
    }

    private int countIn(int Index) {
        int Found = 0;
        for (RecipeHolder<DecorationRecipe> Holder : menu.getRecipes()) {
            if (inTab(Holder.value(), Index)) {
                Found++;
            }
        }
        return Found;
    }

    private void refreshVisible() {
        Visible.clear();
        List<RecipeHolder<DecorationRecipe>> Recipes = menu.getRecipes();
        for (int Index = 0; Index < Recipes.size(); Index++) {
            if (inTab(Recipes.get(Index).value(), Tab)) {
                Visible.add(Index);
            }
        }
    }

    private ItemStack tabIcon(int Index) {
        return Index == 0 ? new ItemStack(PropsRegistry.DecorationWorkbench.get()) : TabCategories.get(Index - 1).icon();
    }

    private Component tabTitle(int Index) {
        return Index == 0 ? Component.translatable("gui.scp_unleashed.decoration_workbench.tab.all") : TabCategories.get(Index - 1).title();
    }

    private int totalRows() {
        return (Visible.size() + Columns - 1) / Columns;
    }

    private int hiddenRows() {
        return Math.max(0, totalRows() - Rows);
    }

    private boolean canScroll() {
        return hiddenRows() > 0;
    }

    private int startRow() {
        return Math.round(ScrollOffset * hiddenRows());
    }

    private static boolean within(double MouseX, double MouseY, int X, int Y, int Width, int Height) {
        return MouseX >= X && MouseX < X + Width && MouseY >= Y && MouseY < Y + Height;
    }

    private int hoveredTab(double MouseX, double MouseY) {
        for (int Index = 0; Index < tabCount(); Index++) {
            if (within(MouseX, MouseY, leftPos + TabX + Index * TabSize, topPos + TabY, TabSize, TabSize)) {
                return Index;
            }
        }
        return -1;
    }

    private int hoveredPlace(double MouseX, double MouseY) {
        int Start = startRow() * Columns;
        for (int Place = 0; Place < Columns * Rows; Place++) {
            if (Start + Place >= Visible.size()) {
                break;
            }
            if (within(MouseX, MouseY, leftPos + GridX + (Place % Columns) * Cell, topPos + GridY + (Place / Columns) * Cell, Cell, Cell)) {
                return Start + Place;
            }
        }
        return -1;
    }

    private int hoveredRecipe(double MouseX, double MouseY) {
        int Place = hoveredPlace(MouseX, MouseY);
        return Place >= 0 ? Visible.get(Place) : -1;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            renderTooltip(Graphics, MouseX, MouseY);
            return;
        }
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        List<Component> Lines = hoverLines(MouseX, MouseY);
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> hoverLines(int MouseX, int MouseY) {
        int HoveredTab = hoveredTab(MouseX, MouseY);
        if (HoveredTab >= 0) {
            return List.of(tabTitle(HoveredTab), Component.translatable("gui.scp_unleashed.decoration_workbench.tab.count", countIn(HoveredTab)).withStyle(ChatFormatting.GRAY));
        }
        int Hovered = hoveredRecipe(MouseX, MouseY);
        if (Hovered >= 0) {
            return recipeLines(menu.getRecipes().get(Hovered).value());
        }
        RecipeHolder<DecorationRecipe> Selected = menu.selectedRecipe();
        if (within(MouseX, MouseY, leftPos + DecorationWorkbenchMenu.MaterialX - 1, topPos + DecorationWorkbenchMenu.MaterialY - 1, Cell, Cell)) {
            return Selected == null ? hintLines("material_hint") : needsLines(Selected.value().Material());
        }
        if (within(MouseX, MouseY, leftPos + DecorationWorkbenchMenu.ExtraX - 1, topPos + DecorationWorkbenchMenu.ExtraY - 1, Cell, Cell)) {
            return Selected == null ? hintLines("extra_hint") : Selected.value().Extra().map(this::needsLines).orElseGet(() -> hintLines("extra_none"));
        }
        return List.of();
    }

    private List<Component> needsLines(SizedIngredient Needed) {
        return List.of(Component.translatable("gui.scp_unleashed.decoration_workbench.needs", Needed.count(), nameOf(Needed)).withStyle(ChatFormatting.RED));
    }

    private List<Component> hintLines(String Hint) {
        return List.of(Component.translatable("gui.scp_unleashed.decoration_workbench." + Hint).withStyle(ChatFormatting.GRAY));
    }

    private static Component nameOf(SizedIngredient Needed) {
        ItemStack[] Choices = Needed.ingredient().getItems();
        if (Choices.length == 0) {
            return Component.translatable("gui.scp_unleashed.decoration_workbench.unknown");
        }
        return Choices[0].getHoverName();
    }

    private List<Component> recipeLines(DecorationRecipe Plan) {
        ItemStack Result = Plan.Result();
        List<Component> Lines = new ArrayList<>(getTooltipFromContainerItem(Result));
        Lines.add(Component.translatable("gui.scp_unleashed.decoration_workbench.cost", Plan.Material().count(), nameOf(Plan.Material())).withStyle(ChatFormatting.GRAY));
        Plan.Extra().ifPresent(Needed -> Lines.add(Component.translatable("gui.scp_unleashed.decoration_workbench.extra", Needed.count(), nameOf(Needed)).withStyle(ChatFormatting.GRAY)));
        if (Result.getCount() > 1) {
            Lines.add(Component.translatable("gui.scp_unleashed.decoration_workbench.makes", Result.getCount()).withStyle(ChatFormatting.GRAY));
        }
        int MaterialShort = Plan.materialShort(menu.getMaterial());
        if (MaterialShort > 0) {
            Lines.add(Component.translatable("gui.scp_unleashed.decoration_workbench.short", MaterialShort, nameOf(Plan.Material())).withStyle(ChatFormatting.RED));
        }
        int ExtraShort = Plan.extraShort(menu.getExtra());
        if (ExtraShort > 0) {
            Plan.Extra().ifPresent(Needed -> Lines.add(Component.translatable("gui.scp_unleashed.decoration_workbench.short", ExtraShort, nameOf(Needed)).withStyle(ChatFormatting.RED)));
        }
        return Lines;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderTabs(Graphics, MouseX, MouseY);
        int ScrollerTop = topPos + TrackY + Math.round((TrackHeight - ScrollerHeight) * ScrollOffset);
        Graphics.blit(Texture, leftPos + TrackX, ScrollerTop, canScroll() ? ScrollerU : IdleScrollerU, ScrollerV, ScrollerWidth, ScrollerHeight);
        renderCells(Graphics, MouseX, MouseY);
        RecipeHolder<DecorationRecipe> Selected = menu.selectedRecipe();
        long Phase = Util.getMillis() / GhostPeriod;
        if (Selected == null) {
            renderGhostSprite(Graphics, DecorationWorkbenchMenu.MaterialSlot, DecorationWorkbenchMenu.MaterialX, DecorationWorkbenchMenu.MaterialY, MaterialGhosts[(int) (Phase % MaterialGhosts.length)]);
            renderGhostSprite(Graphics, DecorationWorkbenchMenu.ExtraSlot, DecorationWorkbenchMenu.ExtraX, DecorationWorkbenchMenu.ExtraY, ExtraGhosts[(int) (Phase % ExtraGhosts.length)]);
            return;
        }
        renderGhostItem(Graphics, DecorationWorkbenchMenu.MaterialSlot, DecorationWorkbenchMenu.MaterialX, DecorationWorkbenchMenu.MaterialY, Optional.of(Selected.value().Material()), Phase);
        renderGhostItem(Graphics, DecorationWorkbenchMenu.ExtraSlot, DecorationWorkbenchMenu.ExtraX, DecorationWorkbenchMenu.ExtraY, Selected.value().Extra(), Phase);
    }

    private void renderTabs(GuiGraphics Graphics, int MouseX, int MouseY) {
        int Hovered = hoveredTab(MouseX, MouseY);
        for (int Index = 0; Index < tabCount(); Index++) {
            int X = leftPos + TabX + Index * TabSize;
            int Y = topPos + TabY;
            int U = Index == Tab ? TabSelectedU : Index == Hovered ? TabHoveredU : TabNormalU;
            Graphics.blit(Texture, X, Y, U, TabV, TabSize, TabSize);
            Graphics.renderItem(tabIcon(Index), X + 1, Y + 1);
        }
    }

    private void renderGhostSprite(GuiGraphics Graphics, int SlotIndex, int X, int Y, int U) {
        if (!menu.getSlot(SlotIndex).hasItem()) {
            Graphics.blit(Texture, leftPos + X, topPos + Y, U, GhostV, 16, 16);
        }
    }

    private void renderGhostItem(GuiGraphics Graphics, int SlotIndex, int X, int Y, Optional<SizedIngredient> Needed, long Phase) {
        if (menu.getSlot(SlotIndex).hasItem() || Needed.isEmpty()) {
            return;
        }
        ItemStack[] Choices = Needed.get().getItems();
        if (Choices.length == 0) {
            return;
        }
        ItemStack Shown = Choices[(int) (Phase % Choices.length)];
        int Left = leftPos + X;
        int Top = topPos + Y;
        Graphics.renderFakeItem(Shown, Left, Top);
        Graphics.fill(RenderType.guiOverlay(), Left, Top, Left + 16, Top + 16, GhostColour);
        Graphics.flush();
        Graphics.renderItemDecorations(font, Shown, Left, Top);
    }

    private void renderCells(GuiGraphics Graphics, int MouseX, int MouseY) {
        List<RecipeHolder<DecorationRecipe>> Recipes = menu.getRecipes();
        int Start = startRow() * Columns;
        int Hovered = hoveredRecipe(MouseX, MouseY);
        for (int Place = 0; Place < Columns * Rows; Place++) {
            if (Start + Place >= Visible.size()) {
                break;
            }
            int Index = Visible.get(Start + Place);
            DecorationRecipe Plan = Recipes.get(Index).value();
            boolean Affordable = menu.affordable(Plan);
            int X = leftPos + GridX + (Place % Columns) * Cell;
            int Y = topPos + GridY + (Place / Columns) * Cell;
            int V = Index == menu.getSelected() ? SelectedV : Index == Hovered ? HoveredV : Affordable ? NormalV : ShortV;
            Graphics.blit(Texture, X, Y, CellU, V, Cell, Cell);
            Graphics.renderItem(Plan.Result(), X + 1, Y + 1);
            Graphics.renderItemDecorations(font, Plan.Result(), X + 1, Y + 1);
            if (!Affordable && Index != menu.getSelected()) {
                Graphics.fill(RenderType.guiOverlay(), X + 1, Y + 1, X + Cell - 1, Y + Cell - 1, DimColour);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        super.renderLabels(Graphics, MouseX, MouseY);
        RecipeHolder<DecorationRecipe> Selected = menu.selectedRecipe();
        if (Selected != null) {
            DecorationRecipe Plan = Selected.value();
            drawCount(Graphics, Plan.Material().count(), MaterialCountY, Plan.materialShort(menu.getMaterial()) == 0);
            Plan.Extra().ifPresent(Needed -> drawCount(Graphics, Needed.count(), ExtraCountY, Plan.extraShort(menu.getExtra()) == 0));
        }
        if (Visible.isEmpty()) {
            Component Empty = Component.translatable("gui.scp_unleashed.decoration_workbench.empty");
            Graphics.drawString(font, Empty, GridX + (Columns * Cell - font.width(Empty)) / 2, GridY + Rows * Cell / 2 - 4, TextColour, false);
        }
    }

    private void drawCount(GuiGraphics Graphics, int Count, int Y, boolean Enough) {
        Component Text = Component.translatable("gui.scp_unleashed.decoration_workbench.count", Count);
        Graphics.drawString(font, Text, CountX - font.width(Text) / 2, Y, Enough ? TextColour : ShortColour, false);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        Scrolling = false;
        int ClickedTab = hoveredTab(MouseX, MouseY);
        if (ClickedTab >= 0 && minecraft != null) {
            if (ClickedTab != Tab) {
                Tab = ClickedTab;
                LastTab = ClickedTab;
                ScrollOffset = 0.0f;
                refreshVisible();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
            return true;
        }
        int Hovered = hoveredRecipe(MouseX, MouseY);
        if (Hovered >= 0 && minecraft != null && minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, Hovered)) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Hovered);
            return true;
        }
        if (canScroll() && within(MouseX, MouseY, leftPos + TrackX, topPos + TrackY, ScrollerWidth, TrackHeight)) {
            Scrolling = true;
            dragTo(MouseY);
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseDragged(double MouseX, double MouseY, int Button, double DragX, double DragY) {
        if (Scrolling && canScroll()) {
            dragTo(MouseY);
            return true;
        }
        return super.mouseDragged(MouseX, MouseY, Button, DragX, DragY);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        Scrolling = false;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double DeltaX, double DeltaY) {
        if (canScroll()) {
            ScrollOffset = Mth.clamp(ScrollOffset - (float) DeltaY / hiddenRows(), 0.0f, 1.0f);
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, DeltaX, DeltaY);
    }

    private void dragTo(double MouseY) {
        float Travel = TrackHeight - ScrollerHeight;
        ScrollOffset = Mth.clamp((float) (MouseY - topPos - TrackY - ScrollerHeight / 2.0) / Travel, 0.0f, 1.0f);
    }
}
