package net.rai.scp_unleashed.refinery.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import net.rai.scp_unleashed.refinery.OrePurifierBlockEntity;
import net.rai.scp_unleashed.refinery.OrePurifierMenu;
import net.rai.scp_unleashed.refinery.RefineryRegistry;
import net.rai.scp_unleashed.refinery.client.OrePurifierScreen;

@JeiPlugin
public class RefineryJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "refinery_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new PurifyingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        Registration.addRecipes(PurifyingCategory.Type, World.getRecipeManager().getAllRecipesFor(RefineryRegistry.Purifying.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(RefineryRegistry.OrePurifierItem.get()), PurifyingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        QuietClickArea.register(Registration, OrePurifierScreen.class, OrePurifierScreen.ChamberX, OrePurifierScreen.ChamberY,
                OrePurifierScreen.ArrowX + OrePurifierScreen.ArrowWidth - OrePurifierScreen.ChamberX, OrePurifierScreen.ChamberSize, PurifyingCategory.Type);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration Registration) {
        Registration.addRecipeTransferHandler(OrePurifierMenu.class, RefineryRegistry.OrePurifierMenuType.get(), PurifyingCategory.Type,
                OrePurifierBlockEntity.InputSlot, 1, OrePurifierMenu.MachineSlots, OrePurifierMenu.HotbarEnd - OrePurifierMenu.MachineSlots);
    }
}
