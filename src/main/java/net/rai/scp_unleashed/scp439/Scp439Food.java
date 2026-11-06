package net.rai.scp_unleashed.scp439;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class Scp439Food {
    public static final int CarryLimit = 8;

    private Scp439Food() {
    }

    public static int each(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return 0;
        }
        FoodProperties Food = Stack.get(DataComponents.FOOD);
        return Food != null ? Math.max(1, Food.nutrition()) : 0;
    }

    public static int value(ItemStack Stack) {
        return each(Stack) * Stack.getCount();
    }

    public static boolean wanted(ItemStack Stack) {
        return each(Stack) > 0;
    }
}
