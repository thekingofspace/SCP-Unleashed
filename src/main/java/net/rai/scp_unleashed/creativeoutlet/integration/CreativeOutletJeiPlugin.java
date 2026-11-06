package net.rai.scp_unleashed.creativeoutlet.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletRegistry;

@JeiPlugin
public class CreativeOutletJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = CreativeOutletRegistry.location(CreativeOutletRegistry.OutletId + "_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info." + CreativeOutletRegistry.OutletId + ".";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(CreativeOutletRegistry.OutletItem.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"));
    }
}
