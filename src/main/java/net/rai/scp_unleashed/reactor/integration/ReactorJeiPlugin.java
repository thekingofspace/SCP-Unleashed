package net.rai.scp_unleashed.reactor.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.FissionTags;
import net.rai.scp_unleashed.fission.ReactorFuel;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.fission.integration.IrradiatingCategory;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.integration.GasSourceCategory;
import net.rai.scp_unleashed.instability.InstabilityRegistry;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import net.rai.scp_unleashed.reactor.IrradiationChamberBlockEntity;
import net.rai.scp_unleashed.reactor.IrradiationChamberMenu;
import net.rai.scp_unleashed.reactor.ReactorControllerBlockEntity;
import net.rai.scp_unleashed.reactor.ReactorRegistry;
import net.rai.scp_unleashed.reactor.TowerFuel;
import net.rai.scp_unleashed.reactor.client.IrradiationChamberScreen;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@JeiPlugin
public class ReactorJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_tower_jei_plugin");
    public static final RecipeType<GasSourceCategory.Source> SteamType = RecipeType.create(Scp_unleashed.ModId, "fission_tower_steam", GasSourceCategory.Source.class);
    public static final int SteamSample = 100;

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        IGuiHelper Helper = Registration.getJeiHelpers().getGuiHelper();
        Registration.addRecipeCategories(new IrradiatingCategory(Helper, ReactorRegistry.ChamberItem.get()));
        Registration.addRecipeCategories(new TowerFuelCategory(Helper));
        Registration.addRecipeCategories(new GasSourceCategory(Helper, GasSourceCategory.ExhaustType, "jei.scp_unleashed.fission_exhaust", ReactorRegistry.FuelTowerItem.get()));
        Registration.addRecipeCategories(new GasSourceCategory(Helper, SteamType, "jei.scp_unleashed.fission_tower_steam", ReactorRegistry.CoolantTowerItem.get()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World != null) {
            Registration.addRecipes(IrradiatingCategory.Type, World.getRecipeManager().getAllRecipesFor(FissionRegistry.Irradiating.get()));
        }
        List<ItemStack> Rods = new ArrayList<>();
        List<TowerFuelCategory.Rod> Fuels = new ArrayList<>();
        ItemStack Spent = ReactorFuel.depletedRod();
        float HeaviestGas = 0.0f;
        for (Holder<Item> Entry : BuiltInRegistries.ITEM.getTagOrEmpty(FissionTags.FissionFuel)) {
            ItemStack Stack = new ItemStack(Entry);
            float Heat = TowerFuel.heatOf(Stack);
            float Gas = Heat * ReactorTuning.GasPerHeat * ReactorTuning.TicksPerSecond;
            HeaviestGas = Math.max(HeaviestGas, Gas);
            Rods.add(Stack);
            Fuels.add(new TowerFuelCategory.Rod(Stack, Spent, Heat, TowerFuel.burnTicks(Stack) / 1200, Gas));
        }
        Registration.addRecipes(TowerFuelCategory.Type, Fuels);
        int PerMinute = Math.round(HeaviestGas * 60.0f);
        Registration.addRecipes(GasSourceCategory.ExhaustType, List.of(new GasSourceCategory.Source(new ItemStack(ReactorRegistry.FuelTowerItem.get()), Rods, FluidStack.EMPTY,
                new GasStack(GasRegistry.ToxicGas.get(), PerMinute), PerMinute, "jei.scp_unleashed.fission_exhaust.tower", List.of(String.format(Locale.ROOT, "%.1f", HeaviestGas)))));
        Registration.addRecipes(SteamType, List.of(
                new GasSourceCategory.Source(new ItemStack(ReactorRegistry.CoolantTowerItem.get()), List.of(), new FluidStack(Fluids.WATER, SteamSample),
                        new GasStack(GasRegistry.Steam.get(), SteamSample), SteamSample, "jei.scp_unleashed.fission_tower_steam.coolant", List.of(Math.round(ReactorTuning.CoolantCap))),
                new GasSourceCategory.Source(new ItemStack(ReactorRegistry.SteamChannelItem.get()), List.of(), new FluidStack(Fluids.WATER, SteamSample),
                        new GasStack(GasRegistry.Steam.get(), SteamSample), SteamSample, "jei.scp_unleashed.fission_tower_steam.channel", List.of(Math.round(ReactorTuning.ChannelCap)))));
        Registration.addItemStackInfo(new ItemStack(ReactorRegistry.FuelTowerItem.get()),
                Component.translatable("jei.scp_unleashed.info.fuel_tower.1", ReactorTuning.MaxHeight),
                Component.translatable("jei.scp_unleashed.info.fuel_tower.2"),
                Component.translatable("jei.scp_unleashed.info.fuel_tower.3", Math.round(ReactorTuning.HotLevel), Math.round(ReactorTuning.CriticalLevel)));
        Registration.addItemStackInfo(new ItemStack(ReactorRegistry.ControlRodTowerItem.get()),
                Component.translatable("jei.scp_unleashed.info.control_rod_tower.1"),
                Component.translatable("jei.scp_unleashed.info.control_rod_tower.2"));
        Registration.addItemStackInfo(new ItemStack(ReactorRegistry.BoosterTowerItem.get()),
                Component.translatable("jei.scp_unleashed.info.booster_tower.1"),
                Component.translatable("jei.scp_unleashed.info.booster_tower.2", Math.round(ReactorTuning.BoostPerSegment * 100.0f), Math.round(ReactorTuning.MaxBoost * 100.0f)),
                Component.translatable("jei.scp_unleashed.info.booster_tower.3"));
        Registration.addItemStackInfo(new ItemStack(ReactorRegistry.CoolantTowerItem.get()),
                Component.translatable("jei.scp_unleashed.info.coolant_tower.1", Math.round(ReactorTuning.CoolantCap)),
                Component.translatable("jei.scp_unleashed.info.coolant_tower.2"));
        Registration.addItemStackInfo(new ItemStack(ReactorRegistry.SteamChannelItem.get()),
                Component.translatable("jei.scp_unleashed.info.steam_channel.1"),
                Component.translatable("jei.scp_unleashed.info.steam_channel.2", Math.round(ReactorTuning.ChannelPeak)));
        Registration.addItemStackInfo(new ItemStack(ReactorRegistry.ControllerItem.get()),
                Component.translatable("jei.scp_unleashed.info.reactor_controller.1", ReactorControllerBlockEntity.LinkRange),
                Component.translatable("jei.scp_unleashed.info.reactor_controller.2"));
        Registration.addItemStackInfo(new ItemStack(ReactorRegistry.ChamberItem.get()),
                Component.translatable("jei.scp_unleashed.info.irradiation_chamber.1"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(ReactorRegistry.ChamberItem.get()), IrradiatingCategory.Type);
        Registration.addRecipeCatalyst(new ItemStack(ReactorRegistry.FuelTowerItem.get()), TowerFuelCategory.Type, GasSourceCategory.ExhaustType);
        Registration.addRecipeCatalyst(new ItemStack(ReactorRegistry.CoolantTowerItem.get()), SteamType);
        Registration.addRecipeCatalyst(new ItemStack(ReactorRegistry.SteamChannelItem.get()), SteamType);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        HoverArea Chamber = IrradiationChamberScreen.RecipeArea;
        QuietClickArea.register(Registration, IrradiationChamberScreen.class, Chamber.X(), Chamber.Y(), Chamber.Width(), Chamber.Height(), IrradiatingCategory.Type);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration Registration) {
        Registration.addRecipeTransferHandler(IrradiationChamberMenu.class, ReactorRegistry.ChamberMenu.get(), IrradiatingCategory.Type,
                IrradiationChamberBlockEntity.InputSlot, 1, IrradiationChamberMenu.MachineSlots, IrradiationChamberMenu.HotbarEnd - IrradiationChamberMenu.MachineSlots);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime Runtime) {
        Runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(FissionRegistry.ReactorItem.get()), new ItemStack(InstabilityRegistry.ReactorItem.get())));
    }
}
