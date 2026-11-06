package net.rai.scp_unleashed.anomaly;

import net.minecraft.world.item.ItemStack;

public interface Extractable {
    FragmentRarity getRarity();

    float getExtraction();

    void setExtraction(float Progress);

    boolean canBeExtracted();

    void collapse();

    default int getRiftColour() {
        return getRarity().getColour();
    }

    default String getRiftLabel() {
        return getRarity().getSerializedName();
    }

    default ItemStack createFragment() {
        return ItemStack.EMPTY;
    }
}
