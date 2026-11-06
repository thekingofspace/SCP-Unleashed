package net.rai.scp_unleashed.steam.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.integration.GasJei;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import net.rai.scp_unleashed.steam.CoolingTowerBlockEntity;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;
import net.rai.scp_unleashed.steam.SteamRegistry;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;
import net.rai.scp_unleashed.steam.WaterBoilerBlockEntity;
import net.rai.scp_unleashed.steam.client.WaterBoilerScreen;

import java.util.List;

@JeiPlugin
public class SteamJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "steam_jei_plugin");
    public static final int SampleSteam = 1000;

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        IGuiHelper Helper = Registration.getJeiHelpers().getGuiHelper();
        Registration.addRecipeCategories(new BoilingCategory(Helper));
        Registration.addRecipeCategories(new TurbineCategory(Helper));
        Registration.addRecipeCategories(new IndustrialTurbineCategory(Helper));
        Registration.addRecipeCategories(new CondensingCategory(Helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<BoilingCategory.BoilingFuel> Fuels = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(WaterBoilerBlockEntity::isFuel)
                .map(Stack -> new BoilingCategory.BoilingFuel(Stack, WaterBoilerBlockEntity.burnTicks(Stack)))
                .toList();
        Registration.addRecipes(BoilingCategory.Type, Fuels);
        Registration.addRecipes(TurbineCategory.Type, List.of(new TurbineCategory.TurbineRun(SampleSteam)));
        Registration.addRecipes(IndustrialTurbineCategory.Type, List.of(new IndustrialTurbineCategory.IndustrialRun(SampleSteam)));
        Registration.addRecipes(CondensingCategory.Type, List.of(new CondensingCategory.Condensing(SampleSteam)));
        Registration.addItemStackInfo(new ItemStack(SteamRegistry.WaterBoilerItem.get()),
                Component.translatable("jei.scp_unleashed.info.water_boiler.1", WaterBoilerBlockEntity.BoilRate * 20),
                Component.translatable("jei.scp_unleashed.info.water_boiler.2"));
        Registration.addItemStackInfo(new ItemStack(SteamRegistry.SteamTurbineItem.get()),
                Component.translatable("jei.scp_unleashed.info.steam_turbine.1", SteamTurbineBlockEntity.PowerPerSteam, SteamTurbineBlockEntity.MaxOutput),
                Component.translatable("jei.scp_unleashed.info.steam_turbine.2"));
        Registration.addItemStackInfo(new ItemStack(SteamRegistry.IndustrialTurbineItem.get()),
                Component.translatable("jei.scp_unleashed.info.industrial_turbine.1", IndustrialTurbineBlockEntity.PowerPerSteam, String.format("%,d", IndustrialTurbineBlockEntity.MaxOutput),
                        IndustrialTurbineBlockEntity.InletFlow * 20),
                Component.translatable("jei.scp_unleashed.info.industrial_turbine.2", IndustrialTurbineBlockEntity.SpinUpTicks / 20));
        Registration.addItemStackInfo(new ItemStack(SteamRegistry.CoolingTowerItem.get()),
                Component.translatable("jei.scp_unleashed.info.cooling_tower.1", Math.round(CoolingTowerBlockEntity.MinReturn * 100), Math.round(CoolingTowerBlockEntity.MaxReturn * 100)),
                Component.translatable("jei.scp_unleashed.info.cooling_tower.2"));
        Registration.addIngredientInfo(new GasStack(GasRegistry.LowPressureSteam.get(), GasJei.ListAmount), GasJei.Type,
                Component.translatable("jei.scp_unleashed.info.gas.low_pressure_steam"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(SteamRegistry.WaterBoilerItem.get()), BoilingCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(SteamRegistry.SteamTurbineItem.get()), TurbineCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(SteamRegistry.IndustrialTurbineItem.get()), IndustrialTurbineCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(SteamRegistry.CoolingTowerItem.get()), CondensingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        HoverArea Drum = WaterBoilerScreen.RecipeArea;
        QuietClickArea.register(Registration, WaterBoilerScreen.class, Drum.X(), Drum.Y(), Drum.Width(), Drum.Height(), BoilingCategory.Type);
    }
}
