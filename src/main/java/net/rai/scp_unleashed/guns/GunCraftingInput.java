package net.rai.scp_unleashed.guns;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record GunCraftingInput(List<ItemStack> Items) implements RecipeInput {
    @Override
    public ItemStack getItem(int Index) {
        return Items.get(Index);
    }

    @Override
    public int size() {
        return Items.size();
    }
}
