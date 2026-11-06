package net.rai.scp_unleashed.scp016.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp016.Scp016Brute;
import net.rai.scp_unleashed.scp016.Scp016Duelist;
import net.rai.scp_unleashed.scp016.Scp016Flyer;
import net.rai.scp_unleashed.scp016.Scp016Glutton;
import net.rai.scp_unleashed.scp016.Scp016Registry;
import net.rai.scp_unleashed.scp016.Scp016Shard;
import net.rai.scp_unleashed.scp016.Scp016Skitter;
import net.rai.scp_unleashed.scp016.Scp016Slinger;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import net.rai.scp_unleashed.scp016.Scp016Swimmer;

@JeiPlugin
public class Scp016JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp016_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        int Lifespan = Scp016Spawn.Lifespan / 1200;
        Registration.addItemStackInfo(Scp016Registry.SkitterEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp016_skitter_spawn_egg.1", (int) Scp016Skitter.Health),
                Component.translatable(Prefix + "scp016_spawn_egg.shared", Lifespan));
        Registration.addItemStackInfo(Scp016Registry.BruteEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp016_brute_spawn_egg.1", (int) Scp016Brute.Health),
                Component.translatable(Prefix + "scp016_spawn_egg.shared", Lifespan));
        Registration.addItemStackInfo(Scp016Registry.SlingerEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp016_slinger_spawn_egg.1", (int) Scp016Slinger.Health, (int) Scp016Shard.Damage),
                Component.translatable(Prefix + "scp016_spawn_egg.shared", Lifespan));
        Registration.addItemStackInfo(Scp016Registry.DuelistEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp016_duelist_spawn_egg.1", (int) Scp016Duelist.Health),
                Component.translatable(Prefix + "scp016_spawn_egg.shared", Lifespan));
        Registration.addItemStackInfo(Scp016Registry.GluttonEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp016_glutton_spawn_egg.1", (int) Scp016Glutton.Health),
                Component.translatable(Prefix + "scp016_spawn_egg.shared", Lifespan));
        Registration.addItemStackInfo(Scp016Registry.SwimmerEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp016_swimmer_spawn_egg.1", (int) Scp016Swimmer.Health),
                Component.translatable(Prefix + "scp016_spawn_egg.shared", Lifespan));
        Registration.addItemStackInfo(Scp016Registry.FlyerEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp016_flyer_spawn_egg.1", (int) Scp016Flyer.Health),
                Component.translatable(Prefix + "scp016_spawn_egg.shared", Lifespan));
    }
}
