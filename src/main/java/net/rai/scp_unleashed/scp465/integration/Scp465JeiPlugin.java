package net.rai.scp_unleashed.scp465.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp465.Scp465Registry;

import java.util.List;

@JeiPlugin
public class Scp465JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp465Registry.location("scp465_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp465Registry.BoxItem.get()),
                Component.translatable(Prefix + "scp465_box.1"),
                Component.translatable(Prefix + "scp465_box.2"),
                Component.translatable(Prefix + "scp465_box.3"));
        Registration.addItemStackInfo(new ItemStack(Scp465Registry.Letter.get()), Component.translatable(Prefix + "scp465_letter.1"));
        Registration.addItemStackInfo(new ItemStack(Scp465Registry.Chocolate.get()), Component.translatable(Prefix + "scp465_chocolate.1"));
        Registration.addItemStackInfo(List.of(new ItemStack(Scp465Registry.Punch.get()), new ItemStack(Scp465Registry.Cupcake.get()), new ItemStack(Scp465Registry.Chips.get())),
                Component.translatable(Prefix + "scp465_food.1"));
        Registration.addItemStackInfo(Scp465Registry.decorItems().stream().map(Entry -> new ItemStack(Entry.get())).toList(),
                Component.translatable(Prefix + "scp465_party.1"));
    }
}
