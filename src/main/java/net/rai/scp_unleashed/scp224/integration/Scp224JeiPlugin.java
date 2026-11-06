package net.rai.scp_unleashed.scp224.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp224.Scp224Registry;

@JeiPlugin
public class Scp224JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp224Registry.location("scp224_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp224Registry.ClockItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp224_clock.1"),
                Component.translatable("jei.scp_unleashed.info.scp224_clock.2"),
                Component.translatable("jei.scp_unleashed.info.scp224_clock.3"));
    }
}
