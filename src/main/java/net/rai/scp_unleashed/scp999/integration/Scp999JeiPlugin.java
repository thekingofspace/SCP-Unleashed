package net.rai.scp_unleashed.scp999.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp999.Scp999Registry;

@JeiPlugin
public class Scp999JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp999Registry.location("scp999_jei_plugin");
    private static final String EggPrefix = "jei.scp_unleashed.info.scp999_spawn_egg.";
    private static final String CandyPrefix = "jei.scp_unleashed.info.chocolate_orbs.";
    private static final String BowlPrefix = "jei.scp_unleashed.info.pink_dog_bowl.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp999Registry.Egg.get().getDefaultInstance(),
                Component.translatable(EggPrefix + "1"),
                Component.translatable(EggPrefix + "2"),
                Component.translatable(EggPrefix + "3"),
                Component.translatable(EggPrefix + "4"));
        Registration.addItemStackInfo(new ItemStack(Scp999Registry.Candy.get()),
                Component.translatable(CandyPrefix + "1"),
                Component.translatable(CandyPrefix + "2"));
        Registration.addItemStackInfo(new ItemStack(Scp999Registry.BowlItem.get()),
                Component.translatable(BowlPrefix + "1"),
                Component.translatable(BowlPrefix + "2"));
    }
}
