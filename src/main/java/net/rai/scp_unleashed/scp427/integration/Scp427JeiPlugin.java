package net.rai.scp_unleashed.scp427.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp427.Scp427Registry;

@JeiPlugin
public class Scp427JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp427Registry.location("scp427_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp427Registry.Locket.get()),
                Component.translatable("jei.scp_unleashed.info.scp427_locket.1"),
                Component.translatable("jei.scp_unleashed.info.scp427_locket.2"),
                Component.translatable("jei.scp_unleashed.info.scp427_locket.3"));
        Registration.addItemStackInfo(new ItemStack(Scp427Registry.SpawnEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp427_1_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp427_1_spawn_egg.2"));
    }
}
