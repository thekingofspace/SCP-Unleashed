package net.rai.scp_unleashed.advmaterials.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsBenchBlockEntity;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsMenu;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRecipes;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRegistry;
import net.rai.scp_unleashed.advmaterials.client.AdvancedMaterialsScreen;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;

@JeiPlugin
public class AdvancedMaterialsJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "advanced_materials_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new AdvancedMaterialsCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addRecipes(AdvancedMaterialsCategory.Type, AdvancedMaterialsRecipes.all(Minecraft.getInstance().level));
        Registration.addItemStackInfo(new ItemStack(AdvancedMaterialsRegistry.BenchItem.get()),
                Component.translatable("jei.scp_unleashed.info.advanced_materials_bench.1"),
                Component.translatable("jei.scp_unleashed.info.advanced_materials_bench.2", AdvancedMaterialsBenchBlockEntity.WorkDraw));
        Registration.addItemStackInfo(new ItemStack(AdvancedMaterialsRegistry.MagnetizedSteelItem.get()),
                Component.translatable("jei.scp_unleashed.info.magnetized_block.1"));
        for (DeferredItem<BlockItem> Door : AdvancedMaterialsRegistry.MagnetizedDoorItems) {
            Registration.addItemStackInfo(new ItemStack(Door.get()),
                    Component.translatable("jei.scp_unleashed.info." + Door.getId().getPath() + ".1"),
                    Component.translatable("jei.scp_unleashed.info.magnetized_bulkhead_door.2"));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(AdvancedMaterialsRegistry.BenchItem.get()), AdvancedMaterialsCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        QuietClickArea.register(Registration, AdvancedMaterialsScreen.class, AdvancedMaterialsScreen.ArrowX, AdvancedMaterialsScreen.ArrowY,
                AdvancedMaterialsScreen.ArrowWidth, AdvancedMaterialsScreen.ArrowHeight, AdvancedMaterialsCategory.Type);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration Registration) {
        Registration.addRecipeTransferHandler(AdvancedMaterialsMenu.class, AdvancedMaterialsRegistry.BenchMenu.get(), AdvancedMaterialsCategory.Type,
                0, AdvancedMaterialsBenchBlockEntity.InputCount, AdvancedMaterialsMenu.MachineSlots, AdvancedMaterialsMenu.HotbarEnd - AdvancedMaterialsMenu.MachineSlots);
    }
}
