package net.rai.scp_unleashed.scp294.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp294.Scp294Registry;

@JeiPlugin
public class Scp294JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp294Registry.location("scp294_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp294Registry.MachineItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp294_machine.1"),
                Component.translatable("jei.scp_unleashed.info.scp294_machine.2"),
                Component.translatable("jei.scp_unleashed.info.scp294_machine.3"));
        Registration.addItemStackInfo(new ItemStack(Scp294Registry.Cup.get()),
                Component.translatable("jei.scp_unleashed.info.scp294_cup.1"));
    }
}
