package net.rai.scp_unleashed.scp860.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp860.Scp860Registry;

@JeiPlugin
public class Scp860JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp860Registry.location("scp860_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp860Registry.Key.get()),
                Component.translatable(Prefix + "scp860_key.1"),
                Component.translatable(Prefix + "scp860_key.2"),
                Component.translatable(Prefix + "scp860_key.3"));
        Registration.addItemStackInfo(new ItemStack(Scp860Registry.DoorItem.get()),
                Component.translatable(Prefix + "scp860_door.1"),
                Component.translatable(Prefix + "scp860_door.2"));
        Registration.addItemStackInfo(new ItemStack(Scp860Registry.Egg.get()),
                Component.translatable(Prefix + "scp860_2_spawn_egg.1"),
                Component.translatable(Prefix + "scp860_2_spawn_egg.2"));
    }
}
