package net.rai.scp_unleashed.daeviteruins.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class DaeviteRuinsJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_ruin_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        for (DeferredItem<? extends Item> Entry : DaeviteRuinsRegistry.CarriedItems) {
            String Name = BuiltInRegistries.ITEM.getKey(Entry.get()).getPath();
            Registration.addItemStackInfo(new ItemStack(Entry.get()), Component.translatable("jei.scp_unleashed.info." + Name + ".1"),
                    Component.translatable("jei.scp_unleashed.info." + Name + ".2"));
        }
        List<ItemStack> Decor = new ArrayList<>();
        for (DeferredItem<? extends Item> Entry : DaeviteRuinsRegistry.DecorItems) {
            Decor.add(new ItemStack(Entry.get()));
        }
        Registration.addItemStackInfo(Decor, Component.translatable("jei.scp_unleashed.info.daevite_ruin_decor.1"),
                Component.translatable("jei.scp_unleashed.info.daevite_ruin_decor.2"));
    }
}
