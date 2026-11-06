package net.rai.scp_unleashed.scp244.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp244.Scp244Registry;

import java.util.List;

@JeiPlugin
public class Scp244JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp244_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<ItemStack> Vases = Scp244Registry.vaseItems().stream().map(Entry -> new ItemStack(Entry.get())).toList();
        Registration.addItemStackInfo(Vases,
                Component.translatable("jei.scp_unleashed.info.scp244_vase.1"),
                Component.translatable("jei.scp_unleashed.info.scp244_vase.2"),
                Component.translatable("jei.scp_unleashed.info.scp244_vase.3"),
                Component.translatable("jei.scp_unleashed.info.scp244_vase.4"));
    }
}
