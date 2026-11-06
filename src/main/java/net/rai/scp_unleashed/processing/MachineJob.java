package net.rai.scp_unleashed.processing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public record MachineJob(RecipeHolder<?> Holder, ItemStack Result, int Ticks, int Consumes, int Draw) {
    public ResourceLocation id() {
        return Holder.id();
    }
}
