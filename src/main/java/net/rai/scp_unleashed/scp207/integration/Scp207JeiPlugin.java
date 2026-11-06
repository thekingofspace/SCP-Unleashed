package net.rai.scp_unleashed.scp207.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp207.Scp207BottlesBlock;
import net.rai.scp_unleashed.scp207.Scp207Ration;
import net.rai.scp_unleashed.scp207.Scp207Registry;

@JeiPlugin
public class Scp207JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp207Registry.location("scp207_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp207Registry.Bottle.get()),
                Component.translatable("jei.scp_unleashed.info.scp207_bottle.1"),
                Component.translatable("jei.scp_unleashed.info.scp207_bottle.2"),
                Component.translatable("jei.scp_unleashed.info.scp207_bottle.3", Scp207BottlesBlock.MaxBottles));
        Registration.addItemStackInfo(new ItemStack(Scp207Registry.BottleOne.get()),
                Component.translatable("jei.scp_unleashed.info.scp207_1_bottle.1"),
                Component.translatable("jei.scp_unleashed.info.scp207_1_bottle.2"));
        Registration.addItemStackInfo(new ItemStack(Scp207Registry.CrateItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp207_crate.1"),
                Component.translatable("jei.scp_unleashed.info.scp207_crate.2", Scp207Ration.Daily),
                Component.translatable("jei.scp_unleashed.info.scp207_crate.3"));
    }
}
