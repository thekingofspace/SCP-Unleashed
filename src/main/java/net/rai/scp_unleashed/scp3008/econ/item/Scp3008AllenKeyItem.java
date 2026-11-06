package net.rai.scp_unleashed.scp3008.econ.item;

import net.minecraft.world.item.ItemStack;

public class Scp3008AllenKeyItem extends Scp3008StoreItem {
    public Scp3008AllenKeyItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack Stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack Stack) {
        return Stack.copyWithCount(1);
    }
}
