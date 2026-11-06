package net.rai.scp_unleashed.oil.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import net.rai.scp_unleashed.oil.CombustionGeneratorBlockEntity;
import net.rai.scp_unleashed.oil.OilExtractorBlockEntity;
import net.rai.scp_unleashed.oil.OilLiquid;
import net.rai.scp_unleashed.oil.OilRegistry;
import net.rai.scp_unleashed.oil.client.ChemicalProcessorScreen;
import net.rai.scp_unleashed.oil.client.CombustionGeneratorScreen;
import net.rai.scp_unleashed.oil.client.DistillationTowerScreen;
import net.rai.scp_unleashed.oil.client.OilRefineryScreen;

@JeiPlugin
public class OilJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "oil_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new DistillingCategory(Registration.getJeiHelpers().getGuiHelper()));
        Registration.addRecipeCategories(new ChemicalProcessingCategory(Registration.getJeiHelpers().getGuiHelper()));
        Registration.addRecipeCategories(new OilRefiningCategory(Registration.getJeiHelpers().getGuiHelper()));
        Registration.addRecipeCategories(new CombustionFuelCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World != null) {
            Registration.addRecipes(DistillingCategory.Type, World.getRecipeManager().getAllRecipesFor(OilRegistry.Distilling.get()));
            Registration.addRecipes(ChemicalProcessingCategory.Type, World.getRecipeManager().getAllRecipesFor(OilRegistry.ChemicalProcessing.get()));
            Registration.addRecipes(OilRefiningCategory.Type, World.getRecipeManager().getAllRecipesFor(OilRegistry.OilRefining.get()));
            Registration.addRecipes(CombustionFuelCategory.Type, World.getRecipeManager().getAllRecipesFor(OilRegistry.CombustionFuelType.get()));
        }
        Registration.addItemStackInfo(new ItemStack(OilRegistry.OilExtractorItem.get()),
                Component.translatable("jei.scp_unleashed.info.oil_extractor.1", OilExtractorBlockEntity.OilPerStroke, OilExtractorBlockEntity.StrokeTicks / 20),
                Component.translatable("jei.scp_unleashed.info.oil_extractor.2", OilExtractorBlockEntity.WorkDraw));
        Registration.addItemStackInfo(new ItemStack(OilRegistry.DistillationTowerItem.get()),
                Component.translatable("jei.scp_unleashed.info.distillation_tower.1"),
                Component.translatable("jei.scp_unleashed.info.distillation_tower.2"));
        Registration.addItemStackInfo(new ItemStack(OilRegistry.ChemicalProcessorItem.get()),
                Component.translatable("jei.scp_unleashed.info.chemical_processor"));
        Registration.addItemStackInfo(new ItemStack(OilRegistry.CombustionGeneratorItem.get()),
                Component.translatable("jei.scp_unleashed.info.combustion_generator.1", CombustionGeneratorBlockEntity.Capacity),
                Component.translatable("jei.scp_unleashed.info.combustion_generator.2"));
        Registration.addItemStackInfo(new ItemStack(OilRegistry.OilRefineryItem.get()),
                Component.translatable("jei.scp_unleashed.info.oil_refinery"));
        for (OilLiquid Liquid : OilRegistry.Liquids) {
            Registration.addItemStackInfo(new ItemStack(Liquid.getBucket().get()),
                    Component.translatable("jei.scp_unleashed.info." + Liquid.getGrade().getName()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(OilRegistry.DistillationTowerItem.get()), DistillingCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(OilRegistry.ChemicalProcessorItem.get()), ChemicalProcessingCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(OilRegistry.OilRefineryItem.get()), OilRefiningCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(OilRegistry.CombustionGeneratorItem.get()), CombustionFuelCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        HoverArea Tower = DistillationTowerScreen.RecipeArea;
        QuietClickArea.register(Registration, DistillationTowerScreen.class, Tower.X(), Tower.Y(), Tower.Width(), Tower.Height(), DistillingCategory.Type);
        HoverArea Processor = ChemicalProcessorScreen.RecipeArea;
        QuietClickArea.register(Registration, ChemicalProcessorScreen.class, Processor.X(), Processor.Y(), Processor.Width(), Processor.Height(), ChemicalProcessingCategory.Type);
        HoverArea Refinery = OilRefineryScreen.RecipeArea;
        QuietClickArea.register(Registration, OilRefineryScreen.class, Refinery.X(), Refinery.Y(), Refinery.Width(), Refinery.Height(), OilRefiningCategory.Type);
        HoverArea Generator = CombustionGeneratorScreen.RecipeArea;
        QuietClickArea.register(Registration, CombustionGeneratorScreen.class, Generator.X(), Generator.Y(), Generator.Width(), Generator.Height(), CombustionFuelCategory.Type);
    }
}
