package net.rai.scp_unleashed.scp529;

import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp458.Scp458Pizza;
import net.rai.scp_unleashed.scp458.Scp458Registry;
import net.rai.scp_unleashed.scp458.Scp458SliceItem;

import java.util.Set;

public final class Scp529Cheese {
    public static final Set<Scp458Pizza> CheesySlices = Set.of(Scp458Pizza.Cheese, Scp458Pizza.FourCheese);

    private Scp529Cheese() {
    }

    public static boolean is(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        if (Stack.is(Scp529Registry.Cheese)) {
            return true;
        }
        return Stack.is(Scp458Registry.Slice.get()) && CheesySlices.contains(Scp458SliceItem.pizza(Stack));
    }
}
