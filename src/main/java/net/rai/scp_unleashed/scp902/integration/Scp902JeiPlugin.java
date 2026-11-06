package net.rai.scp_unleashed.scp902.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp902.Scp902Registry;

import java.util.List;

@JeiPlugin
public class Scp902JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp902Registry.location("scp902_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp902Registry.BoxItem.get()),
                Component.translatable(Prefix + "scp902_box.1"),
                Component.translatable(Prefix + "scp902_box.2"),
                Component.translatable(Prefix + "scp902_box.3"));
        List<ItemStack> Props = Scp902Registry.PropItems.stream().map(Entry -> new ItemStack(Entry.get())).toList();
        Registration.addItemStackInfo(Props, Component.translatable(Prefix + "scp902_props.1"));
    }
}
