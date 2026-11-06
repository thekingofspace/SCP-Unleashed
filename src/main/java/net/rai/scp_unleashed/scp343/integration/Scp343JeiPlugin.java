package net.rai.scp_unleashed.scp343.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp343.Scp343Registry;

@JeiPlugin
public class Scp343JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp343_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp343Registry.SpawnEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp343_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp343_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.scp343_spawn_egg.3"),
                Component.translatable("jei.scp_unleashed.info.scp343_spawn_egg.4"));
        Registration.addItemStackInfo(new ItemStack(Scp343Registry.ThroneItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp343_throne.1"),
                Component.translatable("jei.scp_unleashed.info.scp343_throne.2"),
                Component.translatable("jei.scp_unleashed.info.scp343_throne.3"));
    }
}
