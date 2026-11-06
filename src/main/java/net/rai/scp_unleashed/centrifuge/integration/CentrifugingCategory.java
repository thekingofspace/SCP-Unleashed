package net.rai.scp_unleashed.centrifuge.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.rai.scp_unleashed.centrifuge.CentrifugeBlockEntity;
import net.rai.scp_unleashed.centrifuge.CentrifugeOutput;
import net.rai.scp_unleashed.centrifuge.CentrifugeRecipe;
import net.rai.scp_unleashed.centrifuge.CentrifugeRegistry;
import net.rai.scp_unleashed.centrifuge.client.CentrifugeScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CentrifugingCategory implements IRecipeCategory<RecipeHolder<CentrifugeRecipe>> {
    public static final RecipeType<RecipeHolder<CentrifugeRecipe>> Type = RecipeType.createRecipeHolderType(CentrifugeRegistry.CentrifugingId);
    public static final int Width = 170;
    public static final int Height = 56;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 168;
    public static final int InfoY = 1;
    public static final int InputX = 4;
    public static final int SlotY = 19;
    public static final int WindowX = 30;
    public static final int WindowY = 16;
    public static final int ArrowX = 61;
    public static final int ArrowY = 21;
    public static final int FirstOutputX = 81;
    public static final int OutputSpacing = 22;
    public static final int LabelY = 39;
    public static final long FrameMillis = 50L;
    private static final int TextColour = 0xFF404040;
    private static final int ChanceColour = 0xFFB0781A;

    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final Map<Integer, IDrawableAnimated> Arrows = new HashMap<>();

    public CentrifugingCategory(IGuiHelper Helper) {
        this.Helper = Helper;
        Icon = Helper.createDrawableItemLike(CentrifugeRegistry.CentrifugeItem.get());
        Background = Helper.createDrawable(CentrifugeScreen.Texture, BackgroundU, BackgroundV, Width, Height);
    }

    @Override
    public RecipeType<RecipeHolder<CentrifugeRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.centrifuging");
    }

    @Override
    public int getWidth() {
        return Width;
    }

    @Override
    public int getHeight() {
        return Height;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<CentrifugeRecipe> Entry, IFocusGroup Focuses) {
        CentrifugeRecipe Separation = Entry.value();
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX + 1, SlotY + 1).addItemStacks(List.of(Separation.getInput().getItems()));
        List<CentrifugeOutput> Results = Separation.getResults();
        for (int Index = 0; Index < Results.size(); Index++) {
            CentrifugeOutput Output = Results.get(Index);
            IRecipeSlotBuilder Placed = Builder.addSlot(RecipeIngredientRole.OUTPUT, FirstOutputX + Index * OutputSpacing + 1, SlotY + 1).addItemStack(Output.Stack());
            if (!Output.isGuaranteed()) {
                Placed.addRichTooltipCallback((View, Lines) -> Lines.add(Component.translatable("jei.scp_unleashed.centrifuging.chance", Output.percent()).withStyle(ChatFormatting.GOLD)));
            }
        }
    }

    @Override
    public void draw(RecipeHolder<CentrifugeRecipe> Entry, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        CentrifugeRecipe Separation = Entry.value();
        Font Text = Minecraft.getInstance().font;
        Background.draw(Graphics);
        Graphics.blit(CentrifugeScreen.Texture, WindowX, WindowY, CentrifugeScreen.GlowU, CentrifugeScreen.GlowV, CentrifugeScreen.RotorSize, CentrifugeScreen.RotorSize);
        int Frame = (int) ((Util.getMillis() / FrameMillis) % CentrifugeScreen.RotorFrames);
        Graphics.blit(CentrifugeScreen.RotorTexture, WindowX, WindowY, (Frame % CentrifugeScreen.RotorColumns) * CentrifugeScreen.RotorSize,
                (Frame / CentrifugeScreen.RotorColumns) * CentrifugeScreen.RotorSize, CentrifugeScreen.RotorSize, CentrifugeScreen.RotorSize,
                CentrifugeScreen.RotorSheetWidth, CentrifugeScreen.RotorSheetHeight);
        Arrows.computeIfAbsent(Separation.getTime(), Ticks -> Helper.drawableBuilder(CentrifugeScreen.Texture, CentrifugeScreen.ArrowU, CentrifugeScreen.ArrowV,
                CentrifugeScreen.ArrowWidth, CentrifugeScreen.ArrowHeight).buildAnimated(Ticks, IDrawableAnimated.StartDirection.LEFT, false)).draw(Graphics, ArrowX, ArrowY);
        List<CentrifugeOutput> Results = Separation.getResults();
        for (int Index = 0; Index < Results.size(); Index++) {
            CentrifugeOutput Output = Results.get(Index);
            if (!Output.isGuaranteed()) {
                String Label = Output.percent() + "%";
                Graphics.drawString(Text, Label, FirstOutputX + Index * OutputSpacing + 9 - Text.width(Label) / 2, LabelY, ChanceColour, false);
            }
        }
        Component Info = Component.translatable("jei.scp_unleashed.centrifuging.info", CentrifugeBlockEntity.WorkDraw, seconds(Separation.getTime()));
        Graphics.drawString(Text, Info, 1, InfoY, TextColour, false);
    }

    public static String seconds(int Ticks) {
        return Ticks % 20 == 0 ? Integer.toString(Ticks / 20) : String.format(Locale.ROOT, "%.1f", Ticks / 20.0f);
    }
}
