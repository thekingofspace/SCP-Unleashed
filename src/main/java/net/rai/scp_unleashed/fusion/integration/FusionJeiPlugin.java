package net.rai.scp_unleashed.fusion.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fusion.FusionReactorBlock;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;
import net.rai.scp_unleashed.fusion.FusionRegistry;
import net.rai.scp_unleashed.fusion.client.FusionScreen;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;

import java.util.List;

@JeiPlugin
public class FusionJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fusion_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new FusionCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<FusionCategory.FusionFuel> Fuels = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(FusionReactorBlockEntity::isFuel)
                .map(FusionCategory.FusionFuel::new)
                .toList();
        Registration.addRecipes(FusionCategory.Type, Fuels);
        Registration.addIngredientInfo(new ItemStack(FusionRegistry.FusionReactorItem.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.scp_unleashed.fusion_reactor.info.overview"),
                Component.translatable("jei.scp_unleashed.fusion_reactor.info.startup", FusionReactorBlock.grouped(FusionReactorBlockEntity.StartupEnergy)),
                Component.translatable("jei.scp_unleashed.fusion_reactor.info.fuel", FusionReactorBlockEntity.FuelTicks / 1200),
                Component.translatable("jei.scp_unleashed.fusion_reactor.info.water", FusionReactorBlockEntity.WaterPerTick),
                Component.translatable("jei.scp_unleashed.fusion_reactor.info.output", FusionReactorBlock.grouped(FusionReactorBlockEntity.MaxOutput)),
                Component.translatable("jei.scp_unleashed.fusion_reactor.info.clean"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(FusionRegistry.FusionReactorItem.get()), FusionCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        QuietClickArea.register(Registration, FusionScreen.class, FusionScreen.ViewX, FusionScreen.ViewY, FusionScreen.ViewWidth, FusionScreen.ViewHeight, FusionCategory.Type);
    }
}
