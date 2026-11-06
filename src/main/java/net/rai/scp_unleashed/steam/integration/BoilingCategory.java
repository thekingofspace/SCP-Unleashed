package net.rai.scp_unleashed.steam.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.integration.GasJei;
import net.rai.scp_unleashed.steam.SteamRegistry;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;
import net.rai.scp_unleashed.steam.WaterBoilerBlockEntity;

import java.util.HashMap;
import java.util.Map;

public class BoilingCategory implements IRecipeCategory<BoilingCategory.BoilingFuel> {
    public static final RecipeType<BoilingFuel> Type = RecipeType.create(Scp_unleashed.ModId, "boiling", BoilingFuel.class);
    public static final int Width = 166;
    public static final int Height = 50;
    public static final int SlotX = 1;
    public static final int SlotY = 17;
    public static final int FlameX = 21;
    public static final int FlameY = 19;
    public static final int WaterX = 40;
    public static final int ArrowX = 59;
    public static final int ArrowY = 17;
    public static final int SteamX = 86;
    public static final int TextX = 107;

    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic Slot;
    private final IDrawableStatic FlameEmpty;
    private final IDrawableStatic Arrow;
    private final IDrawableStatic Tank;
    private final Map<Integer, IDrawableAnimated> Flames = new HashMap<>();

    public BoilingCategory(IGuiHelper Helper) {
        this.Helper = Helper;
        Icon = Helper.createDrawableItemLike(SteamRegistry.WaterBoilerItem.get());
        Slot = Helper.getSlotDrawable();
        FlameEmpty = Helper.getRecipeFlameEmpty();
        Arrow = Helper.getRecipeArrow();
        Tank = SteamJeiLayout.tankInset(Helper);
    }

    public record BoilingFuel(ItemStack Stack, int BurnTicks) {
        public int steam() {
            return WaterBoilerBlockEntity.steamFor(BurnTicks);
        }

        public ItemStack remainder() {
            return Stack.getCraftingRemainingItem();
        }
    }

    @Override
    public RecipeType<BoilingFuel> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.boiling");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, BoilingFuel Fuel, IFocusGroup Focuses) {
        int Amount = Math.max(1, Fuel.steam());
        IRecipeSlotBuilder Input = Builder.addSlot(RecipeIngredientRole.INPUT, SlotX, SlotY).addItemStack(Fuel.Stack());
        ItemStack Remainder = Fuel.remainder();
        if (!Remainder.isEmpty()) {
            Input.addRichTooltipCallback((SlotView, Lines) -> Lines.add(Component.translatable("jei.scp_unleashed.coal_generator.returns", Remainder.getHoverName()).withStyle(ChatFormatting.GRAY)));
        }
        Builder.addSlot(RecipeIngredientRole.INPUT, WaterX, SteamJeiLayout.TankY).setFluidRenderer(Amount, false, SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight)
                .addFluidStack(Fluids.WATER, Amount);
        Builder.addSlot(RecipeIngredientRole.OUTPUT, SteamX, SteamJeiLayout.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Amount, SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight))
                .addIngredient(GasJei.Type, new GasStack(GasRegistry.Steam.get(), Amount));
    }

    @Override
    public void draw(BoilingFuel Fuel, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Slot.draw(Graphics, SlotX - 1, SlotY - 1);
        FlameEmpty.draw(Graphics, FlameX, FlameY);
        Flames.computeIfAbsent(Math.max(1, Fuel.BurnTicks() / WaterBoilerBlockEntity.BurnSpeed), Ticks -> Helper.createAnimatedRecipeFlame(Ticks)).draw(Graphics, FlameX, FlameY);
        Tank.draw(Graphics, WaterX - 1, SteamJeiLayout.TankY - 1);
        Tank.draw(Graphics, SteamX - 1, SteamJeiLayout.TankY - 1);
        Arrow.draw(Graphics, ArrowX, ArrowY);
        long Power = (long) Fuel.steam() * SteamTurbineBlockEntity.PowerPerSteam;
        SteamJeiLayout.left(Graphics, Component.translatable("jei.scp_unleashed.boiling.time", Fuel.BurnTicks() / WaterBoilerBlockEntity.BurnSpeed / 20), TextX, 9);
        SteamJeiLayout.left(Graphics, Component.translatable("jei.scp_unleashed.boiling.steam", String.format("%,d", Fuel.steam())), TextX, 21);
        SteamJeiLayout.left(Graphics, Component.translatable("jei.scp_unleashed.boiling.power", String.format("%,d", Power)), TextX, 33);
    }

    @Override
    public void getTooltip(ITooltipBuilder Tooltip, BoilingFuel Fuel, IRecipeSlotsView Slots, double MouseX, double MouseY) {
        if (MouseX >= TextX && MouseX < Width && MouseY >= 7 && MouseY < 43) {
            Tooltip.add(Component.translatable("jei.scp_unleashed.boiling.hint", WaterBoilerBlockEntity.BurnSpeed, SteamTurbineBlockEntity.PowerPerSteam));
        }
    }
}
