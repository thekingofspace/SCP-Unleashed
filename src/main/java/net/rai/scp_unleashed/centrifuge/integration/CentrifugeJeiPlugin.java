package net.rai.scp_unleashed.centrifuge.integration;

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
import net.rai.scp_unleashed.centrifuge.CentrifugeBlockEntity;
import net.rai.scp_unleashed.centrifuge.CentrifugeMenu;
import net.rai.scp_unleashed.centrifuge.CentrifugeRegistry;
import net.rai.scp_unleashed.centrifuge.client.CentrifugeScreen;

@JeiPlugin
public class CentrifugeJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "centrifuge_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new CentrifugingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        Registration.addRecipes(CentrifugingCategory.Type, World.getRecipeManager().getAllRecipesFor(CentrifugeRegistry.Centrifuging.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(CentrifugeRegistry.CentrifugeItem.get()), CentrifugingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        Registration.addRecipeClickArea(CentrifugeScreen.class, CentrifugeScreen.ArrowX, CentrifugeScreen.ArrowY, CentrifugeScreen.ArrowWidth, CentrifugeScreen.ArrowHeight, CentrifugingCategory.Type);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration Registration) {
        Registration.addRecipeTransferHandler(CentrifugeMenu.class, CentrifugeRegistry.CentrifugeMenuType.get(), CentrifugingCategory.Type,
                CentrifugeBlockEntity.InputSlot, 1, CentrifugeMenu.MachineSlots, CentrifugeMenu.HotbarEnd - CentrifugeMenu.MachineSlots);
    }
}
