package net.rai.scp_unleashed.scp1762.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp1762.Scp1762Registry;

@JeiPlugin
public class Scp1762JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp1762Registry.location("scp1762_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp1762Registry.BoxItem.get()),
                Component.translatable(Prefix + "scp1762_box.1"),
                Component.translatable(Prefix + "scp1762_box.2"),
                Component.translatable(Prefix + "scp1762_box.3"));
    }
}
