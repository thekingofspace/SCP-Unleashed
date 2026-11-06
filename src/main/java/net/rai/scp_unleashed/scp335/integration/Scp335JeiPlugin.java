package net.rai.scp_unleashed.scp335.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp335.Scp335Registry;

@JeiPlugin
public class Scp335JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp335Registry.location("scp335_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp335Registry.Disk.get()),
                Component.translatable("jei.scp_unleashed.info.scp335_disk.1"),
                Component.translatable("jei.scp_unleashed.info.scp335_disk.2"),
                Component.translatable("jei.scp_unleashed.info.scp335_disk.3"));
    }
}
