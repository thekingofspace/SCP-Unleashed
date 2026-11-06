package net.rai.scp_unleashed.scp096.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp096.Scp096Registry;

@JeiPlugin
public class Scp096JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp096_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp096Registry.SpawnEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp096_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp096_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.scp096_spawn_egg.3"),
                Component.translatable("jei.scp_unleashed.info.scp096_spawn_egg.4"),
                Component.translatable("jei.scp_unleashed.info.scp096_spawn_egg.5"));
        Registration.addItemStackInfo(new ItemStack(Scp096Registry.Bag.get()),
                Component.translatable("jei.scp_unleashed.info.scp096_bag.1"),
                Component.translatable("jei.scp_unleashed.info.scp096_bag.2"));
        Registration.addItemStackInfo(new ItemStack(Scp096Registry.Goggles.get()),
                Component.translatable("jei.scp_unleashed.info.scramble_goggles.1"),
                Component.translatable("jei.scp_unleashed.info.scramble_goggles.2"));
    }
}
