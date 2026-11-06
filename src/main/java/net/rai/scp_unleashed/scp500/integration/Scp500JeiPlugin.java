package net.rai.scp_unleashed.scp500.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp500.Scp500Registry;

@JeiPlugin
public class Scp500JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp500Registry.location("scp500_jei_plugin");
    private static final String BottlePrefix = "jei.scp_unleashed.info.scp500_bottle.";
    private static final String PillPrefix = "jei.scp_unleashed.info.scp500_pill.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp500Registry.BottleItem.get()),
                Component.translatable(BottlePrefix + "1"),
                Component.translatable(BottlePrefix + "2"),
                Component.translatable(BottlePrefix + "3"));
        Registration.addItemStackInfo(new ItemStack(Scp500Registry.Pill.get()),
                Component.translatable(PillPrefix + "1"),
                Component.translatable(PillPrefix + "2"));
    }
}
