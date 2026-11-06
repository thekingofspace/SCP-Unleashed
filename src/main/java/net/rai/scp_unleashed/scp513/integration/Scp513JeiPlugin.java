package net.rai.scp_unleashed.scp513.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp513.Scp513Registry;

@JeiPlugin
public class Scp513JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp513Registry.location("scp513_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp513_bell.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp513Registry.Bell.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"));
    }
}
