package net.rai.scp_unleashed.fission.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.IrradiatingRecipe;
import net.rai.scp_unleashed.fission.client.FissionReactorScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IrradiatingCategory implements IRecipeCategory<RecipeHolder<IrradiatingRecipe>> {
    public static final RecipeType<RecipeHolder<IrradiatingRecipe>> Type = RecipeType.createRecipeHolderType(FissionRegistry.IrradiatingId);
    public static final ResourceLocation Sheet = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/jei.png");
    public static final int SheetSize = 256;
    public static final int Width = 150;
    public static final int InfoMargin = 4;
    public static final int Height = 52;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 0;
    public static final int InputX = 36;
    public static final int OutputX = 96;
    public static final int SlotY = 17;
    public static final int ArrowX = 61;
    public static final int ArrowY = 17;
    public static final int TrefoilX = 68;
    public static final int TrefoilY = 3;
    public static final int InfoY = 41;
    public static final int TextColour = 0xFF404040;

    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final Map<Integer, IDrawableAnimated> Arrows = new HashMap<>();

    public IrradiatingCategory(IGuiHelper Helper, ItemLike Machine) {
        this.Helper = Helper;
        Icon = Helper.createDrawableItemLike(Machine);
        Background = sheetPart(Helper, BackgroundU, BackgroundV, Width, Height);
    }

    public static IDrawableStatic sheetPart(IGuiHelper Helper, int U, int V, int PartWidth, int PartHeight) {
        return Helper.drawableBuilder(Sheet, U, V, PartWidth, PartHeight).setTextureSize(SheetSize, SheetSize).build();
    }

    @Override
    public RecipeType<RecipeHolder<IrradiatingRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.irradiating");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<IrradiatingRecipe> Entry, IFocusGroup Focuses) {
        IrradiatingRecipe Recipe = Entry.value();
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX, SlotY).addItemStacks(List.of(Recipe.getInput().getItems()));
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, SlotY).addItemStack(Recipe.getResult());
    }

    @Override
    public void draw(RecipeHolder<IrradiatingRecipe> Entry, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        IrradiatingRecipe Recipe = Entry.value();
        Background.draw(Graphics);
        Arrows.computeIfAbsent(Recipe.getTime(), Ticks -> Helper.drawableBuilder(FissionReactorScreen.Texture, FissionReactorScreen.ArrowFillU, FissionReactorScreen.ArrowFillV,
                FissionReactorScreen.ArrowWidth, FissionReactorScreen.ArrowHeight).setTextureSize(FissionReactorScreen.TextureSize, FissionReactorScreen.TextureSize)
                .buildAnimated(Ticks, IDrawableAnimated.StartDirection.LEFT, false)).draw(Graphics, ArrowX, ArrowY);
        Font Text = Minecraft.getInstance().font;
        Component Info = Component.translatable("jei.scp_unleashed.irradiating.time", seconds(Recipe.getTime()));
        int Room = Width - InfoMargin * 2;
        int Wide = Text.width(Info);
        if (Wide <= Room) {
            Graphics.drawString(Text, Info, (Width - Wide) / 2, InfoY, TextColour, false);
            return;
        }
        float Scale = (float) Room / Wide;
        Graphics.pose().pushPose();
        Graphics.pose().translate(InfoMargin, InfoY + Text.lineHeight * (1.0f - Scale) / 2.0f, 0.0f);
        Graphics.pose().scale(Scale, Scale, 1.0f);
        Graphics.drawString(Text, Info, 0, 0, TextColour, false);
        Graphics.pose().popPose();
    }

    public static String seconds(int Ticks) {
        return Ticks % 20 == 0 ? Integer.toString(Ticks / 20) : String.format(Locale.ROOT, "%.1f", Ticks / 20.0f);
    }
}
