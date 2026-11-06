package net.rai.scp_unleashed.fission.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.GasDissolverBlockEntity;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.fission.client.SonicVibratorScreen;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;

import java.util.List;

@JeiPlugin
public class FissionJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new SonicVibratingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addRecipes(SonicVibratingCategory.Type, List.of(new SonicVibratingCategory.Cleaning(SonicVibratingCategory.CleanBatch)));
        Registration.addItemStackInfo(new ItemStack(FissionRegistry.HeatExchangerItem.get()),
                Component.translatable("jei.scp_unleashed.info.fission_heat_exchanger.1"),
                Component.translatable("jei.scp_unleashed.info.fission_heat_exchanger.2"));
        Registration.addItemStackInfo(new ItemStack(FissionRegistry.WasteWaterBucket.get()),
                Component.translatable("jei.scp_unleashed.info.waste_water"));
        Registration.addItemStackInfo(new ItemStack(FissionRegistry.GasDissolverItem.get()),
                Component.translatable("jei.scp_unleashed.info.gas_dissolver.1", GasDissolverBlockEntity.formatAmount(GasDissolverBlockEntity.WaterPerGas)),
                Component.translatable("jei.scp_unleashed.info.gas_dissolver.2", (int) GasDissolverBlockEntity.ScrubRadius));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(FissionRegistry.SonicVibratorItem.get()), SonicVibratingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        HoverArea Vibrator = SonicVibratorScreen.RecipeArea;
        QuietClickArea.register(Registration, SonicVibratorScreen.class, Vibrator.X(), Vibrator.Y(), Vibrator.Width(), Vibrator.Height(), SonicVibratingCategory.Type);
    }
}
