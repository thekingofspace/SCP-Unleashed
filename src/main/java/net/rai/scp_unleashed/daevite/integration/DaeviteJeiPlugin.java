package net.rai.scp_unleashed.daevite.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;

@JeiPlugin
public class DaeviteJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(DaeviteRegistry.KnightEgg.get()),
                Component.translatable("jei.scp_unleashed.info.daevite_knight_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.daevite_knight_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.daevite_knight_spawn_egg.3"));
        Registration.addItemStackInfo(new ItemStack(DaeviteRegistry.OverseerEgg.get()),
                Component.translatable("jei.scp_unleashed.info.daevite_overseer_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.daevite_overseer_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.daevite_overseer_spawn_egg.3"));
        for (DeferredItem<BlockItem> Decor : DaeviteRegistry.DecorItems) {
            String Name = Decor.getId().getPath();
            Registration.addItemStackInfo(new ItemStack(Decor.get()),
                    Component.translatable("jei.scp_unleashed.info." + Name + ".1"),
                    Component.translatable("jei.scp_unleashed.info.daevite_decor.found"));
        }
    }
}
