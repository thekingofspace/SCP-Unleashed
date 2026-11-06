package net.rai.scp_unleashed.scp261.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp261.Scp261Registry;

import java.util.List;

public class Scp261DispensingCategory implements IRecipeCategory<Scp261DispensingCategory.Dispensing> {
    public static final RecipeType<Dispensing> Type = RecipeType.create(Scp_unleashed.ModId, "scp261_dispensing", Dispensing.class);
    public static final int Width = 132;
    public static final int Height = 92;
    public static final int CoinX = 6;
    public static final int CoinY = 22;
    public static final int MachineX = 6;
    public static final int MachineY = 46;
    public static final int ArrowX = 27;
    public static final int ArrowY = 34;
    public static final int GridX = 56;
    public static final int GridY = 4;
    public static final int Pitch = 18;
    public static final int Columns = 4;
    public static final int NoteY = 80;
    private static final int TextColour = 0xFF404040;

    private final IDrawable Icon;
    private final IDrawableStatic Arrow;
    private final IDrawableStatic Slot;

    public Scp261DispensingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(Scp261Registry.VendingItem.get());
        Arrow = Helper.getRecipeArrow();
        Slot = Helper.getSlotDrawable();
    }

    public record Dispensing(List<ItemStack> Coins, List<ItemStack> Snacks) {
    }

    @Override
    public RecipeType<Dispensing> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.scp261_dispensing");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, Dispensing Run, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, CoinX, CoinY).addItemStacks(Run.Coins());
        Builder.addSlot(RecipeIngredientRole.CATALYST, MachineX, MachineY).addItemStack(new ItemStack(Scp261Registry.VendingItem.get()));
        for (int Index = 0; Index < Run.Snacks().size(); Index++) {
            Builder.addSlot(RecipeIngredientRole.OUTPUT, GridX + (Index % Columns) * Pitch, GridY + (Index / Columns) * Pitch).addItemStack(Run.Snacks().get(Index));
        }
    }

    @Override
    public void draw(Dispensing Run, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Slot.draw(Graphics, CoinX - 1, CoinY - 1);
        Slot.draw(Graphics, MachineX - 1, MachineY - 1);
        for (int Index = 0; Index < Run.Snacks().size(); Index++) {
            Slot.draw(Graphics, GridX - 1 + (Index % Columns) * Pitch, GridY - 1 + (Index / Columns) * Pitch);
        }
        Arrow.draw(Graphics, ArrowX, ArrowY);
        Font Text = Minecraft.getInstance().font;
        Component Note = Component.translatable("jei.scp_unleashed.scp261_dispensing.note");
        Graphics.drawString(Text, Note, (Width - Text.width(Note)) / 2, NoteY, TextColour, false);
    }
}
