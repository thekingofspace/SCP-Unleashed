package net.rai.scp_unleashed.scp009.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp009.Scp009Registry;

@JeiPlugin
public class Scp009JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp009_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        info(Registration, Scp009Registry.Shard.get(), "scp009_shard", 4);
        info(Registration, Scp009Registry.Fragment.get(), "scp009_fragment", 3);
        info(Registration, Scp009Registry.RedIceItem.get(), "scp009_red_ice", 2);
    }

    private static void info(IRecipeRegistration Registration, ItemLike Subject, String Name, int Pages) {
        Component[] Lines = new Component[Pages];
        for (int Index = 0; Index < Pages; Index++) {
            Lines[Index] = Component.translatable("jei.scp_unleashed.info." + Name + "." + (Index + 1));
        }
        Registration.addItemStackInfo(new ItemStack(Subject), Lines);
    }
}
