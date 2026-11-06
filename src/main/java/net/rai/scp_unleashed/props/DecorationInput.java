package net.rai.scp_unleashed.props;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record DecorationInput(ItemStack Material, ItemStack Extra) implements RecipeInput {
    @Override
    public ItemStack getItem(int Index) {
        return Index == 0 ? Material : Extra;
    }

    @Override
    public int size() {
        return 2;
    }
}
