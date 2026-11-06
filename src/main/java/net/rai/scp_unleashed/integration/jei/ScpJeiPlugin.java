package net.rai.scp_unleashed.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.CoalGeneratorBlockEntity;
import net.rai.scp_unleashed.client.CoalGeneratorScreen;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.wire.ChargingRedstone;

import java.util.List;

@JeiPlugin
public class ScpJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new CoalGeneratorCategory(Registration.getJeiHelpers().getGuiHelper()));
        Registration.addRecipeCategories(new ChargingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<CoalGeneratorCategory.GeneratorFuel> Fuels = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(CoalGeneratorBlockEntity::isFuel)
                .map(Stack -> new CoalGeneratorCategory.GeneratorFuel(Stack, CoalGeneratorBlockEntity.burnTicks(Stack)))
                .toList();
        Registration.addRecipes(CoalGeneratorCategory.Type, Fuels);
        Registration.addRecipes(ChargingCategory.Type, ChargingRedstone.charges().stream()
                .map(Entry -> new ChargingCategory.ChargingRecipe(new ItemStack(Entry.Source().get()), new ItemStack(Entry.Result().get()), Entry.Cost()))
                .toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(ModItems.CoalGenerator.get()), CoalGeneratorCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(ModItems.Outlet.get()), ChargingCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(ModItems.ChargeDisperser.get()), ChargingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        Registration.addRecipeClickArea(CoalGeneratorScreen.class, CoalGeneratorScreen.FlameX[0], CoalGeneratorScreen.FlameY,
                CoalGeneratorScreen.FlameX[2] + CoalGeneratorScreen.FlameSize - CoalGeneratorScreen.FlameX[0], CoalGeneratorScreen.FlameSize, CoalGeneratorCategory.Type);
    }
}
