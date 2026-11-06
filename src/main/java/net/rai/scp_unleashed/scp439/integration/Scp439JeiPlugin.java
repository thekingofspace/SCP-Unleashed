package net.rai.scp_unleashed.scp439.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp439.Scp439Registry;

@JeiPlugin
public class Scp439JeiPlugin implements IModPlugin {
    private static final ResourceLocation PluginId = Scp439Registry.location("scp439_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return PluginId;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp439Registry.QueenEgg.get()), Component.translatable(Prefix + "scp439_queen_spawn_egg.1"),
                Component.translatable(Prefix + "scp439_queen_spawn_egg.2"));
        Registration.addItemStackInfo(new ItemStack(Scp439Registry.WorkerEgg.get()), Component.translatable(Prefix + "scp439_worker_spawn_egg.1"),
                Component.translatable(Prefix + "scp439_worker_spawn_egg.2"));
        Registration.addItemStackInfo(new ItemStack(Scp439Registry.BruteEgg.get()), Component.translatable(Prefix + "scp439_brute_spawn_egg.1"),
                Component.translatable(Prefix + "scp439_brute_spawn_egg.2"));
    }
}
