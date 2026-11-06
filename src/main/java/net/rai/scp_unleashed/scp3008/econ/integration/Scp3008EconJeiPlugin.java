package net.rai.scp_unleashed.scp3008.econ.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;

@JeiPlugin
public class Scp3008EconJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp3008EconRegistry.location("scp3008_econ_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    private static void info(IRecipeRegistration Registration, ItemLike Item, String Key) {
        Registration.addItemStackInfo(new ItemStack(Item), Component.translatable("jei.scp_unleashed.info." + Key + ".1"));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        info(Registration, Scp3008EconRegistry.Particleboard.get(), "scp3008_particleboard");
        info(Registration, Scp3008EconRegistry.SteelTube.get(), "scp3008_steel_tube");
        info(Registration, Scp3008EconRegistry.HexFittings.get(), "scp3008_hex_fittings");
        info(Registration, Scp3008EconRegistry.FabricScraps.get(), "scp3008_fabric_scraps");
        info(Registration, Scp3008EconRegistry.FoamFilling.get(), "scp3008_foam_filling");
        info(Registration, Scp3008EconRegistry.Cardboard.get(), "scp3008_cardboard");
        info(Registration, Scp3008EconRegistry.AcrylicSheet.get(), "scp3008_acrylic_sheet");
        info(Registration, Scp3008EconRegistry.LedBulb.get(), "scp3008_led_bulb");
        info(Registration, Scp3008EconRegistry.Battery.get(), "scp3008_battery");
        info(Registration, Scp3008EconRegistry.AllenKey.get(), "scp3008_allen_key");
        info(Registration, Scp3008EconRegistry.WorkbenchItem.get(), "scp3008_flatpack_workbench");
        info(Registration, Scp3008EconRegistry.Screwdriver.get(), "scp3008_flatpack_screwdriver");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(Scp3008EconRegistry.WorkbenchItem.get()), RecipeTypes.CRAFTING);
    }
}
