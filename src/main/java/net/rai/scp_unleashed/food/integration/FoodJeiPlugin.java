package net.rai.scp_unleashed.food.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.food.FoodRegistry;
import net.rai.scp_unleashed.food.Snack;
import net.rai.scp_unleashed.food.VendingMachineBlockEntity;

@JeiPlugin
public class FoodJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "food");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        for (Snack Product : Snack.values()) {
            Registration.addItemStackInfo(new ItemStack(FoodRegistry.snack(Product)), Component.translatable("jei.scp_unleashed.vending.snack", Product.getPrice(), Product.getCode()));
        }
        Registration.addItemStackInfo(new ItemStack(FoodRegistry.VendingMachineItem.get()), Component.translatable("jei.scp_unleashed.vending.machine", VendingMachineBlockEntity.MaxCredit));
    }
}
