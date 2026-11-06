package net.rai.scp_unleashed.scp261.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.scp261.Scp261Registry;
import net.rai.scp_unleashed.scp261.Scp261SnackItem;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class Scp261JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp261Registry.location("scp261_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new Scp261DispensingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<ItemStack> Snacks = new ArrayList<>();
        for (DeferredItem<Scp261SnackItem> Treat : Scp261Registry.snacks()) {
            Snacks.add(new ItemStack(Treat.get()));
        }
        List<ItemStack> Coins = List.of(new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.GOLD_NUGGET));
        Registration.addRecipes(Scp261DispensingCategory.Type, List.of(new Scp261DispensingCategory.Dispensing(Coins, Snacks)));
        Registration.addItemStackInfo(new ItemStack(Scp261Registry.VendingItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp261_vending.1"),
                Component.translatable("jei.scp_unleashed.info.scp261_vending.2"),
                Component.translatable("jei.scp_unleashed.info.scp261_vending.3"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(Scp261Registry.VendingItem.get()), Scp261DispensingCategory.Type);
    }
}
