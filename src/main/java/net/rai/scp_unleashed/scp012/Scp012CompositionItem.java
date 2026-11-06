package net.rai.scp_unleashed.scp012;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp012CompositionItem extends Item {
    public static final int Complete = 100;
    public static final int BarSteps = 13;
    public static final int BarColour = 0xA0141E;
    public static final int BarColourFull = 0xD8202E;
    public static final String Prefix = "item.scp_unleashed.scp012_composition.";

    public Scp012CompositionItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp012CompositionItem;
    }

    public static int completeness(ItemStack Stack) {
        return Mth.clamp(Stack.getOrDefault(Scp012Registry.Completeness.get(), 0), 0, Complete);
    }

    public static boolean isComplete(ItemStack Stack) {
        return completeness(Stack) >= Complete;
    }

    public static void setCompleteness(ItemStack Stack, int Value) {
        int Clamped = Mth.clamp(Value, 0, Complete);
        if (Clamped == 0) {
            Stack.remove(Scp012Registry.Completeness.get());
        } else {
            Stack.set(Scp012Registry.Completeness.get(), Clamped);
        }
    }

    public static ItemStack blank() {
        return new ItemStack(Scp012Registry.Composition.get());
    }

    public static ItemStack withCompleteness(int Value) {
        ItemStack Stack = blank();
        setCompleteness(Stack, Value);
        return Stack;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack OldStack, ItemStack NewStack, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(OldStack, NewStack);
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return completeness(Stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return Math.round(BarSteps * completeness(Stack) / (float) Complete);
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return isComplete(Stack) ? BarColourFull : BarColour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        int Value = completeness(Stack);
        if (Value >= Complete) {
            Lines.add(Component.translatable(Prefix + "complete").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        } else {
            Lines.add(Component.translatable(Prefix + "meter", Component.literal(Value + "%").withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.GRAY));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "held").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable(Prefix + "window", Scp012Exposure.WindowRange).withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable(Prefix + "drain").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "finish").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "inert").withStyle(ChatFormatting.GOLD));
        }
    }
}
