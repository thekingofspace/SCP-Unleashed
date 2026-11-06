package net.rai.scp_unleashed.gas.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.HeatExchangerBlockEntity;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.gas.Gas;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTankBlockEntity;
import net.rai.scp_unleashed.gas.VaporizerBlockEntity;
import net.rai.scp_unleashed.gas.VaporizingRecipe;
import net.rai.scp_unleashed.gas.client.VaporizerScreen;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import net.rai.scp_unleashed.plumbing.DrainBlockEntity;
import net.rai.scp_unleashed.plumbing.PlumbingRegistry;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class GasJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gas_jei_plugin");
    public static final int SteamSample = 100;

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    private static List<GasStack> allGases() {
        List<GasStack> All = new ArrayList<>();
        for (Gas Kind : GasRegistry.Gases) {
            All.add(new GasStack(Kind, GasJei.ListAmount));
        }
        return All;
    }

    @Override
    public void registerIngredients(IModIngredientRegistration Registration) {
        Registration.register(GasJei.Type, allGases(), GasJei.Helper.Instance, GasJei.IconRenderer.Instance, GasStack.CODEC);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        IGuiHelper Helper = Registration.getJeiHelpers().getGuiHelper();
        Registration.addRecipeCategories(new VaporizingCategory(Helper));
        Registration.addRecipeCategories(new GasSourceCategory(Helper, GasSourceCategory.SteamType, "jei.scp_unleashed.exchanger_steam", FissionRegistry.HeatExchangerItem.get()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World != null) {
            Registration.addRecipes(VaporizingCategory.Type, World.getRecipeManager().getAllRecipesFor(GasRegistry.Vaporizing.get()));
        }
        Registration.addRecipes(GasSourceCategory.SteamType, List.of(new GasSourceCategory.Source(new ItemStack(FissionRegistry.HeatExchangerItem.get()), List.of(),
                new FluidStack(Fluids.WATER, SteamSample), new GasStack(GasRegistry.Steam.get(), SteamSample * HeatExchangerBlockEntity.SteamPerWater),
                SteamSample * HeatExchangerBlockEntity.SteamPerWater, "jei.scp_unleashed.exchanger_steam.info", List.of(HeatExchangerBlockEntity.SteamPerWater))));
        Registration.addItemStackInfo(new ItemStack(GasRegistry.GasTankItem.get()),
                Component.translatable("jei.scp_unleashed.info.gas_tank.1", GasTankBlockEntity.Capacity),
                Component.translatable("jei.scp_unleashed.info.gas_tank.2"));
        Registration.addItemStackInfo(new ItemStack(GasRegistry.VaporizerItem.get()),
                Component.translatable("jei.scp_unleashed.info.liquid_vaporizer.1", VaporizingRecipe.DefaultPower),
                Component.translatable("jei.scp_unleashed.info.liquid_vaporizer.2", VaporizerBlockEntity.GasCapacity));
        Registration.addItemStackInfo(new ItemStack(PlumbingRegistry.DrainItem.get()),
                Component.translatable("jei.scp_unleashed.info.drain.1", DrainBlockEntity.FlowPerTick * 20),
                Component.translatable("jei.scp_unleashed.info.drain.2", DrainBlockEntity.MaxReach));
        Registration.addIngredientInfo(new GasStack(GasRegistry.Steam.get(), GasJei.ListAmount), GasJei.Type, Component.translatable("jei.scp_unleashed.info.gas.steam"));
        Registration.addIngredientInfo(new GasStack(GasRegistry.ToxicGas.get(), GasJei.ListAmount), GasJei.Type, Component.translatable("jei.scp_unleashed.info.gas.toxic_gas"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(GasRegistry.VaporizerItem.get()), VaporizingCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(FissionRegistry.HeatExchangerItem.get()), GasSourceCategory.SteamType);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        HoverArea Chamber = VaporizerScreen.RecipeArea;
        QuietClickArea.register(Registration, VaporizerScreen.class, Chamber.X(), Chamber.Y(), Chamber.Width(), Chamber.Height(), VaporizingCategory.Type);
    }
}
