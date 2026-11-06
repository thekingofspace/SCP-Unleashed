package net.rai.scp_unleashed.storage.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageRegistry;

@JeiPlugin
public class StorageJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "storage_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Component Accepted = Component.translatable("jei.scp_unleashed.info.storage_container.accepts_catalogued");
        info(Registration, StorageRegistry.SmallContainerItem.get(), "storage_container", 5, Accepted);
        info(Registration, StorageRegistry.TallContainerItem.get(), "storage_container_tall", 3, Accepted);
        info(Registration, StorageRegistry.HeatedUpgrade.get(), "heated_storage_upgrade", 3);
        info(Registration, StorageRegistry.ColdUpgrade.get(), "cold_storage_upgrade", 3);
    }

    private static void info(IRecipeRegistration Registration, ItemLike Subject, String Name, int Lines, Component... Extra) {
        Component[] Pages = new Component[Lines + Extra.length];
        for (int Index = 0; Index < Lines; Index++) {
            Pages[Index] = Component.translatable("jei.scp_unleashed.info." + Name + "." + (Index + 1));
        }
        System.arraycopy(Extra, 0, Pages, Lines, Extra.length);
        Registration.addItemStackInfo(new ItemStack(Subject), Pages);
    }
}
