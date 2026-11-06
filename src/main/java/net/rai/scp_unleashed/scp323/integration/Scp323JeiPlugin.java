package net.rai.scp_unleashed.scp323.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp323.Scp323Registry;
import net.rai.scp_unleashed.scp323.Scp323SkullItem;

@JeiPlugin
public class Scp323JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp323Registry.location("scp323_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp323SkullItem.fresh(),
                Component.translatable("jei.scp_unleashed.info.scp323.1"),
                Component.translatable("jei.scp_unleashed.info.scp323.2"),
                Component.translatable("jei.scp_unleashed.info.scp323.3"),
                Component.translatable("jei.scp_unleashed.info.scp323.4"),
                Component.translatable("jei.scp_unleashed.info.scp323.5"));
        Registration.addItemStackInfo(new ItemStack(Scp323Registry.SpawnEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp323_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp323_spawn_egg.2"));
    }
}
