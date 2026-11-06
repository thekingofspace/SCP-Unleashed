package net.rai.scp_unleashed.scp280.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp280.Scp280Registry;

@JeiPlugin
public class Scp280JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp280Registry.location("scp280_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp280Registry.ShadeEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp280_shade_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp280_shade_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.scp280_shade_spawn_egg.3"),
                Component.translatable("jei.scp_unleashed.info.scp280_shade_spawn_egg.4"));
    }
}
