package net.rai.scp_unleashed.refinery.integration;

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
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.refinery.OrePurifierBlockEntity;
import net.rai.scp_unleashed.refinery.PurifierOutput;
import net.rai.scp_unleashed.refinery.PurifyingRecipe;
import net.rai.scp_unleashed.refinery.RefineryRegistry;
import net.rai.scp_unleashed.refinery.client.OrePurifierScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PurifyingCategory implements IRecipeCategory<RecipeHolder<PurifyingRecipe>> {
    public static final RecipeType<RecipeHolder<PurifyingRecipe>> Type = RecipeType.createRecipeHolderType(RefineryRegistry.PurifyingId);
    public static final int Width = 176;
    public static final int Height = 70;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 168;
    public static final int InfoY = 1;
    public static final int WaterX = 3;
    public static final int WasteX = 163;
    public static final int TankY = 14;
    public static final int TankWidth = 10;
    public static final int TankHeight = 50;
    public static final int GaugeCapacity = 1000;
    public static final int InputX = 17;
    public static final int InputY = 30;
    public static final int WindowX = 39;
    public static final int WindowY = 27;
    public static final int ArrowX = 69;
    public static final int ArrowY = 32;
    public static final int FirstOutputX = 90;
    public static final int OutputSpacing = 22;
    public static final int OutputColumns = 3;
    public static final int FirstRowY = 15;
    public static final int RowSpacing = 28;
    public static final int LabelGap = 19;
    public static final long FrameMillis = 90L;
    private static final int TextColour = 0xFF404040;
    private static final int ChanceColour = 0xFFB0781A;

    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final Map<Integer, IDrawableAnimated> Arrows = new HashMap<>();

    public PurifyingCategory(IGuiHelper Helper) {
        this.Helper = Helper;
        Icon = Helper.createDrawableItemLike(RefineryRegistry.OrePurifierItem.get());
        Background = Helper.createDrawable(OrePurifierScreen.Texture, BackgroundU, BackgroundV, Width, Height);
    }

    public static int outputX(int Index) {
        return FirstOutputX + (Index % OutputColumns) * OutputSpacing;
    }

    public static int outputY(int Index) {
        return FirstRowY + (Index / OutputColumns) * RowSpacing;
    }

    @Override
    public RecipeType<RecipeHolder<PurifyingRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.ore_purifying");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<PurifyingRecipe> Entry, IFocusGroup Focuses) {
        PurifyingRecipe Wash = Entry.value();
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX + 1, InputY + 1).addItemStacks(List.of(Wash.getInput().getItems()));
        if (Wash.getWater() > 0) {
            Builder.addSlot(RecipeIngredientRole.INPUT, WaterX, TankY).setFluidRenderer(GaugeCapacity, false, TankWidth, TankHeight)
                    .addFluidStack(Fluids.WATER, Wash.getWater());
        }
        if (Wash.getWaste() > 0) {
            Builder.addSlot(RecipeIngredientRole.OUTPUT, WasteX, TankY).setFluidRenderer(GaugeCapacity, false, TankWidth, TankHeight)
                    .addFluidStack(FissionRegistry.WasteWater.get(), Wash.getWaste());
        }
        List<PurifierOutput> Results = Wash.getResults();
        for (int Index = 0; Index < Results.size(); Index++) {
            PurifierOutput Output = Results.get(Index);
            IRecipeSlotBuilder Placed = Builder.addSlot(RecipeIngredientRole.OUTPUT, outputX(Index) + 1, outputY(Index) + 1).addItemStack(Output.Stack());
            if (!Output.isGuaranteed()) {
                Placed.addRichTooltipCallback((View, Lines) -> Lines.add(Component.translatable("jei.scp_unleashed.ore_purifying.chance", Output.percent()).withStyle(ChatFormatting.GOLD)));
            }
        }
    }

    @Override
    public void draw(RecipeHolder<PurifyingRecipe> Entry, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        PurifyingRecipe Wash = Entry.value();
        Font Text = Minecraft.getInstance().font;
        Background.draw(Graphics);
        int Frame = (int) ((Util.getMillis() / FrameMillis) % OrePurifierScreen.DrumFrames);
        Graphics.blit(OrePurifierScreen.DrumTexture, WindowX, WindowY, (Frame % OrePurifierScreen.DrumColumns) * OrePurifierScreen.DrumSize,
                (Frame / OrePurifierScreen.DrumColumns) * OrePurifierScreen.DrumSize, OrePurifierScreen.DrumSize, OrePurifierScreen.DrumSize,
                OrePurifierScreen.DrumSheetWidth, OrePurifierScreen.DrumSheetHeight);
        Arrows.computeIfAbsent(Wash.getTime(), Ticks -> Helper.drawableBuilder(OrePurifierScreen.Texture, OrePurifierScreen.ArrowU, OrePurifierScreen.ArrowV,
                OrePurifierScreen.ArrowWidth, OrePurifierScreen.ArrowHeight).buildAnimated(Ticks, IDrawableAnimated.StartDirection.LEFT, false)).draw(Graphics, ArrowX, ArrowY);
        List<PurifierOutput> Results = Wash.getResults();
        for (int Index = 0; Index < Results.size(); Index++) {
            PurifierOutput Output = Results.get(Index);
            if (!Output.isGuaranteed()) {
                String Label = Output.percent() + "%";
                Graphics.drawString(Text, Label, outputX(Index) + 9 - Text.width(Label) / 2, outputY(Index) + LabelGap, ChanceColour, false);
            }
        }
        Component Info = Component.translatable("jei.scp_unleashed.ore_purifying.info", OrePurifierBlockEntity.WorkDraw, seconds(Wash.getTime()));
        Graphics.drawString(Text, Info, 1, InfoY, TextColour, false);
    }

    public static String seconds(int Ticks) {
        return Ticks % 20 == 0 ? Integer.toString(Ticks / 20) : String.format(Locale.ROOT, "%.1f", Ticks / 20.0f);
    }
}
