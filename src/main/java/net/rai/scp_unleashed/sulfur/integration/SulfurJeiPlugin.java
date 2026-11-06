package net.rai.scp_unleashed.sulfur.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.client.IndustrialTankScreen;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlockEntity;

import java.util.List;

@JeiPlugin
public class SulfurJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = SulfurRegistry.location("sulfur_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new AcidMixingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addRecipes(AcidMixingCategory.Type, List.of(AcidMixingCategory.Mixing.standard()));
        Registration.addItemStackInfo(new ItemStack(SulfurRegistry.TankItem.get()),
                Component.translatable(Prefix + "industrial_tank.1"),
                Component.translatable(Prefix + "industrial_tank.2", IndustrialTankBlockEntity.WorkDraw),
                Component.translatable(Prefix + "industrial_tank.3"),
                Component.translatable(Prefix + "industrial_tank.4"));
        Registration.addItemStackInfo(List.of(new ItemStack(SulfurRegistry.OreItem.get()), new ItemStack(SulfurRegistry.DeepslateOreItem.get())),
                Component.translatable(Prefix + "sulfur_ore"));
        Registration.addItemStackInfo(new ItemStack(SulfurRegistry.Sulfur.get()),
                Component.translatable(Prefix + "sulfur"));
        Registration.addItemStackInfo(new ItemStack(SulfurRegistry.AcidBucket.get()),
                Component.translatable(Prefix + "sulfuric_acid_bucket.1"),
                Component.translatable(Prefix + "sulfuric_acid_bucket.2"));
        Registration.addIngredientInfo(new FluidStack(SulfurRegistry.Acid.get(), FluidType.BUCKET_VOLUME), NeoForgeTypes.FLUID_STACK,
                Component.translatable(Prefix + "sulfuric_acid"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(SulfurRegistry.TankItem.get()), AcidMixingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        HoverArea Chamber = IndustrialTankScreen.RecipeArea;
        QuietClickArea.register(Registration, IndustrialTankScreen.class, Chamber.X(), Chamber.Y(), Chamber.Width(), Chamber.Height(), AcidMixingCategory.Type);
    }
}
