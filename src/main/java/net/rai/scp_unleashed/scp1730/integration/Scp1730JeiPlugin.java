package net.rai.scp_unleashed.scp1730.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;

@JeiPlugin
public class Scp1730JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp1730Registry.location("scp1730_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp1730Registry.FolderItem.get()), Component.translatable(Prefix + "reality_folder.1"),
                Component.translatable(Prefix + "reality_folder.2"), Component.translatable(Prefix + "reality_folder.3"));
        Registration.addItemStackInfo(new ItemStack(Scp1730Registry.Anchor.get()), Component.translatable(Prefix + "scranton_reality_anchor.1"));
        Registration.addItemStackInfo(new ItemStack(Scp1730Registry.Fragment.get()), Component.translatable(Prefix + "scp1730_contained_fragment.1"));
    }
}
