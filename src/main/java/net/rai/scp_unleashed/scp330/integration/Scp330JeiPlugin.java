package net.rai.scp_unleashed.scp330.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp330.Scp330Colour;
import net.rai.scp_unleashed.scp330.Scp330Registry;

@JeiPlugin
public class Scp330JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp330Registry.location("scp330_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp330Registry.BowlItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp330_bowl.1"),
                Component.translatable("jei.scp_unleashed.info.scp330_bowl.2"),
                Component.translatable("jei.scp_unleashed.info.scp330_bowl.3"));
        for (Scp330Colour Colour : Scp330Colour.values()) {
            Registration.addItemStackInfo(new ItemStack(Scp330Registry.candy(Colour)),
                    Component.translatable("jei.scp_unleashed.info." + Colour.itemId()));
        }
    }
}
