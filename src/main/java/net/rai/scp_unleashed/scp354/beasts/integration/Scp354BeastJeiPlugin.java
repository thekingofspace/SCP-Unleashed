package net.rai.scp_unleashed.scp354.beasts.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastRegistry;

@JeiPlugin
public class Scp354BeastJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp354_beast_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        for (DeferredItem<DeferredSpawnEggItem> Egg : Scp354BeastRegistry.Eggs) {
            String Path = BuiltInRegistries.ITEM.getKey(Egg.get()).getPath();
            Registration.addItemStackInfo(Egg.get().getDefaultInstance(), Component.translatable("jei.scp_unleashed.info." + Path));
        }
    }
}
